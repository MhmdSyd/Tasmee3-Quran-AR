package com.tasmee3.app.data

import com.tasmee3.app.match.ReviewItem

/** One review word as stored (pure, so it is unit-tested without a database). */
data class ReviewRow(val wordId: String, val text: String, val reason: String, val heard: String?)

object SessionRows {
    fun reviewRows(items: List<ReviewItem>): List<ReviewRow> =
        items.map { ReviewRow(it.word.id, it.word.text, it.primaryReason.name, it.heard) }
}
