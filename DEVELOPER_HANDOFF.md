# Quran Recitation App (Android, offline) — Developer Handoff

Audience: an AI agent or developer building the app from the attached design.
Design source: the `design/` folder (six `.dc.html` screens plus `canvas.json`). They are layout references only; reimplement them natively. Design date: 2026-11-09. Last updated: 2026-11-09 (model decision, stack and build plan updated).

## 1. Product goal

An Android app that shows the Quran in the **Madani mushaf** layout (Hafs from Asim) and has a **recitation mode**:

1. The mushaf text is hidden.
2. The user recites aloud.
3. A speech model running **fully on the phone** (no cloud, works offline) recognizes the speech as fully diacritized Arabic.
4. Each word is revealed in place only when the user says it correctly.
5. Wrong words (including wrong vowels) are flagged, and the word is shown after the second failed try.

Reference experience: the Tarteel app. Target: streaming, low latency, highest accuracy available offline.

## 2. Screens (see `design/`)

All phone screens are 390x844, RTL, Arabic UI.

| File | Screen | Purpose |
|---|---|---|
| Main.dc.html | Reader | Mushaf page, top bar (menu, surah/page/juz, search), prev/next page, big "ابدأ التسميع" button |
| Test_Listening.dc.html | Recitation: listening | Page hidden as word-shaped pills, thin progress bar, offline badge, waveform, hint / mic / finish buttons |
| Test_Progress.dc.html | Recitation: progress + mistake | Revealed words (latest highlighted green), wrong word outlined red, panel showing what the model heard, "N of 29 words" |
| Summary.dc.html | Session summary | First-try accuracy ring, counts (total / correct / review), list of words to review with reasons, "repeat" and "next page" |
| Settings.dc.html | Recitation settings | Reveal mode (word by word / whole verse), strictness (letters only / with diacritics), auto-hint after 5 s, show word after 2nd try, error sound, model status, riwaya (locked to Hafs) |
| Architecture.dc.html | Pipeline overview | See section 5 |

Navigation: Reader -> Listening -> Progress -> Summary -> (Reader | Listening). Settings opens from the gear in the recitation screens.

All text in the mockups is sample data (Surat Al-Fatiha) with illustrative line breaks.

## 3. Design tokens

- Background: `#FBF6EA`. Card / page: `#FFFDF6`. Ink: `#1F2A24`. Muted text: `#5B6258`.
- Primary green: `#1F6B4F` (dark text on green tint: `#17513C`; tint `#E3EFE7`, border `#B7D6C2`; fresh-word highlight `#D7EADD`).
- Gold decoration: `#B8923A`, light gold border `#D9C48A`, header fill `#F3E9CC`, verse-marker text `#6B5518`.
- Error: `#A33A2B` (text `#8A2D20`, fill `#FBEAE6`, pill fill `#F6D9D3`). Errors are shown with outline, icon and text, never color alone.
- Hidden-word pill: `#DDD4B8`, height 18, radius 9, width approx. 9 px per base letter + 10.
- Fonts: Cairo for UI. For the Quran text the mockup used Amiri Quran as a stand-in. **Production must use the King Fahd Complex Uthmani Hafs font (or its per-page QCF version)** so Madani glyph shapes and stacked diacritics render correctly.
- Touch targets at least 48 px. Mockup text sizes: Quran words 21 px, UI 12 to 18 px.

## 4. Behavior requirements

