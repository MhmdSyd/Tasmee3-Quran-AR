package com.tasmee3.app.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineFitterTest {
    @Test
    fun fittedLineNeverExceedsAvailableWidth() {
        val avail = 300f
        val size = LineFitter.fitSize(avail, wordsWidthAtRef = 400f, refSize = 100f, items = 5, marks = 1)
        val natural = size * LineFitter.emWidth(400f, 100f, 5, 1)
        assertTrue("natural=$natural", natural <= avail)
        assertTrue(natural >= avail * 0.97f)
    }

    @Test
    fun denserLineGetsSmallerFont() {
        val loose = LineFitter.fitSize(300f, 300f, 100f, 4, 0)
        val dense = LineFitter.fitSize(300f, 900f, 100f, 12, 2)
        assertTrue(dense < loose)
    }

    @Test
    fun shortLinesAreCentered() {
        assertTrue(LineFitter.isCentered(naturalWidth = 150f, availWidth = 300f))
        assertFalse(LineFitter.isCentered(naturalWidth = 280f, availWidth = 300f))
    }

    @Test
    fun emptyLineDoesNotCrash() {
        assertEquals(Float.MAX_VALUE, LineFitter.fitSize(300f, 0f, 100f, 0, 0), 0f)
    }
}

class LineFitterUniformityTest {
    @Test
    fun medianOfOddAndEvenLists() {
        assertEquals(3f, LineFitter.median(listOf(5f, 1f, 3f)), 0f)
        assertEquals(2.5f, LineFitter.median(listOf(1f, 2f, 3f, 4f)), 0f)
        assertEquals(0f, LineFitter.median(emptyList()), 0f)
    }

    @Test
    fun uniformCapKeepsLooseLinesCloseToTheTypicalSize() {
        assertEquals(22.4f, LineFitter.uniformCap(30f, 20f), 1e-4f)   // 20 * 1.12
        assertEquals(15f, LineFitter.uniformCap(15f, 20f), 0f)        // the global maximum still wins
        assertEquals(30f, LineFitter.uniformCap(30f, 0f), 0f)
    }

    @Test
    fun justifiedGapSplitsTheLeftoverSpace() {
        assertEquals(10f, LineFitter.justifiedGap(300f, 200f, 11), 1e-4f)
        assertEquals(0f, LineFitter.justifiedGap(300f, 400f, 5), 0f)
        assertEquals(0f, LineFitter.justifiedGap(300f, 100f, 1), 0f)
    }
}
