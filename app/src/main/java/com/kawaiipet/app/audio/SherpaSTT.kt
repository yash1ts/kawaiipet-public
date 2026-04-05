package com.kawaiipet.app.audio

import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.HomophoneReplacerConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OnlineLMConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

/**
 * Sherpa-ONNX STT: streaming **Zipformer transducer** (online) or **Moonshine v2** (offline utterance).
 */
class SherpaSTT(private val modelManager: ModelManager) {

    /** Moonshine uses OfflineRecognizer JNI — must not overlap decode/acceptWaveform across threads. */
    private val moonshineJniLock = Any()

    private var onlineRecognizer: OnlineRecognizer? = null
    private var onlineStream: OnlineStream? = null

    private var offlineRecognizer: OfflineRecognizer? = null
    private var offlineStream: OfflineStream? = null

    private var useMoonshine: Boolean = false

    private var initializedForModelId: String? = null

    val isInitialized: Boolean
        get() = onlineRecognizer != null || offlineRecognizer != null

    fun initialize(modelId: String): Boolean {
        if (modelId.isBlank()) {
            release()
            return false
        }
        if (initializedForModelId == modelId && isInitialized) return true

        release()
        if (!modelManager.isModelDownloaded(modelId)) {
            Log.w(TAG, "Model not on disk: $modelId")
            return false
        }

        val transducer = modelManager.resolveSherpaStreamingTransducer(modelId)
        if (transducer != null) {
            useMoonshine = false
            return initOnlineTransducer(modelId, transducer)
        }

        val moonshine = modelManager.resolveSherpaMoonshine(modelId)
        if (moonshine != null) {
            useMoonshine = true
            return initMoonshineOffline(modelId, moonshine)
        }

        Log.e(TAG, "No supported STT layout for $modelId")
        return false
    }

    private fun initOnlineTransducer(modelId: String, paths: SherpaStreamingTransducerPaths): Boolean {
        val transducer = OnlineTransducerModelConfig(
            encoder = paths.encoderPath,
            decoder = paths.decoderPath,
            joiner = paths.joinerPath
        )
        val modelConfig = OnlineModelConfig(
            transducer = transducer,
            tokens = paths.tokensPath,
            numThreads = sttNumThreads(),
            provider = "cpu",
            debug = false
        )
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = modelConfig,
            lmConfig = OnlineLMConfig(),
            endpointConfig = EndpointConfig(),
            enableEndpoint = true
        )
        return try {
            onlineRecognizer = OnlineRecognizer(assetManager = null, config = config)
            initializedForModelId = modelId
            Log.i(TAG, "STT initialized (transducer): $modelId")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "STT transducer init failed", t)
            onlineRecognizer = null
            initializedForModelId = null
            false
        }
    }

    private fun initMoonshineOffline(modelId: String, paths: SherpaMoonshinePaths): Boolean {
        val moon = OfflineMoonshineModelConfig(
            preprocessor = "",
            encoder = paths.encoderPath,
            uncachedDecoder = "",
            cachedDecoder = "",
            mergedDecoder = paths.mergedDecoderPath
        )
        val modelCfg = OfflineModelConfig().apply {
            moonshine = moon
            tokens = paths.tokensPath
            numThreads = sttNumThreads()
            debug = false
            provider = "cpu"
        }
        val config = OfflineRecognizerConfig().apply {
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80)
            modelConfig = modelCfg
            hr = HomophoneReplacerConfig("", "", "")
            decodingMethod = "greedy_search"
            maxActivePaths = 4
            hotwordsFile = ""
            hotwordsScore = 1.5f
            ruleFsts = ""
            ruleFars = ""
            blankPenalty = 0f
        }
        return try {
            offlineRecognizer = OfflineRecognizer(assetManager = null, config = config)
            initializedForModelId = modelId
            Log.i(TAG, "STT initialized (Moonshine offline): $modelId")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "STT Moonshine init failed", t)
            offlineRecognizer = null
            initializedForModelId = null
            false
        }
    }

    fun startStream() {
        endStream()
        if (useMoonshine) {
            synchronized(moonshineJniLock) {
                val rec = offlineRecognizer ?: return
                offlineStream = try {
                    rec.createStream()
                } catch (t: Throwable) {
                    Log.e(TAG, "Moonshine createStream failed", t)
                    null
                }
            }
            return
        }
        val rec = onlineRecognizer ?: return
        onlineStream = try {
            rec.createStream()
        } catch (t: Throwable) {
            Log.e(TAG, "createStream failed", t)
            null
        }
    }

    fun acceptWaveform(samples: FloatArray) {
        if (useMoonshine) {
            synchronized(moonshineJniLock) {
                val s = offlineStream ?: return
                try {
                    s.acceptWaveform(samples, 16000)
                } catch (t: Throwable) {
                    Log.e(TAG, "Moonshine acceptWaveform failed", t)
                }
            }
            return
        }
        val s = onlineStream ?: return
        val rec = onlineRecognizer ?: return
        try {
            s.acceptWaveform(samples, sampleRate = 16000)
            while (rec.isReady(s)) {
                rec.decode(s)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "acceptWaveform failed", t)
        }
    }

    fun isEndpoint(): Boolean {
        if (useMoonshine) return false
        val s = onlineStream ?: return false
        val rec = onlineRecognizer ?: return false
        return try {
            rec.isEndpoint(s)
        } catch (_: Throwable) {
            false
        }
    }

    fun getPartialResult(): String {
        if (useMoonshine) return ""
        val s = onlineStream ?: return ""
        val rec = onlineRecognizer ?: return ""
        return try {
            rec.getResult(s).text
        } catch (_: Throwable) {
            ""
        }
    }

    fun getFinalResult(): String {
        if (useMoonshine) {
            synchronized(moonshineJniLock) {
                val s = offlineStream ?: return ""
                val rec = offlineRecognizer ?: return ""
                return try {
                    rec.decode(s)
                    rec.getResult(s).text.trim()
                } catch (t: Throwable) {
                    Log.e(TAG, "Moonshine final decode failed", t)
                    ""
                }
            }
        }
        return getPartialResult().trim()
    }

    fun endStream() {
        try {
            onlineStream?.release()
        } catch (_: Exception) {
        }
        onlineStream = null
        synchronized(moonshineJniLock) {
            try {
                offlineStream?.release()
            } catch (_: Exception) {
            }
            offlineStream = null
        }
    }

    fun release() {
        endStream()
        try {
            onlineRecognizer?.release()
        } catch (_: Exception) {
        }
        onlineRecognizer = null
        synchronized(moonshineJniLock) {
            try {
                offlineRecognizer?.release()
            } catch (_: Exception) {
            }
            offlineRecognizer = null
        }
        useMoonshine = false
        initializedForModelId = null
    }

    companion object {
        private const val TAG = "SherpaSTT"

        /** ONNX intra-op threads; 2 was conservative and under-used multi-core SoCs. */
        private fun sttNumThreads(): Int =
            minOf(4, maxOf(2, Runtime.getRuntime().availableProcessors()))
    }
}
