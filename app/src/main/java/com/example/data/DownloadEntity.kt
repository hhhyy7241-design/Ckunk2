package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val totalBytes: Long = 0L,
    val totalParts: Int = 1,
    val completedParts: Int = 0,
    val currentPart: Int = 0,
    val downloadedBytes: Long = 0L,
    val status: String, // QUEUED, DOWNLOADING, PAUSING, PAUSED, ERROR, COMPLETED, CANCELLED
    val savedPath: String? = null,
    val mediaStoreUri: String? = null,
    val code: String,
    val speedBps: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val sha256Calculated: String? = null,
    val sha256Expected: String? = null,
    val queuePosition: Int = 0,
    val pausedByNetwork: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
