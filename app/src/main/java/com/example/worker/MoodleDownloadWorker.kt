package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.model.ChunkPart
import com.example.model.DownloadState
import com.example.parser.MoodleCodeParser
import com.example.util.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class MoodleDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_CODE = "key_code"
        const val KEY_CUSTOM_FILENAME = "key_custom_filename"

        const val PROGRESS_PERCENT = "progress_percent"
        const val PROGRESS_BYTES = "progress_bytes"
        const val PROGRESS_TOTAL_BYTES = "progress_total_bytes"
        const val PROGRESS_PART_INDEX = "progress_part_index"
        const val PROGRESS_TOTAL_PARTS = "progress_total_parts"
        const val PROGRESS_SPEED = "progress_speed"
        const val PROGRESS_ETA_SECONDS = "progress_eta_seconds"
        const val PROGRESS_STATE = "progress_state"
        const val PROGRESS_STATUS_MSG = "progress_status_msg"

        const val CHANNEL_ID = "download_chunk_channel"
        const val CHANNEL_NAME = "Download Chunk"
        private const val NOTIFICATION_ID_BASE = 2000
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MiB buffer
    }

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val database = AppDatabase.getDatabase(applicationContext)
    private val downloadDao = database.downloadDao()

    private val notificationId: Int
        get() = NOTIFICATION_ID_BASE + (inputData.getString(KEY_DOWNLOAD_ID)?.hashCode() ?: 1) % 8000

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var startTimeMillis = System.currentTimeMillis()
    private var lastBytesForSpeed = 0L
    private var lastSpeedCalcTime = System.currentTimeMillis()
    private var currentSpeedBps = 0L

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure(
            workDataOf("error" to "Identificador de descarga ausente.")
        )
        val rawCode = inputData.getString(KEY_CODE) ?: return Result.failure(
            workDataOf("error" to "Código de descarga ausente.")
        )
        val customFileName = inputData.getString(KEY_CUSTOM_FILENAME)

        createNotificationChannel()

        // Estado inicial de primer plano
        setForeground(
            createForegroundInfo(
                statusText = "Preparando descarga...",
                percent = 0,
                bytesDownloaded = 0L,
                totalBytes = 0L,
                partIndex = 0,
                totalParts = 0,
                state = DownloadState.PREPARING
            )
        )

        // 1. Validar y parsear código
        val parseResult = MoodleCodeParser.parse(rawCode)
        if (parseResult is MoodleCodeParser.ParseResult.Error) {
            val errorMsg = parseResult.message
            downloadDao.markFailed(downloadId, DownloadState.FAILED.name, errorMsg, System.currentTimeMillis())
            showFinishedNotification("Error en código", errorMsg, false)
            return Result.failure(workDataOf("error" to errorMsg))
        }

        val manifest = (parseResult as MoodleCodeParser.ParseResult.Success).manifest
        val finalFileName = customFileName?.ifBlank { null } ?: manifest.filename
        val orderedParts = manifest.parts
        val totalParts = orderedParts.size
        val totalManifestSize = manifest.size

        // 2. Directorio determinista de reanudación basado en huella SHA-256 del código
        val codeFingerprint = MoodleCodeParser.getCodeFingerprint(rawCode)
        val partsDirectory = File(applicationContext.filesDir, "chunks_$codeFingerprint")
        if (!partsDirectory.exists()) {
            partsDirectory.mkdirs()
        }

        startTimeMillis = System.currentTimeMillis()
        lastSpeedCalcTime = startTimeMillis

        var mediaStoreUri: Uri? = null

        try {
            downloadDao.updateProgress(downloadId, DownloadState.DOWNLOADING.name, 0, 0, 0L, 0L, 0L)

            // Paso C: Descargar cada parte en un archivo temporal
            for (part in orderedParts) {
                if (isStopped) {
                    cleanIncompleteTmp(partsDirectory, part.index)
                    downloadDao.updateProgress(downloadId, DownloadState.CANCELLED.name, part.index, part.index - 1, calculateTotalExistingBytes(partsDirectory, orderedParts), 0L, 0L)
                    return Result.failure(workDataOf("error" to "Descarga cancelada por el usuario."))
                }

                val partFile = File(partsDirectory, "%05d.part".format(part.index))
                val tempFile = File(partsDirectory, "%05d.tmp".format(part.index))

                // Comprobar si la parte ya existe y es válida
                if (partFile.exists() && partFile.length() > 0L) {
                    reportProgress(
                        downloadId = downloadId,
                        state = DownloadState.REUSING_COMPLETED_PART,
                        statusText = "Reutilizando parte ${part.index} de $totalParts...",
                        partsDirectory = partsDirectory,
                        orderedParts = orderedParts,
                        currentPartIndex = part.index,
                        totalParts = totalParts,
                        manifestSize = totalManifestSize,
                        currentPartBytes = 0L
                    )
                    continue
                }

                // Si existe .tmp incompleto, eliminarlo
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                // Descargar parte mediante streaming a .tmp
                downloadPartWithRetries(
                    part = part,
                    tempFile = tempFile,
                    partFile = partFile,
                    downloadId = downloadId,
                    partsDirectory = partsDirectory,
                    orderedParts = orderedParts,
                    totalParts = totalParts,
                    manifestSize = totalManifestSize
                )
            }

            // Paso D: Reconstrucción uniendo físicamente las partes
            reportProgress(
                downloadId = downloadId,
                state = DownloadState.RECONSTRUCTING,
                statusText = "Reconstruyendo archivo...",
                partsDirectory = partsDirectory,
                orderedParts = orderedParts,
                currentPartIndex = totalParts,
                totalParts = totalParts,
                manifestSize = totalManifestSize,
                currentPartBytes = 0L
            )
            downloadDao.updateProgress(downloadId, DownloadState.RECONSTRUCTING.name, totalParts, totalParts, totalManifestSize, 0L, 0L)

            // Crear archivo en MediaStore con IS_PENDING = 1 en Download/Chunk
            val resolver = applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, FileUtils.getMimeType(finalFileName))
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Chunk")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }

            mediaStoreUri = resolver.insert(collectionUri, contentValues)
                ?: throw IOException("No se pudo registrar el archivo en MediaStore Download/Chunk.")

            // Streaming concatenando las partes en orden y calculando SHA-256 al vuelo
            val digest = MessageDigest.getInstance("SHA-256")
            resolver.openOutputStream(mediaStoreUri, "w")?.use { rawOutput ->
                val digestOutput = DigestOutputStream(rawOutput, digest)
                val buffer = ByteArray(BUFFER_SIZE)
                for (part in orderedParts) {
                    if (isStopped) throw CancellationException("Operación cancelada.")
                    val partFile = File(partsDirectory, "%05d.part".format(part.index))
                    if (!partFile.exists()) {
                        throw IOException("Falta el archivo de la parte ${part.index}.")
                    }
                    FileInputStream(partFile).use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            digestOutput.write(buffer, 0, read)
                        }
                    }
                }
                digestOutput.flush()
            } ?: throw IOException("No se pudo abrir el flujo de escritura en MediaStore.")

            // Paso E: Hash calculado directamente de los bytes transferidos
            val calculatedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val expectedSha256 = manifest.sha256?.trim()
            val hashMatches = expectedSha256.isNullOrBlank() || calculatedSha256.equals(expectedSha256, ignoreCase = true)

            // Finalización exitosa: publicar IS_PENDING = 0 (el archivo queda listo y visible en Download/Chunk)
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(mediaStoreUri, contentValues, null, null)

            // Limpieza de partes temporales completadas
            partsDirectory.listFiles()?.forEach { it.delete() }
            partsDirectory.delete()

            val completedAt = System.currentTimeMillis()
            val finalStatus = if (hashMatches) DownloadState.COMPLETED.name else "COMPLETED_WARN_HASH"

            downloadDao.markCompleted(
                id = downloadId,
                status = finalStatus,
                uri = mediaStoreUri.toString(),
                path = "Download/Chunk/$finalFileName",
                sha256 = calculatedSha256,
                bytes = totalManifestSize,
                completedAt = completedAt
            )

            val successMsg = if (hashMatches) {
                "Archivo $finalFileName (${FileUtils.formatBytes(totalManifestSize)}) guardado en Download/Chunk."
            } else {
                "Archivo $finalFileName (${FileUtils.formatBytes(totalManifestSize)}) guardado (aviso: el hash del servidor difiere)."
            }

            showFinishedNotification(
                title = if (hashMatches) "Descarga completada" else "Descarga completada (con aviso)",
                message = successMsg,
                success = true,
                uri = mediaStoreUri
            )

            return Result.success(
                workDataOf(
                    "filename" to finalFileName,
                    "size" to totalManifestSize,
                    "sha256" to calculatedSha256,
                    "uri" to mediaStoreUri.toString()
                )
            )

        } catch (e: CancellationException) {
            mediaStoreUri?.let { uri ->
                try { applicationContext.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            downloadDao.updateProgress(downloadId, DownloadState.CANCELLED.name, 0, 0, 0L, 0L, 0L)
            showFinishedNotification("Descarga cancelada", "La descarga fue cancelada por el usuario.", false)
            return Result.failure(workDataOf("error" to "Descarga cancelada."))
        } catch (e: Exception) {
            // Eliminar archivo incompleto de MediaStore ante fallo definitivo
            mediaStoreUri?.let { uri ->
                try { applicationContext.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            val errorMsg = e.localizedMessage ?: "Error desconocido durante la descarga."
            downloadDao.markFailed(downloadId, DownloadState.FAILED.name, errorMsg, System.currentTimeMillis())
            showFinishedNotification("Error en la descarga", errorMsg, false)
            return Result.failure(workDataOf("error" to errorMsg))
        }
    }

    private suspend fun downloadPartWithRetries(
        part: ChunkPart,
        tempFile: File,
        partFile: File,
        downloadId: String,
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        totalParts: Int,
        manifestSize: Long
    ) {
        var attempts = 0
        val maxAttempts = 4
        var lastException: Exception? = null

        while (attempts < maxAttempts) {
            if (isStopped) throw CancellationException("Operación cancelada.")
            attempts++
            try {
                executePartStreaming(
                    part = part,
                    tempFile = tempFile,
                    downloadId = downloadId,
                    partsDirectory = partsDirectory,
                    orderedParts = orderedParts,
                    totalParts = totalParts,
                    manifestSize = manifestSize
                )

                // Renombrar atómicamente .tmp a .part
                if (!tempFile.renameTo(partFile)) {
                    // Fallback para rename si los sistemas de archivos difieren
                    tempFile.copyTo(partFile, overwrite = true)
                    tempFile.delete()
                }
                return
            } catch (e: CancellationException) {
                tempFile.delete()
                throw e
            } catch (e: IOException) {
                lastException = e
                tempFile.delete() // Nunca confiar en una descarga interrumpida

                val message = e.message ?: ""
                // No reintentar indefinidamente errores de autorización o no encontrado
                if (message.contains("401") || message.contains("403")) {
                    throw IOException("La URL de Moodle caducó o requiere permisos.")
                }
                if (message.contains("404")) {
                    throw IOException("La parte ${part.index} no fue encontrada en el servidor Moodle (HTTP 404).")
                }

                if (attempts < maxAttempts) {
                    reportProgress(
                        downloadId = downloadId,
                        state = DownloadState.RETRYING,
                        statusText = "Reintentando parte ${part.index} (intento $attempts de $maxAttempts)...",
                        partsDirectory = partsDirectory,
                        orderedParts = orderedParts,
                        currentPartIndex = part.index,
                        totalParts = totalParts,
                        manifestSize = manifestSize,
                        currentPartBytes = 0L
                    )
                    delay(2000L * attempts)
                }
            }
        }

        throw lastException ?: IOException("Moodle rechazó la descarga de la parte ${part.index}.")
    }

    private fun executePartStreaming(
        part: ChunkPart,
        tempFile: File,
        downloadId: String,
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        totalParts: Int,
        manifestSize: Long
    ) {
        val request = Request.Builder()
            .url(part.url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/119.0 Firefox/119.0")
            .header("Accept", "*/*")
            .build()

        val response = client.newCall(request).execute()
        val statusCode = response.code

        if (!response.isSuccessful) {
            response.close()
            when (statusCode) {
                401, 403 -> throw IOException("La URL de Moodle caducó o requiere permisos.")
                404 -> throw IOException("La URL de Moodle no existe (HTTP 404).")
                408, 429, 500, 502, 503, 504 -> throw IOException("Error transitorio de red HTTP $statusCode.")
                else -> throw IOException("Moodle rechazó la descarga (HTTP $statusCode).")
            }
        }

        val body = response.body ?: run {
            response.close()
            throw IOException("Respuesta vacía del servidor Moodle.")
        }

        var partBytesWritten = 0L
        var lastReportTime = System.currentTimeMillis()

        try {
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            throw CancellationException("Descarga cancelada.")
                        }

                        output.write(buffer, 0, bytesRead)
                        partBytesWritten += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 500L) {
                            lastReportTime = now
                            calculateSpeed(partBytesWritten)
                            reportProgress(
                                downloadId = downloadId,
                                state = DownloadState.DOWNLOADING_PART,
                                statusText = "Descargando parte ${part.index} de $totalParts...",
                                partsDirectory = partsDirectory,
                                orderedParts = orderedParts,
                                currentPartIndex = part.index,
                                totalParts = totalParts,
                                manifestSize = manifestSize,
                                currentPartBytes = partBytesWritten
                            )
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            response.close()
        }
    }

    private fun calculateSpeed(currentPartBytes: Long) {
        val now = System.currentTimeMillis()
        val durationSec = (now - lastSpeedCalcTime) / 1000.0
        if (durationSec >= 1.0) {
            val bytesDelta = currentPartBytes - lastBytesForSpeed
            if (bytesDelta >= 0) {
                currentSpeedBps = (bytesDelta / durationSec).toLong()
            }
            lastBytesForSpeed = currentPartBytes
            lastSpeedCalcTime = now
        }
    }

    private fun reportProgress(
        downloadId: String,
        state: DownloadState,
        statusText: String,
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        currentPartIndex: Int,
        totalParts: Int,
        manifestSize: Long,
        currentPartBytes: Long
    ) {
        val completedBytes = calculateCompletedPartsBytes(partsDirectory, orderedParts, currentPartIndex)
        val totalDownloaded = completedBytes + currentPartBytes

        val percent = if (manifestSize > 0) {
            ((totalDownloaded.toDouble() / manifestSize.toDouble()) * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }

        val etaSeconds = if (currentSpeedBps > 0 && manifestSize > totalDownloaded) {
            (manifestSize - totalDownloaded) / currentSpeedBps
        } else {
            0L
        }

        setProgressAsync(
            workDataOf(
                PROGRESS_PERCENT to percent,
                PROGRESS_BYTES to totalDownloaded,
                PROGRESS_TOTAL_BYTES to manifestSize,
                PROGRESS_PART_INDEX to currentPartIndex,
                PROGRESS_TOTAL_PARTS to totalParts,
                PROGRESS_SPEED to currentSpeedBps,
                PROGRESS_ETA_SECONDS to etaSeconds,
                PROGRESS_STATE to state.name,
                PROGRESS_STATUS_MSG to statusText
            )
        )

        val speedText = if (currentSpeedBps > 0) " • ${FileUtils.formatBytes(currentSpeedBps)}/s" else ""
        val notifContent = if (totalParts > 1) {
            "Parte $currentPartIndex de $totalParts • $percent%$speedText"
        } else {
            "$percent%$speedText • ${FileUtils.formatBytes(totalDownloaded)}/${FileUtils.formatBytes(manifestSize)}"
        }

        val foregroundInfo = createForegroundInfo(
            statusText = notifContent,
            percent = percent,
            bytesDownloaded = totalDownloaded,
            totalBytes = manifestSize,
            partIndex = currentPartIndex,
            totalParts = totalParts,
            state = state
        )
        setForegroundAsync(foregroundInfo)
        notificationManager.notify(notificationId, foregroundInfo.notification)
    }

    private fun calculateCompletedPartsBytes(
        partsDirectory: File,
        orderedParts: List<ChunkPart>,
        currentPartIndex: Int
    ): Long {
        var sum = 0L
        for (part in orderedParts) {
            if (part.index < currentPartIndex) {
                val f = File(partsDirectory, "%05d.part".format(part.index))
                if (f.exists()) {
                    sum += f.length()
                }
            }
        }
        return sum
    }

    private fun calculateTotalExistingBytes(
        partsDirectory: File,
        orderedParts: List<ChunkPart>
    ): Long {
        var sum = 0L
        for (part in orderedParts) {
            val f = File(partsDirectory, "%05d.part".format(part.index))
            if (f.exists()) {
                sum += f.length()
            }
        }
        return sum
    }

    private fun cleanIncompleteTmp(partsDirectory: File, partIndex: Int) {
        val tmp = File(partsDirectory, "%05d.tmp".format(partIndex))
        if (tmp.exists()) {
            tmp.delete()
        }
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        return applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: -1L
    }

    private fun calculateSha256FromUri(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        applicationContext.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        } ?: throw IOException("No se pudo leer el archivo reconstruido para calcular SHA-256.")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(
            statusText = "Descargando archivo...",
            percent = 0,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            partIndex = 0,
            totalParts = 0,
            state = DownloadState.PREPARING
        )
    }

    private fun createForegroundInfo(
        statusText: String,
        percent: Int,
        bytesDownloaded: Long,
        totalBytes: Long,
        partIndex: Int,
        totalParts: Int,
        state: DownloadState
    ): ForegroundInfo {
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = androidx.work.WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)


        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download Chunk")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (state == DownloadState.DOWNLOADING_PART || state == DownloadState.REUSING_COMPLETED_PART) {
                    setProgress(100, percent.coerceIn(0, 99), false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun showFinishedNotification(
        title: String,
        message: String,
        success: Boolean,
        uri: Uri? = null
    ) {
        val intent = if (success && uri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, applicationContext.contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(applicationContext, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId + 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(notificationId, notification)

        // Respuesta háptica opcional al completar con éxito
        if (success) {
            val prefs = applicationContext.getSharedPreferences("download_chunk_prefs", Context.MODE_PRIVATE)
            val shouldVibrate = prefs.getBoolean("key_vibrate", true)
            if (shouldVibrate) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                        vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(180, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        val v = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                        v?.vibrate(180)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de descarga y reconstrucción de Download Chunk"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
