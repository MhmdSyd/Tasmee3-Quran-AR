package com.tasmee3.app.mushaf

/**
 * Pure maths for fitting one mushaf line into the available width without wrapping.
 * All widths are in the same unit (px). A line is a row of items: words plus verse-number marks.
 * Width at font size s = s * (wordsWidthAtRef/ref + GAP_EM*(items-1) + MARK_EM*marks).
 */
object LineFitter {
    const val GAP_EM = 0.16f      // minimum space between items, in em
    const val MARK_EM = 1.15f     // width of a verse-number mark, in em
    const val SAFETY = 0.98f      // keep 2% spare so rounding never causes overflow
    const val CENTER_RATIO = 0.7f // lines narrower than this share of the width are centered, not justified

    fun emWidth(wordsWidthAtRef: Float, refSize: Float, items: Int, marks: Int): Float =
        wordsWidthAtRef / refSize + GAP_EM * (items - 1).coerceAtLeast(0) + MARK_EM * marks

    /** Largest font size at which the line fits [availWidth]. */
    fun fitSize(availWidth: Float, wordsWidthAtRef: Float, refSize: Float, items: Int, marks: Int): Float {
        val em = emWidth(wordsWidthAtRef, refSize, items, marks)
        return if (em <= 0f) Float.MAX_VALUE else availWidth * SAFETY / em
    }

    fun isCentered(naturalWidth: Float, availWidth: Float): Boolean =
        naturalWidth < availWidth * CENTER_RATIO
}
