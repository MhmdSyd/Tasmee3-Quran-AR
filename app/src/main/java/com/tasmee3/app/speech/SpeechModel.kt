package com.tasmee3.app.speech

/**
 * The only thing the matcher and screens know about speech recognition.
 * Swap the implementation (another Whisper, a streaming model, ...) without touching them.
 *
 * Audio is 16 kHz mono floats in [-1, 1]. [accept] adds audio to an internal buffer,
 * [result] recognizes everything accepted since the last [reset].
 */
interface SpeechModel {
    val sampleRate: Int get() = 16_000
    fun accept(audio: FloatArray)
    fun result(): String
    fun reset()
    fun close()
}