- **Page layout is data.** Each mushaf page has 15 lines (page 1 and 2 are special: Al-Fatiha and the start of Al-Baqarah have fewer/centered lines). Store, per page, which words sit on which line, plus line type (text, surah header, basmala, centered). Do not let the screen wrap text by itself.
- **Line fitting.** Dense pages have 8 to 12 words per line. Fit each line to the screen width (adjust font size or spacing per line). Never wrap or overflow. The page-1 mockup spreads words with space-between; that will not be enough on dense pages.
- **Recitation matching.** The app knows the expected text. Follow along word by word: compare each spoken word to the next expected word (allow one or two words of lookahead for skips, and handle repeats and restarts). Do not rely on free transcription plus fuzzy matching alone.
- **Strictness.** "With diacritics" counts a wrong vowel as an error (for example a kasra instead of a damma on the last letter of "نَعْبُدُ"). "Letters only" ignores vowels. **With the chosen model (quran-asr-small) only "letters only" works**, because it does not output diacritics. Keep the "with diacritics" option disabled until a model that supports it is added. The mockup screens (3, 4, 5) show the vowel-error case as a target design.
- **Second failed try** reveals the word and marks it for review.
- **Hint** reveals the next word without counting it as correct on first try.
- **Summary** accuracy = words correct on first try / total words. List the words marked for review with the reason (wrong vowel, long pause, wrong word).
- **Offline only.** No network permission needed for recitation. No audio leaves the device. Only the microphone permission is required.
- **Streaming.** Audio is processed in chunks (16 kHz mono); reveal should feel real time.

## 5. On-device pipeline

Microphone (16 kHz mono, voice-activity gate) -> streaming speech model (int8 ONNX, on device) -> word matcher against expected text -> reveal engine -> mushaf screen. Session results are stored locally.

Bundled in the app: model + tokens file, the Uthmani word list with Madani page/line layout, and the comparison rules.

## 6. Model decision (updated 2026-11-09)

> **Override 2026-11-10:** the owner chose `tarteel-ai/whisper-base-ar-quran` instead of the model below. See `docs/MODEL.md`. The text below is kept for the comparison and the fallback.

