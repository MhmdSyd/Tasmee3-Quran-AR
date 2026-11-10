package com.tasmee3.app.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

/** Microphone as a flow of 16 kHz mono float chunks (about 100 ms each). Needs RECORD_AUDIO granted. */
class MicRecorder(private val sampleRate: Int = 16_000) {

    @SuppressLint("MissingPermission")
    fun chunks(): Flow<FloatArray> = flow {
        val minBytes = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBytes <= 0) throw IOException("AudioRecord is not available (code $minBytes)")
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBytes, sampleRate * 2)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IOException("Microphone could not be opened")
        }
        val buffer = ShortArray(sampleRate / 10)
        try {
            record.startRecording()
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = record.read(buffer, 0, buffer.size)
                if (n < 0) throw IOException("Microphone read failed (code $n)")
                if (n > 0) emit(FloatArray(n) { buffer[it] / 32768f })
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)
}
