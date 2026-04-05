package com.kawaiipet.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioRecordManager {

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var isRecording = false

    val sampleRate = SttEngineConfig.SAMPLE_RATE

    val bufferSize: Int
        get() = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate)

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording) return true

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf == AudioRecord.ERROR_BAD_VALUE || minBuf == AudioRecord.ERROR) return false

        val bufBytes = (minBuf * BUFFER_MULTIPLIER).coerceAtLeast(minBuf)

        val record = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufBytes)
                .build()
        } catch (_: SecurityException) {
            return false
        }

        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            audioRecord = null
            return false
        }

        try {
            record.startRecording()
        } catch (_: SecurityException) {
            record.release()
            audioRecord = null
            return false
        }

        if (NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                    enabled = true
                }
            } catch (_: Exception) {
                noiseSuppressor?.release()
                noiseSuppressor = null
            }
        }

        isRecording = true
        return true
    }

    suspend fun readLoop(onSamples: (ShortArray) -> Unit) = withContext(Dispatchers.IO) {
        val buffer = ShortArray(CHUNK_SIZE)
        while (isActive && isRecording) {
            val read = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: break
            if (read > 0) {
                onSamples(buffer.copyOf(read))
            }
        }
    }

    fun stop() {
        isRecording = false
        try {
            noiseSuppressor?.release()
        } catch (_: Exception) {
        }
        noiseSuppressor = null
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) { }
        audioRecord?.release()
        audioRecord = null
    }

    fun release() {
        stop()
    }

    companion object {
        private const val CHUNK_SIZE = 3200
        private const val BUFFER_MULTIPLIER = 3
    }
}
