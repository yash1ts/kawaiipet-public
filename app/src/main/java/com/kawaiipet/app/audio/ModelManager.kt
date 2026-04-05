package com.kawaiipet.app.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class ModelManager(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    fun getModelDir(modelId: String): File = File(modelsDir, modelId)

    /**
     * Copies [BundledVoiceModels.ASSET_STT_DIR] and [BundledVoiceModels.ASSET_TTS_DIR] into
     * [getModelDir] when missing or when the on-disk revision differs from [BundledVoiceModels.EXPECTED_ASSET_REVISION].
     */
    fun ensureBundledModelsFromAssets(): Boolean {
        val expectedRev = readExpectedRevisionFromAssets() ?: run {
            Log.e(TAG, "Missing ${BundledVoiceModels.ASSET_REVISION_ASSET_PATH} in assets")
            return false
        }
        if (expectedRev != BundledVoiceModels.EXPECTED_ASSET_REVISION) {
            Log.e(TAG, "Asset REVISION mismatch: asset=$expectedRev expected=${BundledVoiceModels.EXPECTED_ASSET_REVISION}")
            return false
        }

        val revMarker = File(modelsDir, BUNDLED_REVISION_MARKER)
        val sttDir = getModelDir(BundledVoiceModels.STT_MODEL_ID)
        val ttsDir = getModelDir(BundledVoiceModels.TTS_MODEL_ID)
        val upToDate = revMarker.isFile &&
            revMarker.readText().trim() == BundledVoiceModels.EXPECTED_ASSET_REVISION &&
            hasUsableMoonshineLayout(sttDir) &&
            hasUsableKittenLayout(ttsDir)

        if (upToDate) return true

        return try {
            sttDir.deleteRecursively()
            ttsDir.deleteRecursively()
            copyAssetDirectory(BundledVoiceModels.ASSET_STT_DIR, sttDir)
            copyAssetDirectory(BundledVoiceModels.ASSET_TTS_DIR, ttsDir)
            File(sttDir, MARKER_FILE).writeText("1")
            File(ttsDir, MARKER_FILE).writeText("1")
            revMarker.writeText(BundledVoiceModels.EXPECTED_ASSET_REVISION)
            true
        } catch (e: Exception) {
            Log.e(TAG, "ensureBundledModelsFromAssets failed", e)
            sttDir.deleteRecursively()
            ttsDir.deleteRecursively()
            false
        }
    }

    private fun readExpectedRevisionFromAssets(): String? = try {
        context.assets.open(BundledVoiceModels.ASSET_REVISION_ASSET_PATH).bufferedReader().use { it.readText().trim() }
    } catch (_: Exception) {
        null
    }

    private fun copyAssetDirectory(assetPath: String, destDir: File) {
        val am = context.assets
        val children = am.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            destDir.parentFile?.mkdirs()
            am.open(assetPath).use { input ->
                FileOutputStream(destDir).use { input.copyTo(it) }
            }
            return
        }
        destDir.mkdirs()
        for (child in children) {
            val sub = "$assetPath/$child"
            val nextDest = File(destDir, child)
            val subChildren = am.list(sub) ?: emptyArray()
            if (subChildren.isEmpty()) {
                nextDest.parentFile?.mkdirs()
                am.open(sub).use { inp -> FileOutputStream(nextDest).use { inp.copyTo(it) } }
            } else {
                copyAssetDirectory(sub, nextDest)
            }
        }
    }

    private fun hasUsableMoonshineLayout(dir: File): Boolean =
        resolveSherpaMoonshineFromRoot(dir) != null

    private fun hasUsableKittenLayout(dir: File): Boolean =
        resolveSherpaKittenTtsFromRoot(dir) != null

    fun resolveSherpaStreamingTransducer(modelId: String): SherpaStreamingTransducerPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = modelRoots(base)
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

    fun resolveSherpaVitsTts(modelId: String): SherpaVitsTtsPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        val roots = modelRoots(base)
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

    fun resolveSherpaMoonshine(modelId: String): SherpaMoonshinePaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        for (root in modelRoots(base)) {
            val p = resolveSherpaMoonshineFromRoot(root) ?: continue
            return p
        }
        return null
    }

    private fun resolveSherpaMoonshineFromRoot(root: File): SherpaMoonshinePaths? {
        val tokens = File(root, "tokens.txt")
        if (!tokens.isFile) return null
        val files = root.listFiles() ?: return null
        val encoder = files.firstOrNull { f ->
            f.isFile && f.name.startsWith("encoder", ignoreCase = true) &&
                (f.name.endsWith(".ort", ignoreCase = true) || f.name.endsWith(".onnx", ignoreCase = true))
        } ?: return null
        val merged = files.firstOrNull { f ->
            f.isFile &&
                (f.name.endsWith(".ort", ignoreCase = true) || f.name.endsWith(".onnx", ignoreCase = true)) &&
                f.name.contains("merged", ignoreCase = true)
        } ?: files.firstOrNull { f ->
            f.isFile &&
                (f.name.endsWith(".ort", ignoreCase = true) || f.name.endsWith(".onnx", ignoreCase = true)) &&
                f.name.startsWith("decoder", ignoreCase = true)
        } ?: return null
        return SherpaMoonshinePaths(
            encoderPath = encoder.absolutePath,
            mergedDecoderPath = merged.absolutePath,
            tokensPath = tokens.absolutePath
        )
    }

    fun resolveSherpaKittenTts(modelId: String): SherpaKittenTtsPaths? {
        val base = getModelDir(modelId)
        if (!base.isDirectory) return null
        for (root in modelRoots(base)) {
            val p = resolveSherpaKittenTtsFromRoot(root) ?: continue
            return p
        }
        return null
    }

    private fun resolveSherpaKittenTtsFromRoot(root: File): SherpaKittenTtsPaths? {
        val voices = File(root, "voices.bin")
        if (!voices.isFile) return null
        val tokens = File(root, "tokens.txt")
        val espeak = File(root, "espeak-ng-data")
        if (!tokens.isFile || !espeak.isDirectory) return null
        val onnxFiles = root.listFiles()
            ?.filter { f -> f.isFile && f.name.endsWith(".onnx", ignoreCase = true) }
            .orEmpty()
        if (onnxFiles.isEmpty()) return null
        val modelFile = onnxFiles
            .filter { !it.name.contains(".int8.", ignoreCase = true) }
            .maxByOrNull { it.length() }
            ?: onnxFiles.maxByOrNull { it.length() }
            ?: return null
        return SherpaKittenTtsPaths(
            modelPath = modelFile.absolutePath,
            voicesPath = voices.absolutePath,
            tokensPath = tokens.absolutePath,
            dataDirPath = espeak.absolutePath
        )
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val dir = getModelDir(modelId)
        if (!dir.exists()) return false
        if (File(dir, MARKER_FILE).exists()) return true
        return hasUsableModelContent(dir)
    }

    private fun modelRoots(base: File): List<File> = buildList {
        add(base)
        base.listFiles()?.sortedBy { it.name }?.forEach { if (it.isDirectory) add(it) }
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
        private const val TAG = "ModelManager"
        private const val MARKER_FILE = ".kawaiipet_model_ok"
        private const val BUNDLED_REVISION_MARKER = ".bundled_voice_revision"

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
    }
}

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

/** Moonshine v2: encoder + merged decoder (.ort or .onnx) + tokens.txt */
data class SherpaMoonshinePaths(
    val encoderPath: String,
    val mergedDecoderPath: String,
    val tokensPath: String
)

/** Kitten TTS: model.onnx + voices.bin (raw floats) + tokens.txt + espeak-ng-data/. */
data class SherpaKittenTtsPaths(
    val modelPath: String,
    val voicesPath: String,
    val tokensPath: String,
    val dataDirPath: String
)
