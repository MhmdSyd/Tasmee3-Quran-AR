package com.tasmee3.app.mushaf

import org.json.JSONObject

/** Parses the compact per-page files produced by tools/build_layout.py. */
object MushafParser {
    fun parsePage(json: String): MushafPage {
        val root = JSONObject(json)
        val arr = root.getJSONArray("l")
        val lines = ArrayList<MushafLine>(arr.length())
        for (i in 0 until arr.length()) {
            val l = arr.getJSONObject(i)
            val n = l.getInt("n")
            when (val type = l.getString("t")) {
                "h" -> lines.add(MushafLine.SurahHeader(n, l.getString("x"), l.getInt("s")))
                "b" -> lines.add(MushafLine.Basmala(n))
                "w" -> {
                    val v = l.getJSONArray("v")
                    val words = ArrayList<MushafWord>(v.length())
                    for (j in 0 until v.length()) {
                        val w = v.getJSONArray(j)
                        words.add(MushafWord(w.getString(0), w.getString(1), w.getInt(2)))
                    }
                    lines.add(MushafLine.Words(n, words))
                }
                else -> error("Unknown line type '$type'")
            }
        }
        return MushafPage(root.getInt("p"), root.getInt("s"), root.getInt("j"), lines)
    }

    fun parseMeta(json: String): MushafMeta {
        val root = JSONObject(json)
        val surahs = root.getJSONObject("surahs")
        val names = HashMap<Int, String>()
        for (key in surahs.keys()) names[key.toInt()] = surahs.getString(key)
        return MushafMeta(root.getString("basmala"), names)
    }
}
