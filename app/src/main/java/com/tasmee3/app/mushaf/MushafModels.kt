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

data class MushafPage(val number: Int, val surah: Int, val juz: Int, val lines: List<MushafLine>)

data class MushafMeta(val basmala: String, val surahNames: Map<Int, String>)
