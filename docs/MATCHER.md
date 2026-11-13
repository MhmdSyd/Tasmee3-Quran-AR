# Word matcher (step 4)

Pure Kotlin, package `com.tasmee3.app.match`. No Android classes, so it is unit-tested on the JVM.

## Input and output
- `ExpectedWords.fromPage(page)` gives the spoken words of a page in order (verse-end number marks are not words).
- `RecitationMatcher.onSegment(text, startMs, endMs)`: one recognized segment (one pause-to-pause phrase). Times are the
  AUDIO clock, so decode delay does not matter. Returns events: `Revealed`, `WrongTry`, `Ignored`, `Completed`.
- `hint()` reveals the next word. `summary()` gives accuracy, review list and counts.

## Normalisation (applied to expected AND heard text)
1. Remove vowel marks U+064B-U+065F, dagger alef U+0670, Quranic annotation/waqf signs U+06D6-U+06ED, tatweel U+0640, U+200E/U+200F.
2. Alef with hamza or madda (U+0623, U+0625, U+0622) and alef wasla (U+0671) become alef U+0627.
3. Alef maksura U+0649 becomes ya U+064A; hamza on ya U+0626 becomes ya and hamza on waw U+0624 becomes waw (the official text writes them as a plain letter plus a hamza mark).
4. Anything that is not an Arabic letter (digits, punctuation, Latin) becomes a space; spaces collapse.
Kept as is: ta marbuta, the stand-alone hamza U+0621, ha.

## When two words are "the same"
- Equal after normalisation, or equal after also removing every alef (needs at least 3 letters left).
  Reason: Uthmani spelling omits alefs that ordinary spelling writes, and we do not know which spelling the model uses.
- If `fuzzy` (default on): allow edit distance 1 for keys of 4-7 letters and 2 for 8+ letters. Words of 3 letters or fewer must match exactly.
  Trade-off: a genuinely wrong word that differs by one letter from the right one is accepted. Set `fuzzy = false` to be strict.

## Order of decisions for each heard word
1. Inside a replay of earlier text: consume it silently (`Ignored`).
2. Matches the next expected word: reveal it.
3. Two consecutive heard words glued together match it (the model split a word): reveal it.
4. Same as the previous word: ignore it (stutter or model repetition).
5. Exactly matches one of the next 2 expected words: reveal it; the words in between are revealed and marked SKIPPED
   (or WRONG_WORD if they had a failed try).
6. Equals a word from the last 3 revealed words, or a verse-start word earlier on the page: replay/restart, ignored until the reciter catches up.
7. Otherwise a wrong word. Only ONE failed try is counted per segment, so a garbage segment can never reveal several words.
   First failed try: `WrongTry` (red outline). Second failed try: the word is revealed and marked WRONG_WORD.

## Streaming (live reveal while speaking)
The model decodes a whole phrase, so the app re-recognizes the phrase being spoken every ~350 ms (`SpeechPipeline`,
`UtteranceTracker`) and calls `RecitationSession.onPartial`. A partial result only REVEALS words
(`RecitationMatcher.previewCount`: consecutive in-order matches from the current position). It never flags mistakes, skips or
restarts: those are judged on the final result after the pause (`onSegment`), which replaces the preview. Within one phrase
the revealed count never shrinks, a late partial of an already-final phrase is ignored, and a hint takes over the first
previewed word. First-try accuracy is unchanged: the final pass decides.

## Review reasons and first-try accuracy
A word is "first try" only if it has no reason. Reasons: WRONG_WORD (failed try or skipped after a failed try), SKIPPED, LONG_PAUSE
(silence over 5 s before the word), HINTED. SKIPPED and HINTED are additions to the handoff's reasons (wrong word, long pause).
Accuracy = first-try words / all words of the session. Words never recited count as not correct and are not in the review list.
Waiting before the very first word is not a pause.

## Verified
- 49 JVM unit tests (matcher, normalizer, comparer, helpers) pass; deliberately breaking the matcher makes tests fail.
- Simulation on the real data of all 604 pages (77,429 spoken words), with the same code:
  perfect recitation 604/604 pages all first-try; one skipped word detected 603/604; one wrong word flagged 604/604;
  a one-letter recognition slip forgiven 602/604; repeated word and phrase ignored without penalty 590/604
  (the rest are real ambiguities: the text itself continues with the words just repeated).
- NOT verified: behaviour with the real model output on a phone (its spelling and mistakes). Needs the step 3 test results.
