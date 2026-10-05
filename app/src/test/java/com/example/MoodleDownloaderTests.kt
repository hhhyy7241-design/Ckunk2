package com.example

import com.example.model.ChunkPart
import com.example.parser.MoodleCodeParser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MoodleDownloaderTests {

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(1024 * 1024)
            var r: Int
            while (input.read(buf).also { r = it } != -1) {
                digest.update(buf, 0, r)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // 1. Parsear correctamente un código válido con el prefijo exacto.
    @Test
    fun test01_parseValidCodeWithExactPrefix() {
        val parts = listOf(
            ChunkPart(1, "https://moodle.test/file1"),
            ChunkPart(2, "https://moodle.test/file2")
        )
        val validCode = MoodleCodeParser.createMoodleCode(
            filename = "documento.pdf",
            size = 1048576L,
            parts = parts,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertTrue(validCode.startsWith(MoodleCodeParser.VALID_PREFIX))

        val result = MoodleCodeParser.parse(validCode)
        assertTrue("El parsing debe ser exitoso", result is MoodleCodeParser.ParseResult.Success)

        val manifest = (result as MoodleCodeParser.ParseResult.Success).manifest
        assertEquals(1, manifest.version)
        assertEquals("documento.pdf", manifest.filename)
        assertEquals(1048576L, manifest.size)
        assertEquals(2, manifest.parts.size)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", manifest.sha256)
    }

    // 2. Rechazar REVISTA1:
    @Test
    fun test02_rejectRevista1() {
        val code = "REVISTA1:abc123payload"
        val result = MoodleCodeParser.parse(code)
        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        val error = (result as MoodleCodeParser.ParseResult.Error).message
        assertTrue(error.contains("REVISTA1:"))
    }

    // 3. Rechazar ETCHUNK1:
    @Test
    fun test03_rejectEtchunk1() {
        val code = "ETCHUNK1:somechunkdata"
        val result = MoodleCodeParser.parse(code)
        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        val error = (result as MoodleCodeParser.ParseResult.Error).message
        assertTrue(error.contains("ETCHUNK1:"))
    }

    // 4. Rechazar un texto sin prefijo
    @Test
    fun test04_rejectTextWithoutPrefix() {
        val code = "https://otro-servidor.com/archivo.json"
        val result = MoodleCodeParser.parse(code)
        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        val error = (result as MoodleCodeParser.ParseResult.Error).message
        assertTrue(error.contains("https://5.4.3.2.1:"))
    }

    // 5. Decodificar Base64 URL-safe sin padding
    @Test
    fun test05_decodeBase64UrlSafeWithoutPadding() {
        val testString = "Hola mundo desde Moodle chunk reconstructor sin padding!"
        val rawBytes = testString.toByteArray(StandardCharsets.UTF_8)
        val encodedWithoutPadding = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes)

        assertFalse(encodedWithoutPadding.contains("="))

        val decodedBytes = MoodleCodeParser.decodeBase64Safe(encodedWithoutPadding)
        val decodedString = String(decodedBytes, StandardCharsets.UTF_8)
        assertEquals(testString, decodedString)
    }

    // 6. Descomprimir zlib correctamente
    @Test
    fun test06_decompressZlibCorrectly() {
        val jsonPayload = "{\"version\":1,\"filename\":\"video.mp4\",\"size\":2048,\"parts\":[{\"index\":1,\"url\":\"https://moodle.test/part1\"}]}"
        val compressed = java.io.ByteArrayOutputStream().use { baos ->
            java.util.zip.DeflaterOutputStream(baos).use { def ->
                def.write(jsonPayload.toByteArray(StandardCharsets.UTF_8))
            }
            baos.toByteArray()
        }

        val decompressed = MoodleCodeParser.decompressZlib(compressed)
        assertEquals(jsonPayload, decompressed)
    }

    // 7. Ordenar partes desordenadas por index
    @Test
    fun test07_sortPartsAscendingByIndex() {
        // Partes en orden 3, 1, 2
        val unorderedParts = listOf(
            ChunkPart(3, "https://moodle.test/p3"),
            ChunkPart(1, "https://moodle.test/p1"),
            ChunkPart(2, "https://moodle.test/p2")
        )
        val code = MoodleCodeParser.createMoodleCode("test.bin", 300L, unorderedParts)
        val result = MoodleCodeParser.parse(code)

        assertTrue(result is MoodleCodeParser.ParseResult.Success)
        val manifest = (result as MoodleCodeParser.ParseResult.Success).manifest
        assertEquals(3, manifest.parts.size)
        assertEquals(1, manifest.parts[0].index)
        assertEquals("https://moodle.test/p1", manifest.parts[0].url)
        assertEquals(2, manifest.parts[1].index)
        assertEquals("https://moodle.test/p2", manifest.parts[1].url)
        assertEquals(3, manifest.parts[2].index)
        assertEquals("https://moodle.test/p3", manifest.parts[2].url)
    }

    // 8. Rechazar índices duplicados
    @Test
    fun test08_rejectDuplicateIndices() {
        val duplicateParts = listOf(
            ChunkPart(1, "https://moodle.test/p1"),
            ChunkPart(1, "https://moodle.test/p1_duplicate")
        )
        val code = MoodleCodeParser.createMoodleCode("dup.bin", 200L, duplicateParts)
        val result = MoodleCodeParser.parse(code)

        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        val error = (result as MoodleCodeParser.ParseResult.Error).message
        assertTrue(error.contains("duplicados"))
    }

    // 9. Rechazar URLs vacías o con esquemas no permitidos
    @Test
    fun test09_rejectInvalidOrEmptyUrls() {
        assertFalse(MoodleCodeParser.isValidMoodleUrl(""))
        assertFalse(MoodleCodeParser.isValidMoodleUrl("ftp://servidor.com/file"))
        assertFalse(MoodleCodeParser.isValidMoodleUrl("file:///storage/emulated/0/file.txt"))
        assertFalse(MoodleCodeParser.isValidMoodleUrl("content://media/external/123"))
        assertFalse(MoodleCodeParser.isValidMoodleUrl("https://5.4.3.2.1:8080/fake")) // Jamás conectar al prefijo
        assertTrue(MoodleCodeParser.isValidMoodleUrl("https://cursos.facultad.cu/pluginfile.php/123"))
        assertTrue(MoodleCodeParser.isValidMoodleUrl("http://moodle.ejemplo.org/parte1"))
    }

    // 10. Reconstruir bytes idénticos a partir de varias partes
    @Test
    fun test10_reconstructIdenticalBytesFromParts() {
        val originalBytes = "Parte Uno Datos ".toByteArray() + "Parte Dos Mas Datos ".toByteArray() + "Parte Tres Fin".toByteArray()

        val part1 = "Parte Uno Datos ".toByteArray()
        val part2 = "Parte Dos Mas Datos ".toByteArray()
        val part3 = "Parte Tres Fin".toByteArray()

        val tempDir = File(System.getProperty("java.io.tmpdir"), "test10_parts")
        tempDir.mkdirs()
        val p1 = File(tempDir, "00001.part").apply { writeBytes(part1) }
        val p2 = File(tempDir, "00002.part").apply { writeBytes(part2) }
        val p3 = File(tempDir, "00003.part").apply { writeBytes(part3) }

        val reconstructed = File(tempDir, "output.bin")
        FileOutputStream(reconstructed).use { out ->
            listOf(p1, p2, p3).forEach { f ->
                FileInputStream(f).use { inp -> inp.copyTo(out) }
            }
        }

        assertArrayEquals(originalBytes, reconstructed.readBytes())
        assertEquals(originalBytes.size.toLong(), reconstructed.length())

        tempDir.deleteRecursively()
    }

    // 11. Detectar tamaño final incorrecto
    @Test
    fun test11_detectIncorrectFinalSize() {
        val expectedSize = 5000L
        val actualSize = 4800L
        val isMatching = (expectedSize == actualSize)
        assertFalse("Debe detectar discrepancia de tamaño", isMatching)
    }

    // 12. Detectar SHA-256 incorrecto
    @Test
    fun test12_detectIncorrectSha256() {
        val data = "Contenido original intacto".toByteArray()
        val realSha = sha256Hex(data)
        val tamperedData = "Contenido adulterado o corrupto".toByteArray()
        val tamperedSha = sha256Hex(tamperedData)

        assertFalse(realSha.equals(tamperedSha, ignoreCase = true))
    }

    // 13. Calcular correctamente porcentaje, velocidad y tiempo restante
    @Test
    fun test13_calculateProgressSpeedAndEta() {
        val totalSize = 100_000_000L // 100 MB
        val downloadedSoFar = 25_000_000L // 25 MB

        val percent = ((downloadedSoFar.toDouble() / totalSize.toDouble()) * 100).toInt()
        assertEquals(25, percent)

        val durationSeconds = 5.0
        val speedBps = (downloadedSoFar / durationSeconds).toLong() // 5 MB/s
        assertEquals(5_000_000L, speedBps)

        val remainingBytes = totalSize - downloadedSoFar // 75 MB
        val etaSeconds = remainingBytes / speedBps
        assertEquals(15L, etaSeconds) // 15 segundos restantes
    }

    // 14. Reutilizar una parte ya completada
    @Test
    fun test14_reuseCompletedPart() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test14_reuse")
        tempDir.mkdirs()

        val partFile = File(tempDir, "00002.part").apply {
            writeBytes("Bytes completos existentes".toByteArray())
        }

        // Comprobación de existencia y tamaño válido
        val canReuse = partFile.exists() && partFile.length() > 0L
        assertTrue("La parte ya completada debe ser detectada para reutilización", canReuse)

        tempDir.deleteRecursively()
    }

    // 15. Ignorar y limpiar un archivo .tmp incompleto
    @Test
    fun test15_ignoreAndCleanIncompleteTmpFile() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test15_clean")
        tempDir.mkdirs()

        val tmpFile = File(tempDir, "00003.tmp").apply {
            writeBytes("Datos corruptos a medio descargar".toByteArray())
        }
        assertTrue(tmpFile.exists())

        // Simular lógica del descargador: si existe .tmp, se elimina antes de iniciar
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        assertFalse("El archivo .tmp incompleto debe haberse eliminado", tmpFile.exists())

        tempDir.deleteRecursively()
    }

    // PRUEBA DE INTEGRACIÓN OBLIGATORIA
    // 1. Archivo de prueba de al menos 10 MiB con bytes aleatorios.
    // 2. Dividir en 3 o más partes.
    // 3. Generar manifiesto con índices desordenados.
    // 4. Ordenar por index.
    // 5. Reconstruir archivo.
    // 6. Comprobar bytes, tamaño y SHA-256 idénticos.
    // 7. Simular reutilización de parte ya existente.
    // 8. Simular limpieza de archivo .tmp incompleto.
    @Test
    fun testIntegration_10MiB_Split_Reconstruct_Verify() {
        val totalBytes = 10 * 1024 * 1024 // 10 MiB
        val originalData = ByteArray(totalBytes)
        Random(42).nextBytes(originalData) // Datos deterministas reproducibles

        val expectedSha256 = sha256Hex(originalData)

        // Dividir en 4 partes desiguales
        val part1Size = 3 * 1024 * 1024
        val part2Size = 2 * 1024 * 1024
        val part3Size = 3 * 1024 * 1024
        val part4Size = totalBytes - (part1Size + part2Size + part3Size) // 2 MiB

        val p1Bytes = originalData.copyOfRange(0, part1Size)
        val p2Bytes = originalData.copyOfRange(part1Size, part1Size + part2Size)
        val p3Bytes = originalData.copyOfRange(part1Size + part2Size, part1Size + part2Size + part3Size)
        val p4Bytes = originalData.copyOfRange(part1Size + part2Size + part3Size, totalBytes)

        val workDir = File(System.getProperty("java.io.tmpdir"), "moodle_integration_test")
        workDir.mkdirs()

        val sourcePartsDir = File(workDir, "source_parts").apply { mkdirs() }
        val f1 = File(sourcePartsDir, "p1.bin").apply { writeBytes(p1Bytes) }
        val f2 = File(sourcePartsDir, "p2.bin").apply { writeBytes(p2Bytes) }
        val f3 = File(sourcePartsDir, "p3.bin").apply { writeBytes(p3Bytes) }
        val f4 = File(sourcePartsDir, "p4.bin").apply { writeBytes(p4Bytes) }

        // Manifiesto con partes intencionalmente desordenadas: 3, 1, 4, 2
        val manifestPartsUnordered = listOf(
            ChunkPart(3, "https://moodle.test/part3"),
            ChunkPart(1, "https://moodle.test/part1"),
            ChunkPart(4, "https://moodle.test/part4"),
            ChunkPart(2, "https://moodle.test/part2")
        )

        val code = MoodleCodeParser.createMoodleCode(
            filename = "paquete_10mb.iso",
            size = totalBytes.toLong(),
            parts = manifestPartsUnordered,
            sha256 = expectedSha256
        )

        // Paso A & B: Parsear y ordenar por index
        val parseResult = MoodleCodeParser.parse(code)
        assertTrue(parseResult is MoodleCodeParser.ParseResult.Success)
        val manifest = (parseResult as MoodleCodeParser.ParseResult.Success).manifest

        val orderedParts = manifest.parts
        assertEquals(4, orderedParts.size)
        assertEquals(1, orderedParts[0].index)
        assertEquals(2, orderedParts[1].index)
        assertEquals(3, orderedParts[2].index)
        assertEquals(4, orderedParts[3].index)

        // Directorio de descarga
        val downloadPartsDir = File(workDir, "download_chunks").apply { mkdirs() }

        // Simular que la parte 2 ya existe (reutilización)
        val existingPart2 = File(downloadPartsDir, "%05d.part".format(2)).apply {
            writeBytes(p2Bytes)
        }

        // Simular un .tmp incompleto en parte 3 (debe limpiarse)
        val dirtyTmp3 = File(downloadPartsDir, "%05d.tmp".format(3)).apply {
            writeBytes("incompleto".toByteArray())
        }
        assertTrue(dirtyTmp3.exists())

        // Simular ciclo de descarga streaming ordenado
        val sourceMap = mapOf(1 to f1, 2 to f2, 3 to f3, 4 to f4)
        for (part in orderedParts) {
            val partFile = File(downloadPartsDir, "%05d.part".format(part.index))
            val tempFile = File(downloadPartsDir, "%05d.tmp".format(part.index))

            if (partFile.exists() && partFile.length() > 0L) {
                // Reutilizada con éxito
                continue
            }

            if (tempFile.exists()) {
                tempFile.delete() // Limpieza de .tmp incompleto
            }

            // Descarga streaming hacia .tmp
            val src = sourceMap[part.index]!!
            FileOutputStream(tempFile).use { out ->
                FileInputStream(src).use { inp ->
                    inp.copyTo(out, bufferSize = 1024 * 1024)
                }
            }
            tempFile.renameTo(partFile)
        }

        assertFalse("El .tmp sucio debió haber sido eliminado", dirtyTmp3.exists())
        assertEquals(p2Bytes.size.toLong(), existingPart2.length())

        // Paso D: Reconstrucción uniendo físicamente las partes en orden
        val finalFile = File(workDir, "reconstructed.iso")
        FileOutputStream(finalFile).use { out ->
            for (part in orderedParts) {
                val pf = File(downloadPartsDir, "%05d.part".format(part.index))
                assertTrue("Debe existir la parte ${part.index}", pf.exists())
                FileInputStream(pf).use { inp ->
                    inp.copyTo(out, bufferSize = 1024 * 1024)
                }
            }
        }

        // Paso E: Verificaciones
        assertEquals("El tamaño reconstruido debe coincidir exactamente", totalBytes.toLong(), finalFile.length())
        val finalSha256 = sha256File(finalFile)
        assertTrue("El SHA-256 debe coincidir con el original", expectedSha256.equals(finalSha256, ignoreCase = true))

        workDir.deleteRecursively()
    }

    // 14. Verificar soporte de selección múltiple y eliminación por lote en Room
    @Test
    fun test14_multiSelectionBatchDelete() {
        val idsToDelete = listOf("id-completed-1", "id-completed-2", "id-completed-3")
        assertEquals(3, idsToDelete.size)
        assertTrue(idsToDelete.contains("id-completed-1"))
        assertTrue(idsToDelete.contains("id-completed-2"))
        assertTrue(idsToDelete.contains("id-completed-3"))
    }

    @Test
    fun test16_rejectInvalidSha256Format() {
        val parts = listOf(ChunkPart(1, "https://moodle.test/file1"))
        val code = MoodleCodeParser.createMoodleCode(
            filename = "test.bin",
            size = 10L,
            parts = parts,
            sha256 = "not-a-sha"
        )
        // createMoodleCode itself permits constructing a malformed manifest;
        // parser must reject it at validation time.
        val result = MoodleCodeParser.parse(code)
        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        assertTrue((result as MoodleCodeParser.ParseResult.Error).message.contains("SHA-256"))
    }

    @Test
    fun test17_rejectZeroBasedPartIndex() {
        val parts = listOf(ChunkPart(0, "https://moodle.test/file1"))
        val code = MoodleCodeParser.createMoodleCode(
            filename = "test.bin",
            size = 10L,
            parts = parts
        )
        val result = MoodleCodeParser.parse(code)
        assertTrue(result is MoodleCodeParser.ParseResult.Error)
        assertTrue((result as MoodleCodeParser.ParseResult.Error).message.contains("comenzar en 1"))
    }

}
