package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AddDownloadBehavior
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import com.example.data.QueueSortOrder
import com.example.data.SettingsManager
import com.example.data.SpeedLimit
import com.example.data.ThemeMode
import com.example.model.MoodleManifest
import com.example.parser.MoodleCodeParser
import com.example.util.BatteryUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ParseUiState {
    data object Idle : ParseUiState
    data object Validating : ParseUiState
    data class Valid(val manifest: MoodleManifest) : ParseUiState
    data class Invalid(val message: String, val detail: String? = null) : ParseUiState
}

data class AddDownloadSnackbarEvent(
    val message: String,
    val actionLabel: String? = "Ver",
    val targetTab: Int = 1
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val settingsManager = SettingsManager(application)

    val settings: StateFlow<AppSettings> = settingsManager.settings

    // Tab seleccionado centralizado (0: Descargar, 1: En curso)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, 1)
    }

    // Flujos reactivos directos de Room
    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloads: StateFlow<List<DownloadEntity>> = repository.activeDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadingDownloads: StateFlow<List<DownloadEntity>> = repository.downloadingDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val queuedDownloads: StateFlow<List<DownloadEntity>> = combine(
        repository.queuedDownloads,
        settingsManager.settings
    ) { queued, settings ->
        when (settings.queueSortOrder) {
            QueueSortOrder.FIFO -> queued.sortedWith(compareBy({ it.queuePosition }, { it.createdAt }))
            QueueSortOrder.SMALLEST_FIRST -> queued.sortedWith(compareBy<DownloadEntity> { it.totalBytes }.thenBy { it.queuePosition }.thenBy { it.createdAt })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pausedDownloads: StateFlow<List<DownloadEntity>> = repository.pausedDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedDownloads: StateFlow<List<DownloadEntity>> = repository.completedDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _codeText = MutableStateFlow("")
    val codeText: StateFlow<String> = _codeText.asStateFlow()

    private val _parseState = MutableStateFlow<ParseUiState>(ParseUiState.Idle)
    val parseState: StateFlow<ParseUiState> = _parseState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<AddDownloadSnackbarEvent>()
    val snackbarEvent: SharedFlow<AddDownloadSnackbarEvent> = _snackbarEvent.asSharedFlow()

    private val _showWelcomeSheet = MutableStateFlow(false)
    val showWelcomeSheet: StateFlow<Boolean> = _showWelcomeSheet.asStateFlow()

    private var parseDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.resumePendingDownloadsOnStartup()
            delay(400)
            checkWelcomeSheetEligibility()
        }
    }

    fun onCodeChanged(newCode: String) {
        _codeText.value = newCode
        parseDebounceJob?.cancel()

        val trimmed = newCode.trim()
        if (trimmed.isBlank()) {
            _parseState.value = ParseUiState.Idle
            return
        }

        parseDebounceJob = viewModelScope.launch {
            _parseState.value = ParseUiState.Validating
            delay(350)

            when (val result = MoodleCodeParser.parse(trimmed)) {
                is MoodleCodeParser.ParseResult.Success -> {
                    _parseState.value = ParseUiState.Valid(result.manifest)
                }
                is MoodleCodeParser.ParseResult.Error -> {
                    _parseState.value = ParseUiState.Invalid(result.message, result.detail)
                }
            }
        }
    }

    fun startDownload(customFileName: String? = null) {
        val code = _codeText.value.trim()
        if (code.isEmpty()) return

        var currentParse = _parseState.value
        if (currentParse !is ParseUiState.Valid) {
            when (val result = MoodleCodeParser.parse(code)) {
                is MoodleCodeParser.ParseResult.Success -> {
                    _parseState.value = ParseUiState.Valid(result.manifest)
                    currentParse = ParseUiState.Valid(result.manifest)
                }
                is MoodleCodeParser.ParseResult.Error -> {
                    _parseState.value = ParseUiState.Invalid(result.message, result.detail)
                    return
                }
            }
        }

        val manifest = (currentParse as ParseUiState.Valid).manifest
        checkBatteryOptimizationNotice()

        viewModelScope.launch {
            try {
                val currentRunning = downloadingDownloads.value.size
                val limit = settingsManager.settings.value.maxConcurrentDownloads
                val isQueue = currentRunning >= limit || settingsManager.settings.value.addDownloadBehavior == AddDownloadBehavior.ADD_TO_QUEUE_ONLY
                val queuePos = queuedDownloads.value.size + 1

                repository.enqueueDownload(code, manifest, customFileName)

                val msg = if (isQueue) {
                    "Agregada a la cola (#$queuePos)"
                } else {
                    "Descargando ${manifest.filename}"
                }

                _snackbarEvent.emit(AddDownloadSnackbarEvent(message = msg, actionLabel = "Ver", targetTab = 1))

                // Siempre deja el campo vacío listo para el siguiente código
                _codeText.value = ""
                _parseState.value = ParseUiState.Idle
            } catch (e: Exception) {
                _snackbarEvent.emit(AddDownloadSnackbarEvent(message = "Error: ${e.message}", actionLabel = null))
            }
        }
    }

    fun pauseDownload(id: String) = viewModelScope.launch { repository.pauseDownload(id) }
    fun resumeDownload(id: String) = viewModelScope.launch { repository.resumeDownload(id) }
    fun retryDownload(id: String) = viewModelScope.launch { repository.retryDownload(id) }
    fun cancelDownload(id: String) = viewModelScope.launch { repository.cancelDownload(id) }

    fun pauseAll() = viewModelScope.launch { repository.pauseAll() }
    fun resumeAll() = viewModelScope.launch { repository.resumeAll() }

    fun forceStartNow(id: String) = viewModelScope.launch { repository.forceStartNow(id) }
    fun moveToTop(id: String) = viewModelScope.launch { repository.moveToTop(id) }
    fun deleteCompletedItem(id: String) = viewModelScope.launch { repository.deleteDownload(id) }
    fun deleteSelectedCompleted(ids: List<String>) = viewModelScope.launch {
        repository.deleteSelectedCompleted(ids)
        _snackbarEvent.emit(AddDownloadSnackbarEvent("${ids.size} archivos eliminados", null))
    }
    fun clearAllCompleted() = viewModelScope.launch { repository.clearCompleted() }

    fun reDownload(code: String, customFileName: String? = null) {
        viewModelScope.launch {
            try {
                when (val result = MoodleCodeParser.parse(code)) {
                    is MoodleCodeParser.ParseResult.Success -> {
                        repository.enqueueDownload(code, result.manifest, customFileName)
                        _snackbarEvent.emit(AddDownloadSnackbarEvent(message = "Descarga añadida a la cola", actionLabel = "Ver", targetTab = 1))
                    }
                    is MoodleCodeParser.ParseResult.Error -> {
                        _snackbarEvent.emit(AddDownloadSnackbarEvent(message = "No se pudo volver a descargar: ${result.message}", null))
                    }
                }
            } catch (e: Exception) {
                _snackbarEvent.emit(AddDownloadSnackbarEvent(message = "Error: ${e.message}", null))
            }
        }
    }

    // Bienvenida
    fun checkWelcomeSheetEligibility() {
        val s = settingsManager.settings.value
        if (s.dontShowWelcomeAgain) {
            _showWelcomeSheet.value = false
            return
        }

        if (!s.hasSeenWelcome) {
            _showWelcomeSheet.value = true
            return
        }

        val hasActive = activeDownloads.value.isNotEmpty()
        val now = System.currentTimeMillis()
        val threeDaysMs = 3 * 24 * 3600 * 1000L
        val canShowPeriodic = (now - s.lastWelcomeShownTime) > threeDaysMs
        val batteryRestricted = !BatteryUtils.isIgnoringBatteryOptimizations(getApplication())

        if (!hasActive && canShowPeriodic && batteryRestricted) {
            _showWelcomeSheet.value = true
            settingsManager.updateLastWelcomeShownTime(now)
        }
    }

    fun openWelcomeSheetManually() { _showWelcomeSheet.value = true }
    fun dismissWelcomeSheet(dontShowAgain: Boolean = false) {
        _showWelcomeSheet.value = false
        settingsManager.setHasSeenWelcome(true)
        settingsManager.updateLastWelcomeShownTime()
        if (dontShowAgain) settingsManager.setDontShowWelcomeAgain(true)
    }
    fun markAutoStartAcknowledged() { settingsManager.setAutoStartAcknowledged(true) }

    private fun checkBatteryOptimizationNotice() {
        val context = getApplication<Application>()
        val s = settingsManager.settings.value
        if (!s.hasWarnedBatteryRestriction && !BatteryUtils.isIgnoringBatteryOptimizations(context)) {
            settingsManager.setHasWarnedBatteryRestriction(true)
            viewModelScope.launch {
                _snackbarEvent.emit(AddDownloadSnackbarEvent("Sugerencia: Permite batería sin restricciones para no pausar con pantalla apagada.", null))
            }
        }
    }

    // Ajustes
    fun setThemeMode(mode: ThemeMode) = settingsManager.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settingsManager.setDynamicColor(enabled)
    fun setWifiOnly(enabled: Boolean) = settingsManager.setWifiOnly(enabled)
    fun setAutoRetry(enabled: Boolean) = settingsManager.setAutoRetry(enabled)
    fun setMaxRetries(retries: Int) = settingsManager.setMaxRetries(retries)
    fun setMaxConcurrentDownloads(limit: Int) = settingsManager.setMaxConcurrentDownloads(limit)
    fun setMaxConcurrentParts(parts: Int) = settingsManager.setMaxConcurrentParts(parts)
    fun setAddDownloadBehavior(behavior: AddDownloadBehavior) = settingsManager.setAddDownloadBehavior(behavior)
    fun setAutoStartNext(enabled: Boolean) = settingsManager.setAutoStartNext(enabled)
    fun setQueueSortOrder(order: QueueSortOrder) = settingsManager.setQueueSortOrder(order)
    fun setSpeedLimit(limit: SpeedLimit) = settingsManager.setSpeedLimit(limit)
    fun setVibrateOnComplete(enabled: Boolean) = settingsManager.setVibrateOnComplete(enabled)
    fun setAutoClearOnStart(enabled: Boolean) = settingsManager.setAutoClearOnStart(enabled)
    fun setShowProgressNotifications(enabled: Boolean) = settingsManager.setShowProgressNotifications(enabled)
    fun setSoundOnComplete(enabled: Boolean) = settingsManager.setSoundOnComplete(enabled)
}
