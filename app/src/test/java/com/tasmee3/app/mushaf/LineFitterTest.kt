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
