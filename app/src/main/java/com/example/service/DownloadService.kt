package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AddDownloadBehavior
import com.example.data.AppDatabase
import com.example.data.DownloadEntity
import com.example.data.QueueSortOrder
import com.example.data.SettingsManager
import com.example.data.SpeedLimit
import com.example.model.ChunkPart
import com.example.model.DownloadState
import com.example.parser.MoodleCodeParser
import com.example.util.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DownloadService : Service() {

    private class PermanentDownloadException(message: String) : IOException(message)

    companion object {
        const val TAG = "DownloadChunk"

        const val CHANNEL_PROGRESS_ID = "download_progress_channel"
        const val CHANNEL_COMPLETED_ID = "download_completed_channel"
        const val CHANNEL_ERROR_ID = "download_error_channel"

        const val NOTIFICATION_GROUP_KEY = "com.example.downloadchunk.DOWNLOAD_GROUP"
        const val NOTIFICATION_SUMMARY_ID = 1000
        private const val BUFFER_SIZE = 1024 * 512 // 512 KiB buffer

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"
        const val ACTION_RETRY = "com.example.service.ACTION_RETRY"
        const val ACTION_PAUSE_ALL = "com.example.service.ACTION_PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.example.service.ACTION_RESUME_ALL"
        const val ACTION_CANCEL_ALL = "com.example.service.ACTION_CANCEL_ALL"
        const val ACTION_MOVE_TO_TOP = "com.example.service.ACTION_MOVE_TO_TOP"
        const val ACTION_FORCE_START = "com.example.service.ACTION_FORCE_START"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startDownload(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_START, downloadId)
        fun pauseDownload(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_PAUSE, downloadId)
        fun resumeDownload(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_RESUME, downloadId)
        fun cancelDownload(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_CANCEL, downloadId)
        fun retryDownload(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_RETRY, downloadId)
        fun pauseAll(context: Context) = sendServiceCommand(context, ACTION_PAUSE_ALL, null)
        fun resumeAll(context: Context) = sendServiceCommand(context, ACTION_RESUME_ALL, null)
        fun cancelAll(context: Context) = sendServiceCommand(context, ACTION_CANCEL_ALL, null)
        fun moveToTop(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_MOVE_TO_TOP, downloadId)
        fun forceStartNow(context: Context, downloadId: String) = sendServiceCommand(context, ACTION_FORCE_START, downloadId)

        private fun sendServiceCommand(context: Context, action: String, downloadId: String?) {
            Log.d(TAG, "sendServiceCommand: action=$action, downloadId=$downloadId")
            val intent = Intent(context, DownloadService::class.java).apply {
                this.action = action
                if (downloadId != null) putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service for $action: ${e.message}", e)
            }
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled coroutine error in DownloadService: ${throwable.message}", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase
    private lateinit var settingsManager: SettingsManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    // Sincronización y colas seguras
    private val mutexMap = ConcurrentHashMap<String, Mutex>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, MutableSet<Call>>()
    private val pausingFlags = ConcurrentHashMap<String, AtomicBoolean>()
    private val retryAttempts = ConcurrentHashMap<String, AtomicInteger>()

    // Rate limiter para notificaciones (máximo 2 por segundo = 500ms entre actualizaciones)
    private var lastNotificationUpdateTime = 0L
    private val notificationUpdateMutex = Mutex()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DownloadService onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        database = AppDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)

        createNotificationChannels()
        initLocks()
        registerNetworkCallback()
        observeSettingsChanges()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "DEFAULT_START"
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)
        Log.d(TAG, "onStartCommand: action=$action, downloadId=$downloadId")

        // Asegurar startForeground inmediato y sincrónico para cumplir el contrato de Android 8+
        ensureForegroundStarted()

        serviceScope.launch {
            try {
                when (action) {
                    ACTION_START -> {
                        if (downloadId != null) handleStartOrEnqueue(downloadId)
                    }
                    ACTION_PAUSE -> {
                        if (downloadId != null) handlePauseCommand(downloadId)
                    }
                    ACTION_RESUME -> {
                        if (downloadId != null) handleResumeCommand(downloadId)
                    }
                    ACTION_CANCEL -> {
                        if (downloadId != null) handleCancelCommand(downloadId)
                    }
                    ACTION_RETRY -> {
                        if (downloadId != null) handleRetryCommand(downloadId)
                    }
                    ACTION_PAUSE_ALL -> {
                        handlePauseAllCommand()
                    }
                    ACTION_RESUME_ALL -> {
                        handleResumeAllCommand()
                    }
                    ACTION_CANCEL_ALL -> {
                        handleCancelAllCommand()
                    }
                    ACTION_MOVE_TO_TOP -> {
                        if (downloadId != null) handleMoveToTopCommand(downloadId)
                    }
                    ACTION_FORCE_START -> {
                        if (downloadId != null) handleForceStartCommand(downloadId)
                    }
                    else -> {
                        // Restauración tras reinicio del servicio o del sistema
                        restoreActiveDownloads()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error executing service action $action: ${t.message}", t)
            }
        }

        return START_STICKY
    }

    private fun getMutex(id: String): Mutex = mutexMap.computeIfAbsent(id) { Mutex() }

    private suspend fun handleStartOrEnqueue(downloadId: String) {
        val mutex = getMutex(downloadId)
        mutex.withLock {
            val entity = database.downloadDao().getDownloadById(downloadId) ?: return
            val settings = settingsManager.settings.value

            if (settings.addDownloadBehavior == AddDownloadBehavior.ADD_TO_QUEUE_ONLY) {
                Log.d(TAG, "Configured to add to queue only: $downloadId")
                database.downloadDao().updateStatus(downloadId, DownloadState.QUEUED.name, null)
                return
            }

            processQueue()
        }
    }

    private suspend fun handlePauseCommand(downloadId: String) {
        val mutex = getMutex(downloadId)
        mutex.withLock {
            val entity = database.downloadDao().getDownloadById(downloadId) ?: return

            // Ignorar órdenes inválidas
            if (entity.status == DownloadState.PAUSED.name || entity.status == DownloadState.PAUSING.name) {
                Log.d(TAG, "Ignoring pause: $downloadId already in state ${entity.status}")
                return
            }

            Log.d(TAG, "Pausing download safely: $downloadId")
            // 1. Marcar estado transicional
            database.downloadDao().updateStatus(downloadId, DownloadState.PAUSING.name, null, pausedByNetwork = false)

            // 2. Señalizar que termine la escritura en curso y cancelar la llamada HTTP
            pausingFlags[downloadId]?.set(true)
            activeCalls[downloadId]?.forEach {
                try { it.cancel() } catch (_: Throwable) {}
            }

            // 3. Esperar que el trabajo activo termine ordenadamente de guardar en disco
            val job = activeJobs.remove(downloadId)
            try {
                job?.cancelAndJoin()
            } catch (_: Throwable) {
                Log.d(TAG, "Job cancellation completed for $downloadId")
            }

            // 4. Pasar a "Pausada" guardando los bytes descargados
            database.downloadDao().updateStatus(downloadId, DownloadState.PAUSED.name, null, pausedByNetwork = false)
            Log.d(TAG, "Transitioned safely to PAUSED: $downloadId")

            releaseLocksIfIdle()
            throttledUpdateNotifications()

            // Una descarga pausada por el usuario NO ocupa lugar en el límite: arrancar la siguiente en cola
            if (settingsManager.settings.value.autoStartNext) {
                processQueue()
            }
        }
    }

    private suspend fun handleResumeCommand(downloadId: String) {
        val mutex = getMutex(downloadId)
        mutex.withLock {
            val entity = database.downloadDao().getDownloadById(downloadId) ?: return

            if (entity.status == DownloadState.DOWNLOADING.name) {
                Log.d(TAG, "Ignoring resume: $downloadId is already DOWNLOADING")
                return
            }

            Log.d(TAG, "Resuming download: $downloadId")
            retryAttempts.remove(downloadId)
            // Mover a cola para que el procesador respete el límite de simultaneidad
            database.downloadDao().updateStatus(downloadId, DownloadState.QUEUED.name, null, pausedByNetwork = false)
            processQueue()
        }
    }

    private suspend fun handleRetryCommand(downloadId: String) {
        val mutex = getMutex(downloadId)
        mutex.withLock {
            Log.d(TAG, "Retrying download: $downloadId")
            retryAttempts.remove(downloadId)
            database.downloadDao().updateStatus(downloadId, DownloadState.QUEUED.name, null, pausedByNetwork = false)
            processQueue()
        }
    }

    private suspend fun handleCancelCommand(downloadId: String) {
        val mutex = getMutex(downloadId)
        mutex.withLock {
            Log.d(TAG, "Cancelling download: $downloadId")
            pausingFlags[downloadId]?.set(true)
            activeCalls.remove(downloadId)?.forEach { try { it.cancel() } catch (_: Exception) {} }
            activeJobs.remove(downloadId)?.cancel()

            database.downloadDao().updateStatus(downloadId, DownloadState.CANCELLED.name, null)

            // Eliminar notificación individual
            notificationManager.cancel(downloadId.hashCode())
            releaseLocksIfIdle()
            throttledUpdateNotifications()

            if (settingsManager.settings.value.autoStartNext) {
                processQueue()
            }
        }
    }

    private suspend fun handlePauseAllCommand() {
        Log.d(TAG, "Pausing all active downloads")
        val downloading = database.downloadDao().getCurrentlyDownloading()
        for (item in downloading) {
            handlePauseCommand(item.id)
        }
    }

    private suspend fun handleResumeAllCommand() {
        Log.d(TAG, "Resuming all paused downloads")
        val dao = database.downloadDao()
        val paused = dao.getAllDownloads()
        // Poner en cola todas las pausadas
        val unstarted = dao.getUnfinishedDownloads()
        for (item in unstarted) {
            if (item.status == DownloadState.PAUSED.name || item.status == DownloadState.ERROR.name) {
                dao.updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
            }
        }
        processQueue()
    }

    private suspend fun handleCancelAllCommand() {
        Log.d(TAG, "Cancelling all active and queued downloads")
        val unstarted = database.downloadDao().getUnfinishedDownloads()
        for (item in unstarted) {
            handleCancelCommand(item.id)
        }
        notificationManager.cancel(NOTIFICATION_SUMMARY_ID)
    }

    private suspend fun handleMoveToTopCommand(downloadId: String) {
        val dao = database.downloadDao()
        val queued = dao.getQueuedDownloads()
        var pos = 1
        dao.updateQueuePosition(downloadId, 0)
        for (item in queued) {
            if (item.id != downloadId) {
                dao.updateQueuePosition(item.id, pos++)
            }
        }
        Log.d(TAG, "Moved $downloadId to top of queue")
    }

    private suspend fun handleForceStartCommand(downloadId: String) {
        val settings = settingsManager.settings.value
        val maxLimit = settings.maxConcurrentDownloads
        val currentRunning = database.downloadDao().getCurrentlyDownloading()

        // Si se supera el límite, pausar la más reciente
        if (currentRunning.size >= maxLimit && currentRunning.isNotEmpty()) {
            val mostRecent = currentRunning.maxByOrNull { it.createdAt }
            if (mostRecent != null && mostRecent.id != downloadId) {
                Log.d(TAG, "Force start: pausing most recent ${mostRecent.id} to make room")
                handlePauseCommand(mostRecent.id)
            }
        }

        startExecution(downloadId)
    }

    /**
     * Procesador central de la cola de descargas respetando el límite de simultaneidad.
     */
    private suspend fun processQueue() {
        val settings = settingsManager.settings.value
        val maxLimit = settings.maxConcurrentDownloads

        val activeCount = activeJobs.size
        val availableSlots = maxLimit - activeCount

        Log.d(TAG, "processQueue: activeCount=$activeCount, maxLimit=$maxLimit, availableSlots=$availableSlots")

        if (availableSlots < 0) {
            val excessCount = -availableSlots
            val currentlyActive = database.downloadDao().getCurrentlyDownloading()
            val toPause = currentlyActive.takeLast(excessCount)
            for (item in toPause) {
                Log.d(TAG, "Real-time pausing excess download to conform to max limit: ${item.id}")
                handlePauseCommand(item.id)
                database.downloadDao().updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
            }
            throttledUpdateNotifications()
            return
        }

        if (availableSlots == 0) {
            throttledUpdateNotifications()
            return
        }

        var queuedList = database.downloadDao().getQueuedDownloads()

        // Orden de la cola
        queuedList = when (settings.queueSortOrder) {
            QueueSortOrder.FIFO -> queuedList.sortedWith(compareBy({ it.queuePosition }, { it.createdAt }))
            QueueSortOrder.SMALLEST_FIRST -> queuedList.sortedWith(compareBy<DownloadEntity> { it.totalBytes }.thenBy { it.queuePosition }.thenBy { it.createdAt })
        }

        val toStart = queuedList.take(availableSlots)
        for (item in toStart) {
            startExecution(item.id)
        }

        throttledUpdateNotifications()
    }

    private suspend fun startExecution(downloadId: String) {
        if (activeJobs[downloadId]?.isActive == true) return

        val entity = database.downloadDao().getDownloadById(downloadId) ?: return
        val settings = settingsManager.settings.value

        if (settings.wifiOnly && !isWifiConnected()) {
            Log.d(TAG, "Wi-Fi only enabled but no Wi-Fi: pausing $downloadId")
            database.downloadDao().updateStatus(downloadId, DownloadState.PAUSED.name, "Pausada: Solo con Wi-Fi está activado.", pausedByNetwork = true)
            throttledUpdateNotifications()
            return
        }

        acquireLocks()
        pausingFlags[downloadId] = AtomicBoolean(false)
        database.downloadDao().updateStatus(downloadId, DownloadState.DOWNLOADING.name, null, pausedByNetwork = false)

        val job = serviceScope.launch {
            try {
                Log.d(TAG, "Starting download pipeline for $downloadId (${entity.fileName})")
                executeDownloadPipeline(entity)
            } catch (e: CancellationException) {
                Log.d(TAG, "Download pipeline cancelled/paused for $downloadId")
            } catch (e: Exception) {
                if (pausingFlags[downloadId]?.get() == true) {
                    Log.d(TAG, "Download paused intentionally for $downloadId, suppressing error: ${e.message}")
                } else {
                    Log.e(TAG, "Error in download pipeline for $downloadId: ${e.message}", e)
                    handleDownloadError(downloadId, e)
                }
            } finally {
                activeJobs.remove(downloadId)
                activeCalls.remove(downloadId)
                pausingFlags.remove(downloadId)
                releaseLocksIfIdle()
                throttledUpdateNotifications()

                // Arrancar siguiente en cola
                if (settingsManager.settings.value.autoStartNext) {
                    processQueue()
                }
                stopSelfIfIdle()
            }
        }

        activeJobs[downloadId] = job
        throttledUpdateNotifications()
    }

    private suspend fun executeDownloadPipeline(entity: DownloadEntity) {
        val parseResult = MoodleCodeParser.parse(entity.code)
        if (parseResult !is MoodleCodeParser.ParseResult.Success) {
            val errorMsg = (parseResult as? MoodleCodeParser.ParseResult.Error)?.message ?: "Código de descarga inválido."
            database.downloadDao().markFailed(entity.id, DownloadState.ERROR.name, errorMsg, System.currentTimeMillis())
            showErrorNotification(entity.id, entity.fileName, errorMsg)
            return
        }

        val manifest = parseResult.manifest
        val codeFingerprint = MoodleCodeParser.getCodeFingerprint(entity.code)
        val partsDirectory = File(filesDir, "chunks_$codeFingerprint")
        if (!partsDirectory.exists()) partsDirectory.mkdirs()

        val orderedParts = manifest.parts
        val totalParts = orderedParts.size
        val totalManifestSize = manifest.size

        var initialCompletedBytes = 0L
        var initialCompletedCount = 0
        for (part in orderedParts) {
            val partFile = File(partsDirectory, "%05d.part".format(part.index))
            if (partFile.exists() && partFile.length() > 0L) {
                initialCompletedBytes += partFile.length()
                initialCompletedCount++
            }
        }

        val completedBytes = AtomicLong(initialCompletedBytes)
        val completedCount = AtomicInteger(initialCompletedCount)
        val activePartBytes = ConcurrentHashMap<Int, Long>()

        val speedBpsAtomic = AtomicLong(if (entity.speedBps > 0) entity.speedBps else 0L)
        val etaAtomic = AtomicLong(entity.etaSeconds)
        val lastSpeedCalcTimeAtomic = AtomicLong(System.currentTimeMillis())
        val lastBytesForSpeedAtomic = AtomicLong(initialCompletedBytes)
        val lastDbUpdateTimeAtomic = AtomicLong(0L)

        fun getTotalDownloaded(): Long = completedBytes.get() + activePartBytes.values.sum()

        val maxConcurrentParts = settingsManager.settings.value.maxConcurrentParts.coerceIn(1, 4)
        val semaphore = Semaphore(maxConcurrentParts)

        coroutineScope {
            for (part in orderedParts) {
                if (!coroutineScopeIsActive() || pausingFlags[entity.id]?.get() == true) break

                val partFile = File(partsDirectory, "%05d.part".format(part.index))
                val tempFile = File(partsDirectory, "%05d.tmp".format(part.index))

                if (partFile.exists() && partFile.length() > 0L) {
                    continue
                }

                launch {
                    semaphore.withPermit {
                        if (!coroutineScopeIsActive() || pausingFlags[entity.id]?.get() == true) return@withPermit

                        downloadPartWithRange(
                            downloadId = entity.id,
                            partUrl = part.url,
                            tempFile = tempFile,
                            partFile = partFile,
                            onProgress = { bytesInPart ->
                                activePartBytes[part.index] = bytesInPart
                                val now = System.currentTimeMillis()
                                val downloadedTotal = getTotalDownloaded()

                                val lastCalcTime = lastSpeedCalcTimeAtomic.get()
                                val durationMs = now - lastCalcTime
                                if (durationMs >= 350L && lastSpeedCalcTimeAtomic.compareAndSet(lastCalcTime, now)) {
                                    val lastBytes = lastBytesForSpeedAtomic.getAndSet(downloadedTotal)
                                    val delta = downloadedTotal - lastBytes
                                    if (delta >= 0) {
                                        val instantSpeed = (delta * 1000L) / durationMs
                                        val prevSpeed = speedBpsAtomic.get()
                                        val newSpeed = if (prevSpeed > 0) {
                                            ((prevSpeed * 0.70) + (instantSpeed * 0.30)).toLong().coerceAtLeast(1024L)
                                        } else {
                                            instantSpeed.coerceAtLeast(1024L)
                                        }
                                        speedBpsAtomic.set(newSpeed)
                                        if (newSpeed > 0 && totalManifestSize > downloadedTotal) {
                                            etaAtomic.set((totalManifestSize - downloadedTotal) / newSpeed)
                                        }
                                    }
                                }

                                val lastDb = lastDbUpdateTimeAtomic.get()
                                if (now - lastDb >= 350L && lastDbUpdateTimeAtomic.compareAndSet(lastDb, now)) {
                                    val highestActive = activePartBytes.keys.maxOrNull() ?: (completedCount.get() + 1)
                                    serviceScope.launch {
                                        database.downloadDao().updateProgress(
                                            entity.id,
                                            DownloadState.DOWNLOADING.name,
                                            highestActive.coerceIn(1, totalParts),
                                            completedCount.get(),
                                            downloadedTotal,
                                            speedBpsAtomic.get(),
                                            etaAtomic.get()
                                        )
                                    }
                                    throttledUpdateNotifications()
                                }
                            }
                        )

                        if (pausingFlags[entity.id]?.get() != true && partFile.exists()) {
                            activePartBytes.remove(part.index)
                            completedBytes.addAndGet(partFile.length())
                            val doneCount = completedCount.incrementAndGet()
                            val downloadedTotal = getTotalDownloaded()
                            lastBytesForSpeedAtomic.set(downloadedTotal)
                            val highestActive = activePartBytes.keys.maxOrNull() ?: doneCount

                            serviceScope.launch {
                                database.downloadDao().updateProgress(
                                    entity.id,
                                    DownloadState.DOWNLOADING.name,
                                    highestActive.coerceIn(1, totalParts),
                                    doneCount,
                                    downloadedTotal,
                                    speedBpsAtomic.get(),
                                    etaAtomic.get()
                                )
                            }
                            throttledUpdateNotifications()
                        }
                    }
                }
            }
        }

        // Si se pausó durante la descarga, no ensamblar todavía
        if (pausingFlags[entity.id]?.get() == true) {
            Log.d(TAG, "Pipeline paused before assembly: ${entity.id}")
            return
        }

        val allPartsPresent = orderedParts.all { File(partsDirectory, "%05d.part".format(it.index)).exists() }
        if (!allPartsPresent) {
            if (pausingFlags[entity.id]?.get() == true) return
            throw IOException("No se completaron todos los fragmentos para ensamblar.")
        }

        // Reconstrucción del archivo uniendo físicamente las partes y verificando
        // tamaño + SHA-256 antes de publicarlo como descarga completada.
        val assembled = assembleFinalFile(partsDirectory, orderedParts, entity.fileName)
        val mediaStoreUri = assembled.uri
        val actualSize = assembled.size
        val calculatedSha256 = assembled.sha256
        val expectedSha256 = manifest.sha256?.trim()?.lowercase()

        if (actualSize != totalManifestSize) {
            try { contentResolver.delete(mediaStoreUri, null, null) } catch (_: Exception) {}
            // Sin tamaño por fragmento no podemos saber cuál parte está corrupta;
            // descartar los fragmentos evita que un retry reutilice datos incorrectos.
            partsDirectory.listFiles()?.forEach { it.delete() }
            throw IllegalStateException(
                "El tamaño reconstruido no coincide con el manifiesto: " +
                    "$actualSize bytes recibidos, $totalManifestSize esperados."
            )
        }

        if (!expectedSha256.isNullOrBlank() && calculatedSha256.lowercase() != expectedSha256) {
            try { contentResolver.delete(mediaStoreUri, null, null) } catch (_: Exception) {}
            // Un hash final incorrecto no permite identificar qué fragmento falló;
            // limpiar las partes fuerza una reconstrucción limpia en el siguiente retry.
            partsDirectory.listFiles()?.forEach { it.delete() }
            throw IllegalStateException("La verificación SHA-256 falló: el archivo recibido no coincide con el manifiesto.")
        }

        // El archivo solo se conserva si pasó las verificaciones.
        partsDirectory.listFiles()?.forEach { it.delete() }
        partsDirectory.delete()

        val completedAt = System.currentTimeMillis()
        retryAttempts.remove(entity.id)
        database.downloadDao().markCompleted(
            id = entity.id,
            status = DownloadState.COMPLETED.name,
            uri = mediaStoreUri.toString(),
            path = "Download/Chunk/${entity.fileName}",
            sha256 = calculatedSha256,
            bytes = actualSize,
            completedAt = completedAt
        )

        Log.d(TAG, "Download completed and verified: ${entity.fileName}, sha256=$calculatedSha256")
        notificationManager.cancel(entity.id.hashCode())
        showCompletedNotification(entity.fileName, mediaStoreUri)
    }

    private fun downloadPartWithRange(
        downloadId: String,
        partUrl: String,
        tempFile: File,
        partFile: File,
        onProgress: (Long) -> Unit
    ) {
        var allowResume = true
        var attemptCount = 0

        while (true) {
            attemptCount++
            val existingBytes = if (allowResume && tempFile.exists()) tempFile.length() else 0L
            if (!allowResume && tempFile.exists()) {
                tempFile.delete()
            }

            val requestBuilder = Request.Builder().url(partUrl)
            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val request = requestBuilder.build()
            val call = client.newCall(request)
            activeCalls.computeIfAbsent(downloadId) { ConcurrentHashMap.newKeySet() }.add(call)

            val response: Response
            try {
                response = call.execute()
            } catch (e: IOException) {
                activeCalls[downloadId]?.remove(call)
                if (pausingFlags[downloadId]?.get() == true) return
                throw e
            }

            // Manejo de servidores o enlaces que no admiten reanudación (HTTP 416, 400, 405, 501 con Range)
            if (existingBytes > 0L && (response.code == 416 || response.code == 400 || response.code == 405 || response.code == 501)) {
                val contentRange = response.header("Content-Range")
                val totalSize = parseUnsatisfiedRangeTotal(contentRange)
                if (response.code == 416 && totalSize != null && tempFile.exists() && tempFile.length() == totalSize) {
                    response.close()
                    activeCalls[downloadId]?.remove(call)
                    if (partFile.exists()) partFile.delete()
                    if (!tempFile.renameTo(partFile)) {
                        throw IOException("No se pudo finalizar el fragmento descargado.")
                    }
                    onProgress(partFile.length())
                    return
                }

                Log.w(TAG, "Enlace no admite reanudación (HTTP ${response.code}). Reiniciando descarga de este fragmento desde 0...")
                response.close()
                activeCalls[downloadId]?.remove(call)
                allowResume = false
                tempFile.delete()
                onProgress(0L)
                if (attemptCount <= 2) continue else throw IOException("El servidor rechazó la descarga del fragmento.")
            }

            if (!response.isSuccessful) {
                activeCalls[downloadId]?.remove(call)
                response.close()
                if (existingBytes > 0L && attemptCount <= 2) {
                    Log.w(TAG, "Fallo HTTP ${response.code} con Range. Intentando descarga limpia sin Range desde 0...")
                    allowResume = false
                    tempFile.delete()
                    onProgress(0L)
                    continue
                }
                if (response.code == 401 || response.code == 403 || response.code == 404) {
                    throw PermanentDownloadException("El servidor rechazó el enlace (HTTP ${response.code}).")
                }
                throw IOException("Error HTTP ${response.code} descargando fragmento.")
            }

            val isAppend = response.code == 206 && existingBytes > 0L
            if (isAppend) {
                val contentRange = response.header("Content-Range")
                val rangeStart = parseContentRangeStart(contentRange)
                if (rangeStart != null && rangeStart != existingBytes) {
                    activeCalls[downloadId]?.remove(call)
                    response.close()
                    Log.w(TAG, "Rango devuelto ($rangeStart) distinto de esperado ($existingBytes). Reiniciando desde 0...")
                    allowResume = false
                    tempFile.delete()
                    onProgress(0L)
                    if (attemptCount <= 2) continue else throw IOException("Rango no compatible.")
                }
            } else if (existingBytes > 0L && response.code == 200) {
                // El servidor ignoró Range y envió el fragmento completo (HTTP 200)
                Log.i(TAG, "Servidor no soporta Range (HTTP 200). Reiniciando fragmento desde 0...")
                tempFile.delete()
            }

            val expectedResponseBytes = response.body?.contentLength()?.takeIf { it >= 0L }
            val body = response.body ?: run {
                activeCalls[downloadId]?.remove(call)
                response.close()
                throw IOException("Cuerpo de respuesta vacío.")
            }

            var totalWritten = if (isAppend) existingBytes else 0L
            var responseBytesWritten = 0L

            var throttleStartTime = System.currentTimeMillis()
            var bytesWrittenInWindow = 0L

            try {
                body.byteStream().use { input ->
                    FileOutputStream(tempFile, isAppend).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            if (pausingFlags[downloadId]?.get() == true) {
                                output.write(buffer, 0, read)
                                totalWritten += read
                                responseBytesWritten += read
                                output.flush()
                                onProgress(totalWritten)
                                Log.d(TAG, "Flushed and paused chunk cleanly: totalWritten=$totalWritten")
                                break
                            }

                            output.write(buffer, 0, read)
                            totalWritten += read
                            responseBytesWritten += read
                            bytesWrittenInWindow += read
                            onProgress(totalWritten)

                            val currentSpeedLimitBps = settingsManager.settings.value.speedLimit.bytesPerSec
                            if (currentSpeedLimitBps > 0) {
                                val elapsedMs = (System.currentTimeMillis() - throttleStartTime).coerceAtLeast(1L)
                                val expectedMs = (bytesWrittenInWindow * 1000L) / currentSpeedLimitBps
                                if (expectedMs > elapsedMs) {
                                    val sleepMs = (expectedMs - elapsedMs).coerceIn(1L, 400L)
                                    SystemClock.sleep(sleepMs)
                                }
                                if (elapsedMs >= 1000L) {
                                    throttleStartTime = System.currentTimeMillis()
                                    bytesWrittenInWindow = 0L
                                }
                            } else {
                                throttleStartTime = System.currentTimeMillis()
                                bytesWrittenInWindow = 0L
                            }
                        }
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                if (pausingFlags[downloadId]?.get() == true || e is CancellationException) {
                    Log.d(TAG, "Lectura de stream interrumpida de forma limpia por pausa: $downloadId")
                    return
                }
                throw e
            } finally {
                response.close()
                activeCalls[downloadId]?.remove(call)
            }

            if (pausingFlags[downloadId]?.get() == true) return

            if (expectedResponseBytes != null && responseBytesWritten != expectedResponseBytes) {
                throw IOException(
                    "La respuesta del fragmento quedó incompleta: $responseBytesWritten bytes recibidos de $expectedResponseBytes."
                )
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                throw IOException("El fragmento terminó sin datos.")
            }

            if (partFile.exists()) partFile.delete()
            if (!tempFile.renameTo(partFile)) {
                throw IOException("No se pudo finalizar el fragmento descargado.")
            }
            onProgress(partFile.length())
            return
        }
    }

    private fun parseContentRangeStart(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        val match = Regex("^bytes\\s+(\\d+)-\\d+/\\d+$", RegexOption.IGNORE_CASE).find(contentRange.trim())
        return match?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun parseUnsatisfiedRangeTotal(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        val match = Regex("^bytes\\s+\\*/(\\d+)$", RegexOption.IGNORE_CASE).find(contentRange.trim())
        return match?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private data class AssembledFile(
        val uri: Uri,
        val size: Long,
        val sha256: String
    )

    private fun assembleFinalFile(
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        fileName: String
    ): AssembledFile {
        val resolver = contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, FileUtils.getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Chunk")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("No se pudo registrar el archivo en MediaStore.")

        val digest = MessageDigest.getInstance("SHA-256")
        var totalWritten = 0L
        try {
            resolver.openOutputStream(uri, "w")?.use { rawOut ->
                val digestOut = DigestOutputStream(rawOut, digest)
                val buffer = ByteArray(BUFFER_SIZE)
                for (part in orderedParts) {
                    val f = File(partsDirectory, "%05d.part".format(part.index))
                    if (!f.exists()) throw IOException("Falta fragmento ${part.index} para reconstruir.")
                    FileInputStream(f).use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            digestOut.write(buffer, 0, read)
                            totalWritten += read
                        }
                    }
                }
                digestOut.flush()
            } ?: throw IOException("No se pudo escribir en el destino final.")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            if (resolver.update(uri, contentValues, null, null) <= 0) {
                throw IOException("No se pudo finalizar el archivo en MediaStore.")
            }

            val calculatedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            return AssembledFile(uri, totalWritten, calculatedSha256)
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            throw e
        }
    }

    private fun calculateDownloadedBytes(
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        upToPartIndex: Int
    ): Long {
        var sum = 0L
        for (part in orderedParts) {
            if (part.index <= upToPartIndex) {
                val f = File(partsDirectory, "%05d.part".format(part.index))
                if (f.exists()) {
                    sum += f.length()
                }
            }
        }
        return sum
    }

    private fun handleDownloadError(downloadId: String, e: Exception) {
        val settings = settingsManager.settings.value
        val networkUnavailable = !isNetworkAvailable()
        val isVerificationError = e is IllegalStateException
        val isPermanentError = e is PermanentDownloadException || isVerificationError

        serviceScope.launch {
            if (networkUnavailable && !isPermanentError) {
                database.downloadDao().updateStatus(
                    downloadId,
                    DownloadState.PAUSED.name,
                    "Sin conexión a internet. Esperando conexión...",
                    pausedByNetwork = true
                )
            } else if (settings.autoRetry && e is IOException && !isPermanentError) {
                val attempt = (retryAttempts[downloadId]?.incrementAndGet() ?: 1)
                if (attempt <= settings.maxRetries) {
                    database.downloadDao().updateStatus(
                        downloadId,
                        DownloadState.QUEUED.name,
                        "Reintentando ($attempt/${settings.maxRetries})...",
                        pausedByNetwork = false
                    )
                    throttledUpdateNotifications()
                    delay((1000L * attempt).coerceAtMost(10_000L))
                    if (database.downloadDao().getDownloadById(downloadId)?.status == DownloadState.QUEUED.name) {
                        processQueue()
                    }
                    return@launch
                }

                val msg = e.localizedMessage ?: "Se agotaron los reintentos."
                retryAttempts.remove(downloadId)
                database.downloadDao().markFailed(downloadId, DownloadState.ERROR.name, msg, System.currentTimeMillis())
                showErrorNotification(downloadId, "Descarga fallida", msg)
            } else {
                val msg = e.localizedMessage ?: "Error desconocido durante la descarga."
                retryAttempts.remove(downloadId)
                database.downloadDao().markFailed(downloadId, DownloadState.ERROR.name, msg, System.currentTimeMillis())
                showErrorNotification(downloadId, "Descarga fallida", msg)
            }
            throttledUpdateNotifications()
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val builder = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                val settings = settingsManager.settings.value
                if (settings.autoRetry) {
                    serviceScope.launch {
                        delay(1200)
                        val pending = database.downloadDao().getUnfinishedDownloads()
                        for (item in pending) {
                            if (item.pausedByNetwork) {
                                Log.d(TAG, "Auto-resuming network-paused download: ${item.id}")
                                database.downloadDao().updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
                            }
                        }
                        processQueue()
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: pausing active downloads with pausedByNetwork=true")
                serviceScope.launch {
                    val active = database.downloadDao().getCurrentlyDownloading()
                    for (item in active) {
                        pausingFlags[item.id]?.set(true)
                        activeCalls[item.id]?.forEach { try { it.cancel() } catch (_: Exception) {} }
                        activeJobs[item.id]?.cancelAndJoin()
                        activeJobs.remove(item.id)
                        database.downloadDao().updateStatus(item.id, DownloadState.PAUSED.name, "Sin conexión a internet.", pausedByNetwork = true)
                    }
                    releaseLocksIfIdle()
                    throttledUpdateNotifications()
                }
            }
        }

        try {
            cm.registerNetworkCallback(builder.build(), networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun observeSettingsChanges() {
        serviceScope.launch {
            settingsManager.settings.collect { settings ->
                Log.d(TAG, "Settings updated in real-time: wifiOnly=${settings.wifiOnly}, maxConcurrent=${settings.maxConcurrentDownloads}, limit=${settings.speedLimit}")

                // 1. Reaccionar a cambios en Solo con Wi-Fi en tiempo real
                if (settings.wifiOnly && !isWifiConnected()) {
                    val downloading = database.downloadDao().getCurrentlyDownloading()
                    for (item in downloading) {
                        Log.d(TAG, "Real-time pause by Wi-Fi only setting: ${item.id}")
                        database.downloadDao().updateStatus(
                            item.id,
                            DownloadState.PAUSED.name,
                            "Pausada: Solo con Wi-Fi está activado.",
                            pausedByNetwork = true
                        )
                        pausingFlags[item.id]?.set(true)
                        activeCalls[item.id]?.forEach { try { it.cancel() } catch (_: Exception) {} }
                    }
                    releaseLocksIfIdle()
                    throttledUpdateNotifications()
                } else if (!settings.wifiOnly && isNetworkAvailable()) {
                    val pausedByNet = database.downloadDao().getUnfinishedDownloads().filter { it.pausedByNetwork }
                    for (item in pausedByNet) {
                        database.downloadDao().updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
                    }
                    processQueue()
                }

                // 2. Reaccionar a cambios en límite de descargas simultáneas en tiempo real
                processQueue()

                // 3. Reaccionar a cambios en notificaciones de progreso en tiempo real
                if (!settings.showProgressNotifications) {
                    notificationManager.cancel(NOTIFICATION_SUMMARY_ID)
                    val downloading = database.downloadDao().getCurrentlyDownloading()
                    for (item in downloading) {
                        notificationManager.cancel(item.id.hashCode())
                    }
                    val paused = database.downloadDao().getUnfinishedDownloads().filter { it.status == DownloadState.PAUSED.name }
                    for (item in paused) {
                        notificationManager.cancel(item.id.hashCode())
                    }
                } else {
                    throttledUpdateNotifications()
                }
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun restoreActiveDownloads() {
        serviceScope.launch {
            val unfinished = database.downloadDao().getUnfinishedDownloads()
            for (item in unfinished) {
                // Las que estaban descargando vuelven a la cola respetando el límite
                if (item.status == DownloadState.DOWNLOADING.name || item.status == DownloadState.PAUSING.name) {
                    database.downloadDao().updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
                }
            }
            processQueue()
        }
    }

    private fun initLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadChunk:WakeLock")

            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DownloadChunk:WifiLock")
        } catch (_: Exception) {}
    }

    private fun acquireLocks() {
        try {
            wakeLock?.let { if (!it.isHeld) it.acquire(10 * 60 * 1000L) }
            wifiLock?.let { if (!it.isHeld) it.acquire() }
        } catch (_: Exception) {}
    }

    private fun releaseLocksIfIdle() {
        if (activeJobs.isEmpty()) {
            try {
                wakeLock?.let { if (it.isHeld) it.release() }
                wifiLock?.let { if (it.isHeld) it.release() }
            } catch (_: Exception) {}
        }
    }

    /**
     * Limita la actualización de notificaciones a máximo 2 por segundo (500ms)
     */
    private fun throttledUpdateNotifications() {
        serviceScope.launch {
            notificationUpdateMutex.withLock {
                val now = SystemClock.elapsedRealtime()
                if (now - lastNotificationUpdateTime < 500L) {
                    return@withLock
                }
                lastNotificationUpdateTime = now
                updateNotificationsInternal()
            }
        }
    }

    private suspend fun updateNotificationsInternal() {
        val settings = settingsManager.settings.value
        if (!settings.showProgressNotifications) {
            notificationManager.cancel(NOTIFICATION_SUMMARY_ID)
            return
        }

        val activeList = database.downloadDao().getCurrentlyDownloading()
        val queuedList = database.downloadDao().getQueuedDownloads()
        val pausedList = database.downloadDao().getUnfinishedDownloads().filter { it.status == DownloadState.PAUSED.name }

        val totalActive = activeList.size
        val totalQueued = queuedList.size

        if (totalActive == 0 && totalQueued == 0 && pausedList.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_SUMMARY_ID)
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        // 1. Notificación de Resumen agrupada (Foreground)
        val summaryNotification = buildSummaryNotification(totalActive, totalQueued, activeList)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_SUMMARY_ID, summaryNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_SUMMARY_ID, summaryNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed startForeground: ${e.message}")
        }

        // 2. Notificaciones hijas para cada descarga activa o pausada
        for (item in activeList) {
            val childNotif = buildChildNotification(item, isPaused = false)
            notificationManager.notify(item.id.hashCode(), childNotif)
        }

        for (item in pausedList) {
            val childNotif = buildChildNotification(item, isPaused = true)
            notificationManager.notify(item.id.hashCode(), childNotif)
        }
    }

    private fun buildSummaryNotification(
        activeCount: Int,
        queuedCount: Int,
        activeList: List<DownloadEntity>
    ): Notification {
        val contentIntent = createOpenAppPendingIntent()

        val summaryTitle = if (activeCount > 0 && queuedCount > 0) {
            "$activeCount descargas activas · $queuedCount en cola"
        } else if (activeCount > 0) {
            "$activeCount descargas activas"
        } else {
            "$queuedCount descargas en cola"
        }

        var totalBytes = 0L
        var downloadedBytes = 0L
        for (item in activeList) {
            totalBytes += item.totalBytes
            downloadedBytes += item.downloadedBytes
        }
        val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99) else 0

        val builder = NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setContentTitle(summaryTitle)
            .setContentText(if (activeCount > 0) "$percent% completado" else "En espera de turno")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (activeCount > 0) {
            builder.setProgress(100, percent, false)

            val pauseAllIntent = createActionPendingIntent(ACTION_PAUSE_ALL, null, 9001)
            builder.addAction(android.R.drawable.ic_media_pause, "Pausar todo", pauseAllIntent)
        }

        val cancelAllIntent = createActionPendingIntent(ACTION_CANCEL_ALL, null, 9002)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar todo", cancelAllIntent)

        return builder.build()
    }

    private fun buildChildNotification(
        item: DownloadEntity,
        isPaused: Boolean
    ): Notification {
        val contentIntent = createOpenAppPendingIntent()

        val percent = if (item.totalBytes > 0) {
            ((item.downloadedBytes * 100) / item.totalBytes).toInt().coerceIn(0, 99)
        } else 0

        val speedStr = if (item.speedBps > 0 && !isPaused) " • ${FileUtils.formatBytes(item.speedBps)}/s" else ""
        val contentText = if (isPaused) {
            "En pausa • $percent%"
        } else {
            "Parte ${item.currentPart} de ${item.totalParts} • $percent%$speedStr"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setContentTitle(item.fileName)
            .setContentText(contentText)
            .setSmallIcon(if (isPaused) android.R.drawable.ic_media_pause else android.R.drawable.stat_sys_download)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (!isPaused) {
            builder.setProgress(100, percent, percent == 0)
            val pauseIntent = createActionPendingIntent(ACTION_PAUSE, item.id, item.id.hashCode() + 10)
            builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pauseIntent)
        } else {
            val resumeIntent = createActionPendingIntent(ACTION_RESUME, item.id, item.id.hashCode() + 20)
            builder.addAction(android.R.drawable.ic_media_play, "Reanudar", resumeIntent)
        }

        val cancelIntent = createActionPendingIntent(ACTION_CANCEL, item.id, item.id.hashCode() + 30)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelIntent)

        return builder.build()
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra(MainActivity.EXTRA_TARGET_TAB, 1) // Abrir en la pestaña "En curso"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            500,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createActionPendingIntent(action: String, downloadId: String?, requestCode: Int): PendingIntent {
        val intent = Intent(this, DownloadActionReceiver::class.java).apply {
            this.action = action
            if (downloadId != null) {
                putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, downloadId)
            }
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureForegroundStarted() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
                .setContentTitle("Download Chunk")
                .setContentText("Servicio de descargas activo")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_SUMMARY_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_SUMMARY_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureForegroundStarted error: ${e.message}")
        }
    }

    private fun showCompletedNotification(fileName: String, uri: Uri) {
        val settings = settingsManager.settings.value

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingShare = PendingIntent.getActivity(
            this,
            fileName.hashCode(),
            Intent.createChooser(shareIntent, "Compartir $fileName"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_COMPLETED_ID)
            .setContentTitle("Descarga completada")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_share, "Compartir", pendingShare)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (settings.soundOnComplete) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            notificationManager.notify(fileName.hashCode() + 500, builder.build())
        } catch (_: Exception) {}

        if (settings.vibrateOnComplete) {
            triggerVibration()
        }
    }

    private fun showErrorNotification(downloadId: String, fileName: String, errorMsg: String) {
        val retryIntent = createActionPendingIntent(ACTION_RETRY, downloadId, downloadId.hashCode() + 40)

        val builder = NotificationCompat.Builder(this, CHANNEL_ERROR_ID)
            .setContentTitle("Fallo en descarga: $fileName")
            .setContentText(errorMsg)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Reintentar", retryIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            notificationManager.notify(downloadId.hashCode() + 600, builder.build())
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    private fun stopSelfIfIdle() {
        serviceScope.launch {
            try {
                val downloading = database.downloadDao().getCurrentlyDownloading()
                val queued = database.downloadDao().getQueuedDownloads()
                if (activeJobs.isEmpty() && downloading.isEmpty() && queued.isEmpty()) {
                    val paused = database.downloadDao().getUnfinishedDownloads().filter { it.status == DownloadState.PAUSED.name }
                    if (paused.isEmpty()) {
                        try {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } catch (_: Exception) {}
                    } else {
                        throttledUpdateNotifications()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in stopSelfIfIdle: ${t.message}")
            }
        }
    }

    private fun coroutineScopeIsActive(): Boolean = serviceScope.isActive

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de las descargas en segundo plano"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_ID,
                "Completadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de descargas completadas con éxito"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val errorChannel = NotificationChannel(
                CHANNEL_ERROR_ID,
                "Errores",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos de fallos o interrupciones en descargas"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(progressChannel, completedChannel, errorChannel))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DownloadService onDestroy")
        serviceScope.cancel()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try { cm?.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        releaseLocksIfIdle()
    }
}
