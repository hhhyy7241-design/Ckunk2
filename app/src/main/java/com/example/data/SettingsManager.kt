package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AddDownloadBehavior(val label: String) {
    START_IMMEDIATELY("Empezar de inmediato (si hay lugar)"),
    ADD_TO_QUEUE_ONLY("Agregar a la cola sin iniciar")
}

enum class QueueSortOrder(val label: String) {
    FIFO("Primero en entrar, primero en salir"),
    SMALLEST_FIRST("Primero los archivos más pequeños")
}

enum class SpeedLimit(val bytesPerSec: Long, val label: String) {
    NO_LIMIT(0L, "Sin límite"),
    LIMIT_500KB(500 * 1024L, "500 KB/s"),
    LIMIT_1MB(1024 * 1024L, "1 MB/s"),
    LIMIT_2MB(2 * 1024 * 1024L, "2 MB/s")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val wifiOnly: Boolean = false,
    val autoRetry: Boolean = true,
    val maxRetries: Int = 3,
    val maxConcurrentDownloads: Int = 2,
    val maxConcurrentParts: Int = 2,
    val addDownloadBehavior: AddDownloadBehavior = AddDownloadBehavior.START_IMMEDIATELY,
    val autoStartNext: Boolean = true,
    val queueSortOrder: QueueSortOrder = QueueSortOrder.FIFO,
    val speedLimit: SpeedLimit = SpeedLimit.NO_LIMIT,
    val vibrateOnComplete: Boolean = true,
    val autoClearOnStart: Boolean = true,
    val showProgressNotifications: Boolean = true,
    val soundOnComplete: Boolean = true,
    val hasSeenWelcome: Boolean = false,
    val dontShowWelcomeAgain: Boolean = false,
    val lastWelcomeShownTime: Long = 0L,
    val autoStartAcknowledged: Boolean = false,
    val hasWarnedBatteryRestriction: Boolean = false
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("download_chunk_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val theme = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.SYSTEM }
        val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        val wifi = prefs.getBoolean(KEY_WIFI_ONLY, false)
        val retry = prefs.getBoolean(KEY_AUTO_RETRY, true)
        val maxRet = prefs.getInt(KEY_MAX_RETRIES, 3).coerceIn(1, 5)
        val maxConc = prefs.getInt(KEY_MAX_CONCURRENT, 2).coerceIn(1, 4)
        val maxParts = prefs.getInt(KEY_MAX_PARTS, 2).coerceIn(1, 4)

        val addBehaviorStr = prefs.getString(KEY_ADD_BEHAVIOR, AddDownloadBehavior.START_IMMEDIATELY.name)
        val addBehavior = try { AddDownloadBehavior.valueOf(addBehaviorStr ?: "") } catch (_: Exception) { AddDownloadBehavior.START_IMMEDIATELY }

        val autoNext = prefs.getBoolean(KEY_AUTO_START_NEXT, true)

        val sortOrderStr = prefs.getString(KEY_SORT_ORDER, QueueSortOrder.FIFO.name)
        val sortOrder = try { QueueSortOrder.valueOf(sortOrderStr ?: "") } catch (_: Exception) { QueueSortOrder.FIFO }

        val speedLimitStr = prefs.getString(KEY_SPEED_LIMIT, SpeedLimit.NO_LIMIT.name)
        val speedLimit = try { SpeedLimit.valueOf(speedLimitStr ?: "") } catch (_: Exception) { SpeedLimit.NO_LIMIT }

        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val autoClear = prefs.getBoolean(KEY_AUTO_CLEAR, true)
        val showNotifs = prefs.getBoolean(KEY_SHOW_PROGRESS_NOTIFS, true)
        val sound = prefs.getBoolean(KEY_SOUND_ON_COMPLETE, true)
        val seenWelcome = prefs.getBoolean(KEY_SEEN_WELCOME, false)
        val dontShowWelcome = prefs.getBoolean(KEY_DONT_SHOW_WELCOME, false)
        val lastWelcome = prefs.getLong(KEY_LAST_WELCOME_TIME, 0L)
        val autoStartAck = prefs.getBoolean(KEY_AUTO_START_ACK, false)
        val warnedBattery = prefs.getBoolean(KEY_WARNED_BATTERY, false)

        return AppSettings(
            themeMode = theme,
            dynamicColor = dynamic,
            wifiOnly = wifi,
            autoRetry = retry,
            maxRetries = maxRet,
            maxConcurrentDownloads = maxConc,
            maxConcurrentParts = maxParts,
            addDownloadBehavior = addBehavior,
            autoStartNext = autoNext,
            queueSortOrder = sortOrder,
            speedLimit = speedLimit,
            vibrateOnComplete = vibrate,
            autoClearOnStart = autoClear,
            showProgressNotifications = showNotifs,
            soundOnComplete = sound,
            hasSeenWelcome = seenWelcome,
            dontShowWelcomeAgain = dontShowWelcome,
            lastWelcomeShownTime = lastWelcome,
            autoStartAcknowledged = autoStartAck,
            hasWarnedBatteryRestriction = warnedBattery
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _settings.value = _settings.value.copy(wifiOnly = enabled)
    }

    fun setAutoRetry(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RETRY, enabled).apply()
        _settings.value = _settings.value.copy(autoRetry = enabled)
    }

