package com.kawaiipet.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.math.sqrt

enum class PipelineState { IDLE, LISTENING, PROCESSING, SPEAKING }

class AudioPipeline(
    private val appContext: Context,
    private val stt: SherpaSTT,
    private val tts: SherpaTTS,
    private val recorder: AudioRecordManager,
    private val player: AudioTrackManager
) {
    private val _state = MutableStateFlow(PipelineState.IDLE)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /** One-shot load of STT+TTS for the current pet session; see [schedulePetVoiceModelPrepare]. */
    private var petVoicePrepareJob: Job? = null

    var onAmplitude: ((Float) -> Unit)?
        get() = player.onAmplitude
        set(value) { player.onAmplitude = value }

    val isSttReady: Boolean get() = stt.isInitialized
    val isTtsReady: Boolean get() = tts.isInitialized

    fun initializeSTT(modelId: String): Boolean = stt.initialize(modelId)
    fun initializeTTS(modelId: String): Boolean = tts.initialize(modelId)

    /**
     * Starts loading the selected STT and TTS models once per overlay session.
     * Safe to call multiple times: in-flight work is not duplicated.
     */
    fun schedulePetVoiceModelPrepare(
        scope: CoroutineScope,
        sttId: String,
        ttsId: String,
        loadStt: Boolean,
        loadTts: Boolean
    ) {
        if (petVoicePrepareJob?.isActive == true) return
        petVoicePrepareJob = scope.launch(Dispatchers.Default) {
            if (loadStt && sttId.isNotBlank()) {
                val ok = initializeSTT(sttId)
                Log.d(TAG, "Pet voice prepare: STT id=$sttId success=$ok")
            }
            if (loadTts && ttsId.isNotBlank()) {
                val ok = initializeTTS(ttsId)
                Log.d(TAG, "Pet voice prepare: TTS id=$ttsId success=$ok")
            }
        }
    }

    /**
     * Waits for [schedulePetVoiceModelPrepare] to finish (up to [timeoutMs]), so engines are ready to reuse.
     */
    suspend fun awaitPetVoiceEnginesReady(timeoutMs: Long = 90_000L) {
        withTimeoutOrNull(timeoutMs) {
            petVoicePrepareJob?.join()
        }
    }

    /**
     * Listens for speech, transcribes it, and returns the text.
     * Uses VAD (voice activity detection) to auto-detect when the user stops speaking.
     *
     * Priority: Sherpa STT → Platform SpeechRecognizer → Raw audio VAD (no transcription).
     */
    suspend fun listenAndTranscribe(
        timeoutMs: Long = 30_000L,
        onPartialText: (String) -> Unit = {}
    ): String {
        _state.value = PipelineState.LISTENING
        Log.d(TAG, "listenAndTranscribe: starting, sttReady=${stt.isInitialized}")

        return try {
            when {
                stt.isInitialized -> listenWithSherpa(timeoutMs)
                isPlatformSttAvailable() -> listenWithPlatformSpeechRecognizer(onPartialText, timeoutMs)
                else -> {
                    Log.d(TAG, "No STT available, using raw VAD")
                    listenWithVadOnly(timeoutMs, onPartialText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "listenAndTranscribe failed", e)
            ""
        } finally {
            _state.value = PipelineState.IDLE
        }
    }

    private fun isPlatformSttAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(appContext)
    } catch (_: Exception) {
        false
    }

    private suspend fun listenWithSherpa(timeoutMs: Long): String {
        stt.startStream()
        if (!recorder.start()) return ""

        val result = CompletableDeferred<String>()
        var silenceCount = 0
        var hasSpeechHint = false

        recordJob = scope.launch {
            recorder.readLoop { samples ->
                val floatSamples = samples.map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()
                stt.acceptWaveform(floatSamples)

                if (hasSpeechHint && stt.isEndpoint()) {
                    // Stop capture before final decode: Moonshine OfflineRecognizer is not
                    // thread-safe if decode runs while acceptWaveform is still feeding audio.
                    recorder.stop()
                    result.complete(stt.getFinalResult())
                    return@readLoop
                }

                val rms = sqrt(floatSamples.map { (it * it).toDouble() }.average()).toFloat()
                if (rms > SPEECH_THRESHOLD) {
                    hasSpeechHint = true
                    silenceCount = 0
                } else if (hasSpeechHint) {
                    silenceCount++
                }

                if (hasSpeechHint && silenceCount > SILENCE_CHUNKS_AFTER_SPEECH) {
                    recorder.stop()
                    result.complete(stt.getFinalResult())
                    return@readLoop
                }
            }
        }

        val awaited = withTimeoutOrNull(timeoutMs) { result.await() }
        recorder.stop()
        recordJob?.cancelAndJoin()
        val text = awaited ?: stt.getFinalResult()

        stt.endStream()
        return text
    }

    private suspend fun listenWithPlatformSpeechRecognizer(
        onPartialText: (String) -> Unit,
        timeoutMs: Long
    ): String = withContext(Dispatchers.Main.immediate) {
        Log.d(TAG, "Using platform SpeechRecognizer")

        val text = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer: ready")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: speech started")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: speech ended (VAD)")
                    }

                    override fun onError(error: Int) {
                        Log.e(TAG, "SpeechRecognizer error: $error")
                        sr.destroy()
                        if (cont.isActive) cont.resume("")
                    }

                    override fun onResults(results: Bundle?) {
                        val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                        Log.d(TAG, "SpeechRecognizer result: $t")
                        sr.destroy()
                        if (cont.isActive) cont.resume(t)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }

                sr.setRecognitionListener(listener)
                sr.startListening(intent)

                cont.invokeOnCancellation {
                    try { sr.stopListening() } catch (_: Exception) {}
                    sr.destroy()
                }
            }
        } ?: ""

        text
    }

    /**
     * Fallback: record raw audio and use energy-based VAD to detect when the user
     * starts and stops speaking. No transcription — returns "[voice]" when speech
     * was detected so the caller knows it happened.
     */
    private suspend fun listenWithVadOnly(
        timeoutMs: Long,
        onPartialText: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (!recorder.start()) {
            Log.e(TAG, "VAD: failed to start recorder")
            return@withContext ""
        }

        Log.d(TAG, "VAD: recording started")
        onPartialText("Listening…")

        val result = CompletableDeferred<Boolean>()
        var silenceFrames = 0
        var speechFrames = 0
        var hasSpeechStarted = false

        recordJob = scope.launch {
            recorder.readLoop { samples ->
                val floatSamples = samples.map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()
                val rms = sqrt(floatSamples.map { (it * it).toDouble() }.average()).toFloat()

                if (rms > SPEECH_THRESHOLD) {
                    speechFrames++
                    silenceFrames = 0

                    if (speechFrames >= MIN_SPEECH_FRAMES && !hasSpeechStarted) {
                        hasSpeechStarted = true
                        Log.d(TAG, "VAD: speech detected")
                        onPartialText("Speaking…")
                    }
                } else {
                    if (hasSpeechStarted) {
                        silenceFrames++
                        if (silenceFrames > SILENCE_CHUNKS_AFTER_SPEECH) {
                            Log.d(TAG, "VAD: silence after speech, stopping")
                            result.complete(true)
                            return@readLoop
                        }
                    }
                }
            }
        }

        val speechDetected = withTimeoutOrNull(timeoutMs) { result.await() } ?: false
        recorder.stop()
        recordJob?.cancel()

        if (speechDetected) "[voice]" else ""
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "speak skipped: blank text")
            return
        }
        if (!tts.isInitialized) {
            awaitPetVoiceEnginesReady(timeoutMs = 90_000L)
        }
        if (!tts.isInitialized) {
            Log.w(TAG, "speak skipped: TTS not ready after pet voice prepare")
            return
        }

        _state.value = PipelineState.SPEAKING

        val channel = Channel<FloatArray>(capacity = 2)

        val producerJob = scope.launch {
            for (sentence in SherpaTTS.splitIntoSentences(text)) {
                val samples = tts.generate(sentence.trim())
                if (samples != null && samples.isNotEmpty()) {
                    channel.send(samples)
                }
            }
            channel.close()
        }

        player.playStreaming(channel, tts.sampleRate)
        producerJob.join()

        _state.value = PipelineState.IDLE
    }

    fun stopListening() {
        recorder.stop()
        recordJob?.cancel()
        stt.endStream()
        _state.value = PipelineState.IDLE
    }

    fun stopSpeaking() {
        player.stop()
        _state.value = PipelineState.IDLE
    }

    fun release() {
        petVoicePrepareJob?.cancel()
        petVoicePrepareJob = null
        recorder.release()
        player.release()
        stt.release()
        tts.release()
        _state.value = PipelineState.IDLE
    }

    companion object {
        private const val TAG = "AudioPipeline"
        private const val SPEECH_THRESHOLD = 0.025f
        private const val MIN_SPEECH_FRAMES = 3
        private const val SILENCE_CHUNKS_AFTER_SPEECH = 8
    }
}
