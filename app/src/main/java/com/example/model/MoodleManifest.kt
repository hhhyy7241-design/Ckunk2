package com.example.model

data class ChunkPart(
    val index: Int,
    val url: String
)

data class MoodleManifest(
    val version: Int,
    val filename: String,
    val size: Long,
    val sha256: String?,
    val created: Long?,
    val parts: List<ChunkPart>,
    val rawCode: String
)

enum class DownloadState(val label: String) {
    QUEUED("En cola"),
    DOWNLOADING("Descargando"),
    PAUSING("Pausando"),
    PAUSED("Pausada"),
    ERROR("Error"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada"),

    // Aliases para compatibilidad interna
    PREPARING("Preparando"),
    DOWNLOADING_PART("Descargando parte"),
    REUSING_COMPLETED_PART("Reutilizando parte"),
    RECONSTRUCTING("Reconstruyendo"),
    RETRYING("Reintentando"),
    FAILED("Error");

    companion object {
        fun fromString(value: String): DownloadState {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                when (value.uppercase()) {
                    "EN_COLA", "PREPARING" -> QUEUED
                    "DESCARGANDO", "DOWNLOADING_PART", "REUSING_COMPLETED_PART", "RECONSTRUCTING" -> DOWNLOADING
                    "PAUSANDO" -> PAUSING
                    "PAUSADA" -> PAUSED
                    "ERROR", "FAILED" -> ERROR
                    "COMPLETADA", "COMPLETED", "COMPLETED_WARN_HASH" -> COMPLETED
                    "CANCELADA", "CANCELLED" -> CANCELLED
                    else -> QUEUED
                }
            }
        }
    }
}
