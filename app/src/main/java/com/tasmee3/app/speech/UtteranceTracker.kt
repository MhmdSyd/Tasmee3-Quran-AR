package com.tasmee3.app.speech

/**
 * Keeps the audio of the phrase being spoken right now (plus a short pre-roll) so it can be recognized again and
 * again while the reciter is still talking. This is what makes words appear one by one instead of after a pause.
 * Pure Kotlin. Times are on the audio clock in milliseconds.
 */
class UtteranceTracker(private val sampleRate: Int = 16_000, private val preRollMs: Int = 400) {
    private val preRoll = ArrayDeque<FloatArray>()
    private var preRollSamples = 0
    private val buffer = ArrayList<FloatArray>()
    private var samples = 0

    var active = false
        private set

    /** Audio-clock time when speech was first detected (pre-roll not included). */
    var startMs = 0L
        private set

    val lengthMs: Long get() = samples * 1000L / sampleRate

    /** [nowMs] is the audio clock at the END of [chunk]. [speaking] is the voice detector's current state. */
    fun push(chunk: FloatArray, speaking: Boolean, nowMs: Long) {
        if (active) {
            buffer.add(chunk)
            samples += chunk.size
            return
        }
        if (speaking) {
            active = true
            startMs = nowMs - chunk.size * 1000L / sampleRate
            buffer.clear()
            samples = 0
            for (c in preRoll) { buffer.add(c); samples += c.size }
            preRoll.clear(); preRollSamples = 0
            buffer.add(chunk); samples += chunk.size
        } else {
            preRoll.addLast(chunk)
            preRollSamples += chunk.size
            val limit = preRollMs * sampleRate / 1000
            while (preRoll.size > 1 && preRollSamples - preRoll.first().size >= limit) {
                preRollSamples -= preRoll.removeFirst().size
            }
        }
    }

    /** The voice detector delivered the final segment of this phrase, or the phrase was abandoned. */
    fun reset() {
        active = false
        buffer.clear()
        samples = 0
        preRoll.clear()
        preRollSamples = 0
    }

    /** Everything spoken so far in this phrase, as one array. */
    fun audio(): FloatArray {
        val out = FloatArray(samples)
        var offset = 0
        for (c in buffer) { c.copyInto(out, offset); offset += c.size }
        return out
    }
}
