# Tasmee3 — تسميع

Offline Android app for Quran recitation practice (Madani mushaf, Hafs from Asim). Recognition runs fully on the device; no network permission, no analytics.

Spec: see `DEVELOPER_HANDOFF.md`. Status: **step 6 of 7 (summary, settings, storage, icon)** with the `quran-asr-small` model (see `docs/MODEL.md`). The green button on the reader starts a live, fully offline recitation (`docs/TEST_STEP5.md`, `docs/TEST_STEP6.md`). Matcher rules: `docs/MATCHER.md`. Last step: signed release build (step 7).

## Assets (fonts and Mushaf data)
They are not in git. `bash scripts/fetch_assets.sh` downloads and verifies them (CI does this automatically). See `THIRD_PARTY.md` for licenses and open points.
`LAYOUT=QPC2 bash scripts/fetch_assets.sh` switches the Mushaf edition (default QPC1, 1405 print).

## Build
- **CI (recommended):** push to GitHub, open Actions, download the `tasmee3-debug-apk` artifact. Details: `docs/BUILD_WITHOUT_ANDROID_STUDIO.md`.
- **Local:** needs JDK 17, the Android SDK (platform 35, build-tools 35.0.0) and Gradle 8.9. Run `gradle wrapper --gradle-version 8.9` once, then `./gradlew test assembleDebug`.

## Page screenshots
Actions -> **Page screenshots (emulator)** -> Run workflow. Download the `page-screenshots` artifact (pages 1, 2, 3, 50, 503, 600, 604).
On a phone you can also jump to any page with the search icon.

## Test
`gradle test` (unit tests). Anything that needs the microphone or a phone is listed as a manual test in the step reports.

## Speech model
The model (`Abdou/quran-asr-small`, 36 MB) is downloaded from Hugging Face by `scripts/fetch_assets.sh` during every build and bundled inside the APK, so the app itself works fully offline. See `docs/MODEL.md`.
