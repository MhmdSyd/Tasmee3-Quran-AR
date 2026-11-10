# Third-party assets

None of the assets below are stored in git. `scripts/fetch_assets.sh` downloads them (pinned commit + SHA-256) at build time and the
license texts are copied into the app (`assets/licenses/`).

**Status key:** OK = license clear. TEST-ONLY = do not publish the APK publicly until the open point is resolved.

| Asset | Source (pinned in the script) | License | Status |
|---|---|---|---|
| KFGQPC Uthmanic Script HAFS v0.18 font (`uthmanic_hafs.ttf`, unmodified) | github.com/thetruetruth/quran-data-kfgqpc | KFGQPC EULA embedded in the font: free of cost to use, copy and distribute; the font cannot be sold, modified, altered, translated, reverse engineered or "reproduced" (same text also forbids "Reproduced", which conflicts with "Copy"). Full text: `third_party_licenses/KFGQPC_EULA.txt` | TEST-ONLY: wording is ambiguous; ask KFGQPC for written confirmation before a public release. Never modify or subset the font. |
| Mushaf page/line layout and per-word text (converted by `tools/build_layout.py`) | github.com/zonetecde/mushaf-layout (QPC1 = 1405 print by default, QPC2 = 1421 print) | Repo code: ISC. **No license for the data.** README says it was generated from the Quran.com API | TEST-ONLY: data license not confirmed |
| Cairo font (`cairo.ttf`, UI) | github.com/google/fonts, ofl/cairo | SIL OFL 1.1 | OK |
| AndroidX / Jetpack Compose / Material 3 / Navigation / Coroutines | Maven | Apache-2.0 | OK |
| Kotlin stdlib | Maven | Apache-2.0 | OK |
| JUnit 4 / org.json (tests only) | Maven | EPL-1.0 / JSON License | OK (not shipped) |

Planned, not bundled yet: `Abdou/quran-asr-small` model (MIT; training data CC-BY-4.0, attribution required).

## How to swap (decision log)
- **Font:** change `FONT_*` in `scripts/fetch_assets.sh` to another TTF named `uthmanic_hafs.ttf` (for example Amiri Quran, SIL OFL). No code change.
- **Layout data:** `LAYOUT=QPC2 bash scripts/fetch_assets.sh` switches edition. A different dataset only needs a new converter producing the same `pNNN.json` files.