    fun setMaxRetries(retries: Int) {
        val safe = retries.coerceIn(1, 5)
        prefs.edit().putInt(KEY_MAX_RETRIES, safe).apply()
        _settings.value = _settings.value.copy(maxRetries = safe)
    }

    fun setMaxConcurrentDownloads(limit: Int) {
        val safe = limit.coerceIn(1, 4)
        prefs.edit().putInt(KEY_MAX_CONCURRENT, safe).apply()
        _settings.value = _settings.value.copy(maxConcurrentDownloads = safe)
    }

    fun setMaxConcurrentParts(limit: Int) {
        val safe = limit.coerceIn(1, 4)
        prefs.edit().putInt(KEY_MAX_PARTS, safe).apply()
        _settings.value = _settings.value.copy(maxConcurrentParts = safe)
    }

    fun setAddDownloadBehavior(behavior: AddDownloadBehavior) {
        prefs.edit().putString(KEY_ADD_BEHAVIOR, behavior.name).apply()
        _settings.value = _settings.value.copy(addDownloadBehavior = behavior)
    }

    fun setAutoStartNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_NEXT, enabled).apply()
        _settings.value = _settings.value.copy(autoStartNext = enabled)
    }

    fun setQueueSortOrder(order: QueueSortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
        _settings.value = _settings.value.copy(queueSortOrder = order)
    }

    fun setSpeedLimit(limit: SpeedLimit) {
        prefs.edit().putString(KEY_SPEED_LIMIT, limit.name).apply()
        _settings.value = _settings.value.copy(speedLimit = limit)
    }

    fun setVibrateOnComplete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _settings.value = _settings.value.copy(vibrateOnComplete = enabled)
    }

    fun setAutoClearOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
        _settings.value = _settings.value.copy(autoClearOnStart = enabled)
    }

    fun setShowProgressNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PROGRESS_NOTIFS, enabled).apply()
        _settings.value = _settings.value.copy(showProgressNotifications = enabled)
    }

    fun setSoundOnComplete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ON_COMPLETE, enabled).apply()
        _settings.value = _settings.value.copy(soundOnComplete = enabled)
    }

    fun setHasSeenWelcome(seen: Boolean) {
        prefs.edit().putBoolean(KEY_SEEN_WELCOME, seen).apply()
        _settings.value = _settings.value.copy(hasSeenWelcome = seen)
    }

    fun setDontShowWelcomeAgain(dontShow: Boolean) {
        prefs.edit().putBoolean(KEY_DONT_SHOW_WELCOME, dontShow).apply()
        _settings.value = _settings.value.copy(dontShowWelcomeAgain = dontShow)
    }

    fun updateLastWelcomeShownTime(time: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_WELCOME_TIME, time).apply()
        _settings.value = _settings.value.copy(lastWelcomeShownTime = time)
    }

    fun setAutoStartAcknowledged(acknowledged: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_ACK, acknowledged).apply()
        _settings.value = _settings.value.copy(autoStartAcknowledged = acknowledged)
    }

    fun setHasWarnedBatteryRestriction(warned: Boolean) {
        prefs.edit().putBoolean(KEY_WARNED_BATTERY, warned).apply()
        _settings.value = _settings.value.copy(hasWarnedBatteryRestriction = warned)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_AUTO_RETRY = "key_auto_retry"
        private const val KEY_MAX_RETRIES = "key_max_retries"
        private const val KEY_MAX_CONCURRENT = "key_max_concurrent"
        private const val KEY_MAX_PARTS = "key_max_parts"
        private const val KEY_ADD_BEHAVIOR = "key_add_behavior"
        private const val KEY_AUTO_START_NEXT = "key_auto_start_next"
        private const val KEY_SORT_ORDER = "key_sort_order"
        private const val KEY_SPEED_LIMIT = "key_speed_limit"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_AUTO_CLEAR = "key_auto_clear"
        private const val KEY_SHOW_PROGRESS_NOTIFS = "key_show_progress_notifs"
        private const val KEY_SOUND_ON_COMPLETE = "key_sound_on_complete"
        private const val KEY_SEEN_WELCOME = "key_seen_welcome"
        private const val KEY_DONT_SHOW_WELCOME = "key_dont_show_welcome"
        private const val KEY_LAST_WELCOME_TIME = "key_last_welcome_time"
        private const val KEY_AUTO_START_ACK = "key_auto_start_ack"
        private const val KEY_WARNED_BATTERY = "key_warned_battery"
    }
}
