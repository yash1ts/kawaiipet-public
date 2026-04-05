package com.kawaiipet.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class VoiceModel(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val sizeMb: Int,
    val type: ModelType
)

enum class ModelType { STT, TTS }

/** Absolute paths for Sherpa streaming transducer (encoder/decoder/joiner + tokens). */
data class SherpaStreamingTransducerPaths(
    val tokensPath: String,
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String
)

/** Piper VITS layout: *.onnx + tokens.txt + espeak-ng-data/. */
data class SherpaVitsTtsPaths(
    val modelPath: String,
    val tokensPath: String,
    val dataDirPath: String,
    val lexiconPath: String,
    val dictDirPath: String
)

/** Moonshine v2: encoder + merged decoder (.ort) + tokens.txt */
data class SherpaMoonshinePaths(
    val encoderPath: String,
    val mergedDecoderPath: String,
    val tokensPath: String
)

/** Kitten TTS: model.onnx + voices.bin + tokens.txt + espeak-ng-data/. */
data class SherpaKittenTtsPaths(
    val modelPath: String,
    val voicesPath: String,
    val tokensPath: String,
    val dataDirPath: String
)

class ModelManager(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val availableSTTModels = listOf(
        VoiceModel(
            id = FeaturedVoiceModels.MOONSHINE_TINY_EN_V2,
            name = "Moonshine v2 Tiny (English)",
            description = "Compact Moonshine v2 — good quality, small footprint",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27.tar.bz2",
            sizeMb = 42,
            type = ModelType.STT
        ),
        VoiceModel(
            id = "sherpa-onnx-streaming-zipformer-en",
            name = "Zipformer English (Streaming)",
            description = "Fast English streaming recognition",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2",
            sizeMb = 65,
            type = ModelType.STT
        ),
        VoiceModel(
            id = "sherpa-onnx-streaming-zipformer-bilingual",
            name = "Zipformer Bilingual (ZH/EN)",
            description = "Chinese + English streaming recognition",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2",
            sizeMb = 75,
            type = ModelType.STT
        )
    )

    val availableTTSModels = listOf(
        VoiceModel(
            id = FeaturedVoiceModels.KITTEN_NANO_EN_V0_1,
            name = "Kitten TTS (nano EN)",
            description = "KittenML nano English — small multi-voice model",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kitten-nano-en-v0_1-fp16.tar.bz2",
            sizeMb = 22,
            type = ModelType.TTS
        ),
        VoiceModel(
            id = "vits-piper-en-us-amy",
            name = "Amy (English US)",
            description = "Natural female voice",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2",
            sizeMb = 40,
            type = ModelType.TTS
        ),
        VoiceModel(
            id = "vits-piper-en-gb-alba",
            name = "Alba (English UK)",
            description = "British female voice",
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_GB-alba-medium.tar.bz2",
            sizeMb = 55,
            type = ModelType.TTS
        )
    )

    fun getModelDir(modelId: String): File = File(modelsDir, modelId)

    /**
     * Finds tokens.txt plus encoder/decoder/joiner *.onnx under [modelId] or its first subfolder
     * (tar archives usually add a versioned directory).
     */
    fun resolveSherpaStreamingTransducer(modelId: String): SherpaStreamingTransducerPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = buildList {
            add(base)
            base.listFiles()?.sortedBy { it.name }?.forEach { if (it.isDirectory) add(it) }
        }
        for (root in roots) {
            val tokens = File(root, "tokens.txt")
            if (!tokens.isFile) continue
            val encoder = pickSherpaOnnx(root, "encoder") ?: continue
            val decoder = pickSherpaOnnx(root, "decoder") ?: continue
            val joiner = pickSherpaOnnx(root, "joiner") ?: continue
            return SherpaStreamingTransducerPaths(
                tokensPath = tokens.absolutePath,
                encoderPath = encoder.absolutePath,
                decoderPath = decoder.absolutePath,
                joinerPath = joiner.absolutePath
            )
        }
        return null
    }

    /**
     * Finds a VITS/Piper TTS bundle: tokens.txt, espeak-ng-data/, and a voice *.onnx.
     */
    fun resolveSherpaVitsTts(modelId: String): SherpaVitsTtsPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = buildList {
            add(base)
            base.listFiles()?.sortedBy { it.name }?.forEach { if (it.isDirectory) add(it) }
        }
        for (root in roots) {
            val tokens = File(root, "tokens.txt")
            if (!tokens.isFile) continue
            val espeak = File(root, "espeak-ng-data")
            if (!espeak.isDirectory) continue
            val onnxFiles = root.listFiles()
                ?.filter { f -> f.isFile && f.name.endsWith(".onnx", ignoreCase = true) }
                .orEmpty()
            if (onnxFiles.isEmpty()) continue
            val modelFile = onnxFiles
                .filter { !it.name.contains(".int8.", ignoreCase = true) }
                .maxByOrNull { it.length() }
                ?: onnxFiles.maxByOrNull { it.length() }
                ?: continue
            val lex = File(root, "lexicon.txt")
            val lexPath = if (lex.isFile) lex.absolutePath else ""
            val dict = File(root, "dict")
            val dictPath = if (dict.isDirectory) dict.absolutePath else ""
            return SherpaVitsTtsPaths(
                modelPath = modelFile.absolutePath,
                tokensPath = tokens.absolutePath,
                dataDirPath = espeak.absolutePath,
                lexiconPath = lexPath,
                dictDirPath = dictPath
            )
        }
        return null
    }

    /**
     * Moonshine v2 English tiny: `encoder_model.ort`, `decoder_model_merged.ort`, `tokens.txt`.
     */
    fun resolveSherpaMoonshine(modelId: String): SherpaMoonshinePaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = buildList {
            add(base)
            base.listFiles()?.sortedBy { it.name }?.forEach { if (it.isDirectory) add(it) }
        }
        for (root in roots) {
            val tokens = File(root, "tokens.txt")
            if (!tokens.isFile) continue
            val encoder = root.listFiles()
                ?.firstOrNull { f ->
                    f.isFile && f.name.endsWith(".ort", ignoreCase = true) &&
                        f.name.startsWith("encoder", ignoreCase = true)
                } ?: continue
            val merged = root.listFiles()
                ?.firstOrNull { f ->
                    f.isFile && f.name.endsWith(".ort", ignoreCase = true) &&
                        f.name.contains("merged", ignoreCase = true)
                }
                ?: root.listFiles()?.firstOrNull { f ->
                    f.isFile && f.name.endsWith(".ort", ignoreCase = true) &&
                        f.name.startsWith("decoder", ignoreCase = true)
                }
                ?: continue
            return SherpaMoonshinePaths(
                encoderPath = encoder.absolutePath,
                mergedDecoderPath = merged.absolutePath,
                tokensPath = tokens.absolutePath
            )
        }
        return null
    }

    /**
     * Kitten TTS: `voices.bin`, `tokens.txt`, `espeak-ng-data/`, `model*.onnx`.
     */
    fun resolveSherpaKittenTts(modelId: String): SherpaKittenTtsPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = buildList {
            add(base)
            base.listFiles()?.sortedBy { it.name }?.forEach { if (it.isDirectory) add(it) }
        }
        for (root in roots) {
            val voices = File(root, "voices.bin")
            val tokens = File(root, "tokens.txt")
            val espeak = File(root, "espeak-ng-data")
            if (!voices.isFile || !tokens.isFile || !espeak.isDirectory) continue
            val onnxFiles = root.listFiles()
                ?.filter { f -> f.isFile && f.name.endsWith(".onnx", ignoreCase = true) }
                .orEmpty()
            if (onnxFiles.isEmpty()) continue
            val modelFile = onnxFiles
                .filter { !it.name.contains(".int8.", ignoreCase = true) }
                .maxByOrNull { it.length() }
                ?: onnxFiles.maxByOrNull { it.length() }
                ?: continue
            return SherpaKittenTtsPaths(
                modelPath = modelFile.absolutePath,
                voicesPath = voices.absolutePath,
                tokensPath = tokens.absolutePath,
                dataDirPath = espeak.absolutePath
            )
        }
        return null
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val dir = getModelDir(modelId)
        if (!dir.exists()) return false
        if (File(dir, MARKER_FILE).exists()) return true
        return hasUsableModelContent(dir)
    }

    suspend fun getDownloadedModelIds(): Set<String> = withContext(Dispatchers.IO) {
        (availableSTTModels + availableTTSModels)
            .map { it.id }
            .filter { isModelDownloaded(it) }
            .toSet()
    }

    /**
     * @param onProgress (overall 0f–1f, phase label). Download maps to ~0–88%, extraction to 88–100%.
     */
    suspend fun downloadModel(
        modelId: String,
        url: String,
        onProgress: (progress: Float, phase: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val targetDir = getModelDir(modelId)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val tempFile = File(context.cacheDir, "$modelId.tar.bz2")
        try {
            onProgress(0f, "Downloading…")
            downloadFile(url, tempFile) { downloadFraction ->
                // Reserve 12% for decompression
                onProgress(DOWNLOAD_WEIGHT * downloadFraction, "Downloading…")
            }

            onProgress(DOWNLOAD_WEIGHT, "Decompressing…")
            when {
                url.endsWith(".tar.bz2", ignoreCase = true) || url.endsWith(".tbz2", ignoreCase = true) ->
                    extractTarBz2(tempFile, targetDir) { extractFraction ->
                        onProgress(DOWNLOAD_WEIGHT + EXTRACT_WEIGHT * extractFraction, "Decompressing…")
                    }
                url.endsWith(".zip", ignoreCase = true) ->
                    extractZip(tempFile, targetDir) { extractFraction ->
                        onProgress(DOWNLOAD_WEIGHT + EXTRACT_WEIGHT * extractFraction, "Decompressing…")
                    }
                else -> extractTarBz2(tempFile, targetDir) { extractFraction ->
                    onProgress(DOWNLOAD_WEIGHT + EXTRACT_WEIGHT * extractFraction, "Decompressing…")
                }
            }

            onProgress(1f, "Finishing…")

            if (!hasUsableModelContent(targetDir)) {
                targetDir.deleteRecursively()
                return@withContext false
            }

            File(targetDir, MARKER_FILE).writeText("1")
            true
        } catch (_: Exception) {
            targetDir.deleteRecursively()
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun downloadFile(url: String, target: File, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress(downloadedBytes.toFloat() / totalBytes)
                    } else {
                        // Unknown Content-Length (common on GitHub): assume ~80MB model
                        val estMax = 80L * 1024L * 1024L
                        onProgress((downloadedBytes.toFloat() / estMax.toFloat()).coerceIn(0.02f, 0.99f))
                    }
                }
            }
        }
        onProgress(1f)
    }

    private fun extractTarBz2(archive: File, targetDir: File, onProgress: (Float) -> Unit) {
        val totalBytes = archive.length().coerceAtLeast(1L)
        val countingIn = CountingInputStream(FileInputStream(archive), totalBytes, onProgress)
        BufferedInputStream(countingIn).use { bis ->
            BZip2CompressorInputStream(bis).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    var entry: TarArchiveEntry? = tar.nextTarEntry
                    while (entry != null) {
                        val e = entry!!
                        val name = sanitizeEntryName(e.name)
                        if (name == null) {
                            tar.copyTo(DISCARD)
                        } else {
                            val outFile = File(targetDir, name)
                            if (e.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    tar.copyTo(fos)
                                }
                            }
                        }
                        entry = tar.nextTarEntry
                    }
                }
            }
        }
    }

    private fun extractZip(archive: File, targetDir: File, onProgress: (Float) -> Unit) {
        val totalBytes = archive.length().coerceAtLeast(1L)
        val countingIn = CountingInputStream(FileInputStream(archive), totalBytes, onProgress)
        ZipInputStream(countingIn).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val e = entry!!
                val name = sanitizeEntryName(e.name)
                if (name == null) {
                    zis.copyTo(DISCARD)
                } else {
                    val outFile = File(targetDir, name)
                    if (e.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun sanitizeEntryName(raw: String): String? {
        val name = raw.trim().trimStart('/').replace('\\', '/')
        if (name.isEmpty()) return null
        val parts = name.split('/')
        if (parts.any { it == ".." || it.contains("..") }) return null
        return name
    }

    private fun hasUsableModelContent(dir: File): Boolean {
        var fileCount = 0
        var totalBytes = 0L
        dir.walkTopDown().forEach { f ->
            if (f.isFile) {
                fileCount++
                totalBytes += f.length()
            }
        }
        return fileCount >= 2 && totalBytes > 10_000L
    }

    companion object {
        private fun pickSherpaOnnx(dir: File, role: String): File? {
            val files = dir.listFiles() ?: return null
            val candidates = files.filter { f ->
                f.isFile &&
                    f.name.endsWith(".onnx", ignoreCase = true) &&
                    f.name.startsWith(role, ignoreCase = true)
            }
            if (candidates.isEmpty()) return null
            return candidates.sortedWith(compareBy { it.name.contains(".int8.") }).first()
        }

        private const val MARKER_FILE = ".kawaiipet_model_ok"
        /** Fraction of [0,1] used for HTTP download (rest is decompress + finish). */
        private const val DOWNLOAD_WEIGHT = 0.88f
        private const val EXTRACT_WEIGHT = 0.12f

        private val DISCARD = object : OutputStream() {
            override fun write(b: Int) {}
            override fun write(b: ByteArray, off: Int, len: Int) {}
        }
    }
}

/** Tracks bytes read from the archive file so decompression progress can be shown. */
private class CountingInputStream(
    private val wrapped: InputStream,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit
) : InputStream() {
    private var read = 0L

    override fun read(): Int = wrapped.read().also { b ->
        if (b != -1) {
            read++
            emit()
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = wrapped.read(b, off, len).also { n ->
        if (n > 0) {
            read += n
            emit()
        }
    }

    private fun emit() {
        onProgress((read.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
    }

    override fun close() {
        wrapped.close()
    }
}
