package com.tasmee3.app.match

data class SessionResult(val page: Int, val surahName: String, val summary: Summary)

/** In-memory hand-over from the recitation screen to the summary screen. Persistent storage comes in step 6. */
object LastSession {
    @Volatile var result: SessionResult? = null
}
