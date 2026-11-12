package com.tasmee3.app.mushaf

/**
 * One token of a mushaf line. [text] is "" for a verse-end mark that wrapped onto the start of
 * a line by itself (not a spoken word). [verseEnd] is the verse number if a verse ends here, else 0.
 */
data class MushafWord(val location: String, val text: String, val verseEnd: Int) {
    val isSpoken: Boolean get() = text.isNotEmpty()
}

sealed interface MushafLine {
    val number: Int

    data class SurahHeader(override val number: Int, val text: String, val surah: Int) : MushafLine
    data class Basmala(override val number: Int) : MushafLine
    data class Words(override val number: Int, val words: List<MushafWord>) : MushafLine
}

data class MushafPage(val number: Int, val surah: Int, val juz: Int, val lines: List<MushafLine>) {
    /** (surah, ayah) of the first spoken word on the page, for the "6:36" reference in the page header. */
    fun firstVerse(): Pair<Int, Int>? {
        val loc = lines.filterIsInstance<MushafLine.Words>().flatMap { it.words }.firstOrNull { it.isSpoken }?.location
            ?: return null
        val parts = loc.split(':')
        val s = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val a = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return s to a
    }
}

data class MushafMeta(val basmala: String, val surahNames: Map<Int, String>)
