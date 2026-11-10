package com.tasmee3.app.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafParserTest {
    // Dummy tokens only: this test must not contain real Quran text.
    private val sample = """
      {"p":7,"s":2,"j":1,"l":[
        {"n":1,"t":"h","x":"عنوان","s":2},
        {"n":2,"t":"b"},
        {"n":3,"t":"w","v":[["2:1:1","كلمةأ",0],["2:1:2","كلمةب",5]]},
        {"n":4,"t":"w","v":[["2:5:3","",6],["2:6:1","كلمةج",0]]}
      ]}
    """.trimIndent()

    @Test
    fun parsesLinesAndWords() {
        val page = MushafParser.parsePage(sample)
        assertEquals(7, page.number)
        assertEquals(2, page.surah)
        assertEquals(1, page.juz)
        assertEquals(4, page.lines.size)
        assertTrue(page.lines[0] is MushafLine.SurahHeader)
        assertTrue(page.lines[1] is MushafLine.Basmala)
        val words = (page.lines[2] as MushafLine.Words).words
        assertEquals("2:1:2", words[1].location)
        assertEquals(5, words[1].verseEnd)
    }

    @Test
    fun markOnlyTokenIsNotSpoken() {
        val page = MushafParser.parsePage(sample)
        val first = (page.lines[3] as MushafLine.Words).words[0]
        assertFalse(first.isSpoken)
        assertEquals(6, first.verseEnd)
    }

    @Test
    fun parsesMeta() {
        val meta = MushafParser.parseMeta("""{"basmala":"أ ب ج د","surahs":{"1":"س١","2":"س٢"}}""")
        assertEquals(4, meta.basmala.split(' ').size)
        assertEquals("س٢", meta.surahNames[2])
    }
}
