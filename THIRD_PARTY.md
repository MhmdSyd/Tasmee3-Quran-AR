# Third-party assets

None of the assets below are stored in git. `scripts/fetch_assets.sh` downloads them (pinned commit + SHA-256) at build time and the
license texts are copied into the app (`assets/licenses/`).

**Status key:** OK = license clear. TEST-ONLY = do not publish the APK publicly until the open point is resolved.

| Asset | Source (pinned in the script) | License | Status |
|---|---|---|---|
| UthmanicHafs1 Ver18 font (KFGQPC Uthmanic Script Hafs), unmodified, from the owner's QUL download: `data/qul/UthmanicHafs1Ver18.ttf` | QUL > Fonts (QPC Hafs) | KFGQPC EULA embedded in the font: free of cost to use, copy and distribute; the font cannot be sold, modified, altered, translated, reverse engineered or "reproduced" (the same text also says "Copy"). Full text: `third_party_licenses/KFGQPC_EULA.txt` | TEST-ONLY until KFGQPC confirms in writing. Never modify or subset the font. |
| Mushaf layout "Quran Complex V1 (1405 print)" (`data/qul/qpc-v1-15-lines.db`) and Uthmani Hafs script (`data/qul/qpc-hafs.db`), from the owner's QUL downloads, converted by `tools/build_layout.py` | qul.tarteel.ai (Mushaf layouts, Scripts) | QUL: usable in commercial projects, each resource has its own licence page (needs a QUL login) | TEST-ONLY until the two resource licence pages are checked |
| sherpa-onnx 2.0.1 Android AAR (includes ONNX Runtime, MIT), fetched pinned + SHA-256 | github.com/k2-fsa/sherpa-onnx releases | Apache-2.0 (ONNX Runtime: MIT) | OK |
| Silero VAD model (`silero_vad.onnx`), pinned + SHA-256 | sherpa-onnx `asr-models` release (from github.com/snakers4/silero-vad) | MIT (`third_party_licenses/silero-vad_LICENSE.txt`) | OK |
| Abdou/quran-asr-small (`model.int8.onnx`, `tokens.txt`), fetched from Hugging Face pinned + verified | huggingface.co/Abdou/quran-asr-small | MIT. Training data credited by the author includes Tarteel tlog (CC-BY-4.0, attribution: Tarteel AI), IqraEval, EveryAyah, Tanzil. Notice: `third_party_licenses/quran-asr-small_NOTICE.txt` | OK to ship with attribution |
| Cairo font (`cairo.ttf`, UI) | github.com/google/fonts, ofl/cairo | SIL OFL 1.1 | OK |
| AndroidX / Jetpack Compose / Material 3 / Navigation / Coroutines | Maven | Apache-2.0 | OK |
| Room 2.6.1 + KSP (build tool) | Maven | Apache-2.0 | OK |
| Kotlin stdlib | Maven | Apache-2.0 | OK |
| JUnit 4 / org.json (tests only) | Maven | EPL-1.0 / JSON License | OK (not shipped) |



## How to swap (decision log)
- **Font:** change `FONT_*` in `scripts/fetch_assets.sh` to another TTF named `uthmanic_hafs.ttf` (for example Amiri Quran, SIL OFL). No code change.
- **Layout data:** a different mushaf or script only needs new files in `data/qul/` and an updated `tools/build_layout.py` producing the same `pNNN.json` files.
