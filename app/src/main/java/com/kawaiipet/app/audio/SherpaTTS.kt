package com.kawaiipet.app.audio

import android.util.Log
import kotlin.math.absoluteValue
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

/**
 * Offline TTS via Sherpa-ONNX [OfflineTts] (Kitten, or Piper VITS: model.onnx + tokens.txt + espeak-ng-data).
 *
 * [DEFAULT_SYNTH_SPEED] matches Sherpa generate speed; [AudioTrackManager] applies matching playback speed + pitch.
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
                lengthScale = KITTEN_LENGTH_SCALE
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
                noiseScale = VITS_NOISE_SCALE,
                noiseScaleW = VITS_NOISE_SCALE_W,
                lengthScale = VITS_LENGTH_SCALE
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
            silenceScale = SILENCE_SCALE
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

    fun generate(
        text: String,
        speakerId: Int = DEFAULT_SPEAKER_ID,
        speed: Float = DEFAULT_SYNTH_SPEED
    ): FloatArray? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val engine = synchronized(lock) { tts ?: return null }
        return try {
            val audio = engine.generate(trimmed, sid = speakerId, speed = speed)
            sampleRate = audio.sampleRate
            polishSamples(audio.samples.copyOf())
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
        speakerId: Int = DEFAULT_SPEAKER_ID,
        speed: Float = DEFAULT_SYNTH_SPEED,
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
                val polished = polishSamples(audio.samples.copyOf())
                if (polished.isNotEmpty()) {
                    onChunk(polished)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "TTS generateChunked failed for chunk", t)
            }
        }
    }

    fun release() = synchronized(lock) { releaseLocked() }

    /** Remove DC offset and gently tame occasional overs for cleaner speakers / headphones. */
    private fun polishSamples(samples: FloatArray): FloatArray {
        if (samples.isEmpty()) return samples
        var sum = 0.0
        for (x in samples) sum += x
        val mean = (sum / samples.size).toFloat()
        var peak = 1e-6f
        for (i in samples.indices) {
            val v = (samples[i] - mean) * PCM_HEADROOM
            samples[i] = v
            val a = v.absoluteValue
            if (a > peak) peak = a
        }
        if (peak > SOFT_CLIP_START) {
            val gain = ((1f - SOFT_CLIP_KNEE) / peak + SOFT_CLIP_KNEE).coerceIn(0.5f, 1f)
            for (i in samples.indices) {
                samples[i] = softClip(samples[i] * gain)
            }
        }
        return samples
    }

    private fun softClip(x: Float): Float {
        val a = x.absoluteValue
        if (a <= SOFT_CLIP_START) return x
        val over = a - SOFT_CLIP_START
        val shaped = SOFT_CLIP_START + over / (1f + over * OVERCOMPRESS)
        val sgn = when {
            x < 0f -> -1f
            x > 0f -> 1f
            else -> 0f
        }
        return shaped * sgn
    }

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

        /** Kitten speaker id: 1 lines up with the first “light” female voice in the nano bundle ordering. */
        const val DEFAULT_SPEAKER_ID = 1

        /** Sherpa `generate` speed (paired with [AudioTrackManager] playback speed). */
        const val DEFAULT_SYNTH_SPEED = 1.2f

        private const val KITTEN_LENGTH_SCALE = 1.0f

        private const val VITS_LENGTH_SCALE = 1.0f

        /** Lower VITS noise = smoother, less grainy (Piper / VITS paths). */
        private const val VITS_NOISE_SCALE = 0.45f
        private const val VITS_NOISE_SCALE_W = 0.65f

        /** Tighter pauses between sentences feel tidier. */
        private const val SILENCE_SCALE = 0.11f

        private const val PCM_HEADROOM = 0.96f
        private const val SOFT_CLIP_START = 0.88f
        private const val SOFT_CLIP_KNEE = 0.08f
        private const val OVERCOMPRESS = 2.2f

        private val SENTENCE_SPLIT = Regex("""(?<=[.!?;])\s+|(?<=,)\s+(?=.{30,})""")

        fun splitIntoSentences(text: String): List<String> {
            val parts = text.split(SENTENCE_SPLIT).filter { it.isNotBlank() }
            if (parts.size <= 1) return listOf(text)
            return parts
        }
    }
}