**Decision: start with `Abdou/quran-asr-small`, keep `tarteel-ai/whisper-tiny-ar-quran` as a fallback, and put the model behind an interface so it can be swapped.** All numbers below come from public Hugging Face model cards (the authors' own results) and should be re-checked on recordings that match real users (phone microphone, different reciters, noise). The task leaderboard is https://huggingface.co/spaces/deepdml/open_universal_arabic_quranic_asr_leaderboard (it loads dynamically; I could not read the live table).

### Chosen: Abdou/quran-asr-small
- 33M parameters, 36 MB int8 ONNX (`model.int8.onnx` + `tokens.txt`). Runs on Android with sherpa-onnx as a zipformer CTC model.
- Quran-Lab benchmark (600 clips): 4.49% WER, 2.17% CER, #2 of 33 at submission. Weakest on phone recordings (10.45% WER).
- License: MIT. Training data keeps its own terms (the tlog dataset is CC-BY-4.0, which requires attribution).
- Limits: offline model (decodes whole clips, no streaming). Output is Arabic letters in Uthmani spelling **without diacritics** (31 output tokens), so it cannot detect a wrong vowel. Hafs and Warsh only. Greedy decoding, no language model.
- Consequences for the app:
  - Run it on a rolling window or after short pauses (voice-activity gate), for example re-decode the growing buffer about every 0.5 s. Words appear with a small delay.
  - Default strictness is **letters only**. The "with diacritics" setting in the Settings screen stays disabled (or hidden) while this model is used.
  - Mispronunciation feedback is limited to wrong words and long pauses.

### Fallback: tarteel-ai/whisper-tiny-ar-quran
- Whisper tiny fine-tune (about 39M parameters; the card does not state size). Apache-2.0. 7.05% WER on the card's own evaluation set, which is not comparable to the benchmark above. The card says nothing about diacritics, training data or mobile use, and it lists two quantized variants.
- Whisper reads 30-second windows, so it is not a streaming model, and it can invent words on unclear audio. **Before using it, test that its output keeps diacritics.**

### Upgrade path (only if vowel and tajweed feedback is required): Quran-Lab/zipformer_p-arabic-v3
- Streaming, 65.5M parameters, outputs phonemes (251 symbols that encode tajweed features), not text. Leaderboard score 3.46% WER (with a retrieval step that matches phonemes to the nearest verse). Phoneme error rate 1.43% studio, 3.65% real phone recordings, 9.10% unseen professional reciter.
- Its predecessor `Muno459/zipformer_p-quran` has a 73 MB int8 streaming ONNX file that works with sherpa-onnx. An Android ONNX file for v3 is **not confirmed** (the v3 card lists Apple packages).
- **License (NPL-1.2, no-profit):** the app must be free. No charging for the model or any feature it powers, no ads, no subscription. The app must state that automatic feedback can be wrong and does not replace a teacher. Access is gated behind an agreement on Hugging Face.
- Needs a text-to-phoneme step so the expected verses can be compared to the phoneme output, which is extra work.
- Take this path only if the app will be free.

### Rejected (do not use)
| Model | Reason |
|---|---|
| nvidia/stt_ar_fastconformer_hybrid_large_pcd_v1.0 | About 115M parameters, offline, NeMo only, no mobile support listed. Outputs diacritized text but is a general Arabic model (6.55% WER on EveryAyah, 12 to 28% on general sets). CC-BY-4.0. |
| Muno459/fastconformer-quran | Diacritized Quran text, 5.41% WER, but 219 MB (fp16) to 437 MB and non-commercial only. |
| IJyad/whisper-large-v3-Tarteel | 2B parameters in 32-bit format (several GB). Validation WER stuck near 48%. |
| facebook/hf-seamless-m4t-medium, facebook/seamless-m4t-v2-large | 1.2B and 2.3B parameters, general translation models, CC-BY-NC (non-commercial). |
| Abdou/quran-asr-tiny | 19 MB, but about 7% WER and no diacritics. Only for very low-end devices. |

## 7. Stack and build (updated)

- **Phone app: native Kotlin + Jetpack Compose** (full RTL support, direct audio capture). sherpa-onnx provides Android bindings and loads the chosen model (`model.int8.onnx` + `tokens.txt`). Flutter and Python-on-phone frameworks (Kivy, BeeWare) are not recommended: Arabic shaping with stacked diacritics, audio capture and the ONNX runtime are more reliable natively.
- **Python and FastAPI are not part of the phone app.** Recognition must work with no server. Use Python only for development tools (`tools/`: build the page-layout data, normalize text, benchmark models). A FastAPI backend is **optional, later** (sync, accounts) and the app must work fully without it.
- Local storage for progress and review words (Room/SQLite).
- Min SDK to be decided based on the ONNX runtime requirements.
- **Build without Android Studio.** The owner does not have it. Build from the command line with the Gradle wrapper (needs JDK 17 and the Android SDK command-line tools). If those are not available in the agent's environment, use **GitHub Actions**:
  - Workflow 1: on every push, run `./gradlew test assembleDebug` and upload the debug APK as an artifact.
  - Workflow 2 (manual or on version tags): signed release APK/AAB using a keystore kept in GitHub repository secrets.
  - Fetch the model and font with a checksum-verified script instead of committing large binaries.
  - Install on the phone: download the APK artifact, copy it to the phone, allow installs from unknown sources, open the file.

## 8. Open decisions

1. Will the app be free, with no ads or subscription? If yes, the zipformer phoneme model becomes possible. If no, stay with quran-asr-small.
2. Whether wrong-vowel feedback is a must-have for version 1. If yes, a different model is needed (see section 6).
3. Source and license of the Madani page/line layout data and Quran text (Quran.com's Quran Universal Library and Tanzil are the usual sources). Check both.
4. Licenses of the chosen model, its training data attribution (CC-BY-4.0 data) and the font, before shipping.
5. UI language: Arabic only, or Arabic plus English.
6. Whether wrong words stay hidden or are shown after the second try (current design shows them).

## 9. Build order proposal

0. Project setup and a green GitHub Actions build that produces a debug APK.
1. Mushaf reader with page-layout data and the correct font; verify dense pages (for example page 2 and late pages like 600+) before anything else.
2. Audio capture + the chosen model (quran-asr-small through sherpa-onnx) running in a test screen that prints recognized text. Wrap it in a `SpeechModel` interface (audio in, text or phonemes out) so the model can be replaced without changing the matcher or screens. Test on real phone recordings.
3. Word matcher against expected text, tested on recordings.
4. Recitation screens (listening, reveal, mistake) wired to the matcher.
5. Summary, settings, local storage, polish.
