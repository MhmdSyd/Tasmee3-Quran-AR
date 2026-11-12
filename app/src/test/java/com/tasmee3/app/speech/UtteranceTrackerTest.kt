package com.tasmee3.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtteranceTrackerTest {
    private fun chunk(v: Float, n: Int = 1600) = FloatArray(n) { v } // 100 ms at 16 kHz

    @Test
    fun staysIdleUntilSpeechIsDetected() {
        val t = UtteranceTracker()
        t.push(chunk(0f), false, 100)
        assertFalse(t.active)
        assertEquals(0, t.audio().size)
    }

    @Test
    fun startsWithPreRollAndRemembersWhenSpeechBegan() {
        val t = UtteranceTracker()
        for (i in 1..6) t.push(chunk(0f), false, i * 100L)   // silence, only the last 400 ms are kept
        t.push(chunk(1f), true, 700)
        assertTrue(t.active)
        assertEquals(600L, t.startMs)                          // speech began in the chunk that ended at 700
        assertEquals(5 * 1600, t.audio().size)                // 4 pre-roll chunks + the speech chunk
        assertEquals(1f, t.audio().last(), 0f)
    }

    @Test
    fun keepsGrowingWhileActiveEvenIfTheDetectorBlinks() {
        val t = UtteranceTracker()
        t.push(chunk(1f), true, 100)
        t.push(chunk(0.5f), false, 200)  // short gap between words
        t.push(chunk(1f), true, 300)
        assertEquals(3 * 1600, t.audio().size)
        assertEquals(300L, t.lengthMs)
    }

    @Test
    fun resetEndsThePhraseAndForgetsTheAudio() {
        val t = UtteranceTracker()
        t.push(chunk(1f), true, 100)
        t.reset()
        assertFalse(t.active)
        assertEquals(0, t.audio().size)
        t.push(chunk(1f), true, 900)
        assertEquals(800L, t.startMs)
    }
}
