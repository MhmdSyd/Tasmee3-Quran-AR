package com.tasmee3.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameBufferTest {
    @Test
    fun cutsExactWindowsAndKeepsRemainder() {
        val fb = FrameBuffer(512)
        val first = fb.push(FloatArray(1000) { it.toFloat() })
        assertEquals(1, first.size)
        assertEquals(512, first[0].size)
        val second = fb.push(FloatArray(100))      // 488 + 100 = 588 -> one more window
        assertEquals(1, second.size)
        assertEquals(512, second[0].size)
        assertEquals(512f, second[0][0], 0f) // first sample left over from the first push
    }

    @Test
    fun preservesSampleOrderAcrossChunks() {
        val fb = FrameBuffer(4)
        val out = ArrayList<Float>()
        for (chunk in listOf(floatArrayOf(0f, 1f, 2f), floatArrayOf(3f, 4f, 5f, 6f, 7f), floatArrayOf(8f))) {
            fb.push(chunk).forEach { f -> out.addAll(f.toList()) }
        }
        assertEquals(listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f), out)
    }

    @Test
    fun smallInputGivesNoFrames() {
        assertTrue(FrameBuffer(512).push(FloatArray(100)).isEmpty())
    }

    @Test
    fun clearDropsRemainder() {
        val fb = FrameBuffer(4)
        fb.push(floatArrayOf(1f, 2f, 3f))
        fb.clear()
        assertTrue(fb.push(floatArrayOf(9f)).isEmpty())
    }
}
