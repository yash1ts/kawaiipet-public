package com.kawaiipet.app.audio

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

/**
 * Offline TTS via Sherpa-ONNX [OfflineTts] (Kitten, or Piper VITS: model.onnx + tokens.txt + espeak-ng-data).
 */
class SherpaTTS(private val modelManager: ModelManager) {

    private val lock = Any()

    @Volatile
    private var tts: OfflineTts? = null

    @Volatile
    private var initializedForModelId: String? = null

    @Volatile
    var sampleRate: Int = 22050
        private set

    val isInitialized: Boolean
        get() = synchronized(lock) { tts != null }

    fun initialize(modelId: String): Boolean = synchronized(lock) {
        if (modelId.isBlank()) {
            releaseLocked()
            return false
        }
        if (initializedForModelId == modelId && tts != null) return true

        releaseLocked()
        if (!modelManager.isModelDownloaded(modelId)) {
            Log.w(TAG, "TTS model not on disk: $modelId")
            return false
        }

        val kittenPaths = modelManager.resolveSherpaKittenTts(modelId)
        val modelCfg = if (kittenPaths != null) {
            val kittenCfg = OfflineTtsKittenModelConfig(
                model = kittenPaths.modelPath,
                voices = kittenPaths.voicesPath,
                tokens = kittenPaths.tokensPath,
                dataDir = kittenPaths.dataDirPath,
                lengthScale = 1.0f
            )
            OfflineTtsModelConfig().apply {
                kitten = kittenCfg
                numThreads = 2
                debug = false
                provider = "cpu"
            }
        } else {
            val paths = modelManager.resolveSherpaVitsTts(modelId) ?: run {
                Log.e(TAG, "Not Kitten or VITS/Piper bundle: $modelId")
                return false
            }
            val vits = OfflineTtsVitsModelConfig(
                model = paths.modelPath,
                lexicon = paths.lexiconPath,
                tokens = paths.tokensPath,
                dataDir = paths.dataDirPath,
                dictDir = paths.dictDirPath,
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )
            OfflineTtsModelConfig().apply {
                this.vits = vits
                numThreads = 2
                debug = false
                provider = "cpu"
            }
        }
        val cfg = OfflineTtsConfig(
            model = modelCfg,
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f
        )

        return try {
            val engine = OfflineTts(assetManager = null, config = cfg)
            tts = engine
            initializedForModelId = modelId
            sampleRate = engine.sampleRate()
            Log.i(TAG, "TTS initialized: $modelId sampleRate=$sampleRate")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "TTS init failed", t)
            tts = null
            initializedForModelId = null
            false
        }
    }

    fun generate(text: String, speakerId: Int = 0, speed: Float = 1.0f): FloatArray? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val engine = synchronized(lock) { tts ?: return null }
        return try {
            val audio = engine.generate(trimmed, sid = speakerId, speed = speed)
            sampleRate = audio.sampleRate
            audio.samples
        } catch (t: Throwable) {
            Log.e(TAG, "TTS generate failed", t)
            null
        }
    }

    /**
     * Splits [text] into sentences and generates audio for each one, calling
     * [onChunk] as soon as each sentence is ready. Because [onChunk] is a
     * suspend lambda the caller can feed a [Channel] with back-pressure.
     */
    suspend fun generateChunked(
        text: String,
        speakerId: Int = 0,
        speed: Float = 1.0f,
        onChunk: suspend (FloatArray) -> Unit
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val engine = synchronized(lock) { tts ?: return }

        for (sentence in splitIntoSentences(trimmed)) {
            if (sentence.isBlank()) continue
            try {
                val audio = engine.generate(sentence.trim(), sid = speakerId, speed = speed)
                sampleRate = audio.sampleRate
                if (audio.samples.isNotEmpty()) {
                    onChunk(audio.samples)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "TTS generateChunked failed for chunk", t)
            }
        }
    }

    fun release() = synchronized(lock) { releaseLocked() }

    private fun releaseLocked() {
        try {
            tts?.release()
        } catch (_: Exception) {
        }
        tts = null
        initializedForModelId = null
    }

    companion object {
        private const val TAG = "SherpaTTS"

        private val SENTENCE_SPLIT = Regex("""(?<=[.!?;])\s+|(?<=,)\s+(?=.{30,})""")

        fun splitIntoSentences(text: String): List<String> {
            val parts = text.split(SENTENCE_SPLIT).filter { it.isNotBlank() }
            if (parts.size <= 1) return listOf(text)
            return parts
        }
    }
}
