package com.example.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.text.DecimalFormat
import java.util.Locale

object FileUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.##")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0s"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    /**
     * Comprueba si el archivo físico aún existe en el almacenamiento del dispositivo
     * comprobando tanto el URI de MediaStore como las rutas estándar en Descargas/Chunk.
     */
    fun isFileInStorage(context: Context, uriString: String?, pathString: String?, fileName: String): Boolean {
        // 1. Probar a abrir el stream a través del ContentResolver (MediaStore)
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { return true }
            } catch (_: Exception) {}
        }

        // 2. Probar ruta especificada en entidad si existe
        if (!pathString.isNullOrBlank()) {
            try {
                val direct = File(Environment.getExternalStorageDirectory(), pathString)
                if (direct.exists() && direct.length() > 0L) return true
            } catch (_: Exception) {}
        }

        // 3. Probar carpeta pública Download/Chunk/fileName
        try {
            val chunkDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Chunk")
            val chunkFile = File(chunkDir, fileName)
            if (chunkFile.exists() && chunkFile.length() > 0L) return true
        } catch (_: Exception) {}

        // 4. Probar carpeta raíz Download/fileName
        try {
            val rootDownload = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (rootDownload.exists() && rootDownload.length() > 0L) return true
        } catch (_: Exception) {}

        return false
    }

    /**
     * Obtiene el tamaño real en bytes del archivo en almacenamiento si está disponible
     */
    fun getFileStorageSize(context: Context, uriString: String?, pathString: String?, fileName: String): Long {
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val statSize = pfd.statSize
                    if (statSize > 0L) return statSize
                }
            } catch (_: Exception) {}
        }

        try {
            val chunkDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Chunk")
            val chunkFile = File(chunkDir, fileName)
            if (chunkFile.exists() && chunkFile.length() > 0L) return chunkFile.length()
        } catch (_: Exception) {}

        return 0L
    }
}
