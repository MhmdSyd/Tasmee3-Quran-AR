package com.tasmee3.app.settings

import com.tasmee3.app.match.MatcherConfig
import com.tasmee3.app.match.RevealMode

/**
 * Recitation settings. Strictness is not a setting yet: with the current model only "letters only" works
 * (the model does not output vowel marks), so the "with diacritics" choice is shown disabled.
 */
data class RecitationSettings(
    val revealMode: RevealMode = RevealMode.WORD,
    val autoHint: Boolean = true,            // reveal the next word after 5 s of silence
    val revealAfterSecondTry: Boolean = true,
    val errorSound: Boolean = false,
) {
    fun matcherConfig() = MatcherConfig(revealAfterSecondFailure = revealAfterSecondTry)
}
