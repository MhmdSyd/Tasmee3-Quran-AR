package com.tasmee3.app.mushaf

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Reads the bundled per-page layout files from assets (offline). */
class MushafRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val cache = LruCache<Int, MushafPage>(12)
    @Volatile private var meta: MushafMeta? = null

    suspend fun page(number: Int): MushafPage = withContext(Dispatchers.IO) {
        require(number in 1..PAGE_COUNT) { "page $number out of range" }
        cache.get(number) ?: read("mushaf/p%03d.json".format(Locale.ROOT, number))
            .let(MushafParser::parsePage)
            .also { cache.put(number, it) }
    }

    suspend fun meta(): MushafMeta = withContext(Dispatchers.IO) {
        meta ?: MushafParser.parseMeta(read("mushaf/meta.json")).also { meta = it }
    }

    private fun read(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val PAGE_COUNT = 604
    }
}
