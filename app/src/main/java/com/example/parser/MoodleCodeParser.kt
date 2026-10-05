package com.example.parser

import com.example.model.ChunkPart
import com.example.model.MoodleManifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object MoodleCodeParser {

    const val VALID_PREFIX = "https://5.4.3.2.1:"
    const val FORBIDDEN_REVISTA = "REVISTA1:"
    const val FORBIDDEN_ETCHUNK = "ETCHUNK1:"

    sealed class ParseResult {
        data class Success(val manifest: MoodleManifest) : ParseResult()
        data class Error(val message: String, val detail: String? = null) : ParseResult()
    }

    /**
     * Valida y analiza un código de texto generado por el bot.
     */
    fun parse(code: String): ParseResult {
        val trimmed = code.trim()

        if (trimmed.isEmpty()) {
            return ParseResult.Error("El código está vacío.")
        }

        if (trimmed.startsWith(FORBIDDEN_REVISTA, ignoreCase = true)) {
            return ParseResult.Error("Formato no admitido: Los códigos con prefijo 'REVISTA1:' están rechazados.")
        }

        if (trimmed.startsWith(FORBIDDEN_ETCHUNK, ignoreCase = true)) {
            return ParseResult.Error("Formato no admitido: Los códigos con prefijo 'ETCHUNK1:' están rechazados.")
        }

        if (!trimmed.startsWith(VALID_PREFIX)) {
            return ParseResult.Error("El código debe comenzar por $VALID_PREFIX")
        }

        val rawPayload = trimmed.substring(VALID_PREFIX.length).trim()
        if (rawPayload.isEmpty()) {
            return ParseResult.Error("El código está corrupto o incompleto.")
        }

        // 1. Reponer el padding Base64 necesario antes de decodificar: '=' hasta múltiplo de 4
        val paddedPayload = padBase64(rawPayload)

        // 2. Decodificar Base64 URL-safe
        val compressedBytes = try {
            decodeBase64Safe(paddedPayload)
        } catch (e: Exception) {
            return ParseResult.Error("El código está corrupto o incompleto.", e.localizedMessage)
        }

        // 3. Descomprimir con zlib
        val jsonString = try {
            decompressZlib(compressedBytes)
        } catch (e: Exception) {
            return ParseResult.Error("El código está corrupto o incompleto.", e.localizedMessage)
        }

        // 4. Parsear JSON y validar
        return try {
            val manifest = parseJson(jsonString, trimmed)
            ParseResult.Success(manifest)
        } catch (e: IllegalArgumentException) {
            ParseResult.Error(e.message ?: "Manifiesto inválido.")
        } catch (e: Exception) {
            ParseResult.Error("El código está corrupto o incompleto.", e.localizedMessage)
        }
    }

    fun padBase64(input: String): String {
        val clean = input.replace("\\s".toRegex(), "")
        val remainder = clean.length % 4
        return if (remainder == 0) {
            clean
        } else {
            clean + "=".repeat(4 - remainder)
        }
    }

    fun decodeBase64Safe(input: String): ByteArray {
        val clean = padBase64(input.replace("\\s".toRegex(), ""))
        return try {
            android.util.Base64.decode(clean, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            try {
                android.util.Base64.decode(clean, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
            } catch (_: Throwable) {
                try {
                    java.util.Base64.getUrlDecoder().decode(clean)
                } catch (_: Throwable) {
                    java.util.Base64.getDecoder().decode(clean)
                }
            }
        }
    }

    fun encodeBase64Safe(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP).trimEnd('=')
        } catch (_: Throwable) {
            java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }

    fun decompressZlib(compressedBytes: ByteArray): String {
        return try {
            val inflater = Inflater(false)
            val input = ByteArrayInputStream(compressedBytes)
            val inflaterStream = InflaterInputStream(input, inflater)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var read: Int
            while (inflaterStream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            inflater.end()
            output.toString(StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            val inflaterRaw = Inflater(true)
            val input = ByteArrayInputStream(compressedBytes)
            val inflaterStream = InflaterInputStream(input, inflaterRaw)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var read: Int
            while (inflaterStream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            inflaterRaw.end()
            output.toString(StandardCharsets.UTF_8.name())
        }
    }

    private fun parseJson(jsonString: String, rawCode: String): MoodleManifest {
        val root = JSONObject(jsonString)

        val version = root.optInt("version", -1)
        if (version != 1) {
            throw IllegalArgumentException("La versión del manifiesto no es compatible.")
        }

        val rawFilename = root.optString("filename")
        if (rawFilename.isBlank()) {
            throw IllegalArgumentException("El manifiesto no contiene un nombre de archivo válido.")
        }
        val filename = sanitizeFilename(rawFilename)

        if (!root.has("size")) {
            throw IllegalArgumentException("El manifiesto no especifica el tamaño del archivo.")
        }
        val size = root.optLong("size", -1L)
        if (size < 0) {
            throw IllegalArgumentException("El tamaño especificado en el manifiesto no puede ser negativo.")
        }

        val sha256 = if (root.has("sha256") && !root.isNull("sha256")) {
            val rawSha = root.optString("sha256", "").trim()
            if (rawSha.isNotBlank() && rawSha != "null" && rawSha != "undefined") {
                if (!rawSha.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                    throw IllegalArgumentException("El SHA-256 del manifiesto no tiene un formato válido.")
                }
                rawSha.lowercase()
            } else {
                null
            }
        } else {
            null
        }
        val created = root.optLong("created", -1L).takeIf { it > 0 }

        val partsArray = root.optJSONArray("parts")
            ?: throw IllegalArgumentException("No se encontraron partes.")

        if (partsArray.length() == 0) {
            throw IllegalArgumentException("No se encontraron partes.")
        }

        val rawParts = mutableListOf<ChunkPart>()
        val seenIndices = mutableSetOf<Int>()

        for (i in 0 until partsArray.length()) {
            val partObj = partsArray.optJSONObject(i)
                ?: throw IllegalArgumentException("Estructura de partes inválida en el manifiesto.")

            if (!partObj.has("index")) {
                throw IllegalArgumentException("La parte ${i + 1} no tiene un índice válido.")
            }
            val index = partObj.getInt("index")
            if (index <= 0) {
                throw IllegalArgumentException("Índice de parte no válido: $index. Los índices deben comenzar en 1.")
            }
            if (seenIndices.contains(index)) {
                throw IllegalArgumentException("Se detectaron índices de partes duplicados ($index).")
            }
            seenIndices.add(index)

            val url = partObj.optString("url").trim()
            if (url.isBlank() || !isValidMoodleUrl(url)) {
                throw IllegalArgumentException("La URL de una parte no es válida.")
            }

            rawParts.add(ChunkPart(index = index, url = url))
        }

        val orderedParts = rawParts.sortedBy { it.index }
        val expectedIndices = (1..orderedParts.size).toList()
        val actualIndices = orderedParts.map { it.index }
        if (actualIndices != expectedIndices) {
            throw IllegalArgumentException(
                "Los índices de las partes deben ser consecutivos desde 1. " +
                    "Se recibieron: ${actualIndices.joinToString(", ")}."
            )
        }

        return MoodleManifest(
            version = version,
            filename = filename,
            size = size,
            sha256 = sha256,
            created = created,
            parts = orderedParts,
            rawCode = rawCode
        )
    }

    fun sanitizeFilename(filename: String): String {
        val baseName = filename.substringAfterLast('/').substringAfterLast('\\')
        val clean = baseName.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
        if (clean.isBlank() || clean == "." || clean == "..") {
            return "archivo_descargado"
        }
        return clean
    }

    fun isValidMoodleUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false
        }
        if (lower.contains("5.4.3.2.1")) {
            return false
        }
        return true
    }

    fun getCodeFingerprint(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(code.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(24)
    }

    fun createMoodleCode(
        filename: String,
        size: Long,
        parts: List<ChunkPart>,
        sha256: String? = null
    ): String {
        val json = JSONObject().apply {
            put("version", 1)
            put("filename", filename)
            put("size", size)
            if (sha256 != null) put("sha256", sha256)
            put("created", System.currentTimeMillis() / 1000)

            val partsArray = JSONArray()
            parts.forEach { part ->
                val p = JSONObject().apply {
                    put("index", part.index)
                    put("url", part.url)
                }
                partsArray.put(p)
            }
            put("parts", partsArray)
        }

        val jsonBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val deflater = Deflater()
        deflater.setInput(jsonBytes)
        deflater.finish()

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()

        val compressed = output.toByteArray()
        val base64 = encodeBase64Safe(compressed)

        return "$VALID_PREFIX$base64"
    }
}
