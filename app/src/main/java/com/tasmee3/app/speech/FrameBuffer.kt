package com.tasmee3.app.speech

/** Re-cuts arbitrary audio chunks into exact windows of [window] samples (the VAD needs fixed sizes). */
class FrameBuffer(private val window: Int) {
    private var pending = FloatArray(0)

    fun push(samples: FloatArray): List<FloatArray> {
        val all = if (pending.isEmpty()) samples else pending + samples
        val frames = ArrayList<FloatArray>()
        var i = 0
        while (i + window <= all.size) {
            frames.add(all.copyOfRange(i, i + window))
            i += window
        }
        pending = all.copyOfRange(i, all.size)
        return frames
    }

    fun clear() {
        pending = FloatArray(0)
    }
}
