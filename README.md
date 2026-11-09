# Tasmee3 — تسميع

Offline Android app for Quran recitation practice (Madani mushaf, Hafs from Asim). Recognition runs fully on the device; no network permission, no analytics.

Spec: see `DEVELOPER_HANDOFF.md`. Status: **step 1 of 7 (project setup)**. Screens are placeholders.

## Build
- **CI (recommended):** push to GitHub, open Actions, download the `tasmee3-debug-apk` artifact. Details: `docs/BUILD_WITHOUT_ANDROID_STUDIO.md`.
- **Local:** needs JDK 17, the Android SDK (platform 35, build-tools 35.0.0) and Gradle 8.9. Run `gradle wrapper --gradle-version 8.9` once, then `./gradlew test assembleDebug`.

## Test
`gradle test` (unit tests). Anything that needs the microphone or a phone is listed as a manual test in the step reports.

## Swap the model
Not implemented yet (step 3 adds the `SpeechModel` interface).
