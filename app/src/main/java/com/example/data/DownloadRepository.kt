package com.example.data

import android.content.Context
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.service.DownloadService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DownloadRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val downloadingDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadingDownloadsFlow()
    val queuedDownloads: Flow<List<DownloadEntity>> = downloadDao.getQueuedDownloadsFlow()
    val pausedDownloads: Flow<List<DownloadEntity>> = downloadDao.getPausedDownloadsFlow()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    suspend fun enqueueDownload(
        code: String,
        manifest: MoodleManifest,
        customFileName: String? = null
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val finalFileName = customFileName?.ifBlank { null } ?: manifest.filename
        val maxPos = downloadDao.getMaxQueuePosition() ?: 0

        val entity = DownloadEntity(
            id = downloadId,
            fileName = finalFileName,
            totalBytes = manifest.size,
            totalParts = manifest.parts.size,
            completedParts = 0,
            currentPart = 0,
            downloadedBytes = 0L,
            status = DownloadState.QUEUED.name,
            savedPath = "Download/Chunk/$finalFileName",
            code = code,
            sha256Expected = manifest.sha256,
            queuePosition = maxPos + 1,
            pausedByNetwork = false,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.insert(entity)

        // Delegar arranque o encolado al DownloadService
        DownloadService.startDownload(context, downloadId)

        return downloadId
    }

    suspend fun pauseDownload(id: String) {
        DownloadService.pauseDownload(context, id)
    }

    suspend fun resumeDownload(id: String) {
        DownloadService.resumeDownload(context, id)
    }

    suspend fun retryDownload(id: String) {
        DownloadService.retryDownload(context, id)
    }

    suspend fun cancelDownload(id: String) {
        DownloadService.cancelDownload(context, id)
    }

    suspend fun pauseAll() {
        DownloadService.pauseAll(context)
    }

    suspend fun resumeAll() {
        DownloadService.resumeAll(context)
    }

    suspend fun forceStartNow(id: String) {
        DownloadService.forceStartNow(context, id)
    }

    suspend fun moveToTop(id: String) {
        DownloadService.moveToTop(context, id)
    }

    suspend fun reorderQueue(orderedIds: List<String>) {
        for ((index, id) in orderedIds.withIndex()) {
            downloadDao.updateQueuePosition(id, index)
        }
    }

    suspend fun deleteDownload(id: String) {
        DownloadService.cancelDownload(context, id)
        downloadDao.deleteById(id)
    }

    suspend fun deleteSelectedCompleted(ids: List<String>) {
        downloadDao.deleteByIds(ids)
    }

    suspend fun clearCompleted() {
        downloadDao.deleteCompleted()
    }

    suspend fun resumePendingDownloadsOnStartup() {
        try {
            val pending = downloadDao.getUnfinishedDownloads()
            for (item in pending) {
                if (item.status == DownloadState.DOWNLOADING.name || item.status == DownloadState.PAUSING.name) {
                    downloadDao.updateStatus(item.id, DownloadState.QUEUED.name, null, pausedByNetwork = false)
                }
            }
        } catch (_: Exception) {}
    }
}
