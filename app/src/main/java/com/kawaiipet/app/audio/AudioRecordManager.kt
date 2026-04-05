package com.kawaiipet.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioRecordManager {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    val sampleRate = SAMPLE_RATE
    val bufferSize: Int
        get() = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLE_RATE) // At least 1 second of buffer

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording) return true

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf == AudioRecord.ERROR_BAD_VALUE || minBuf == AudioRecord.ERROR) return false

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
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
            audioRecord?.stop()
        } catch (_: IllegalStateException) { }
        audioRecord?.release()
        audioRecord = null
    }

    fun release() {
        stop()
    }

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHUNK_SIZE = 3200 // 200ms at 16kHz
    }
}
