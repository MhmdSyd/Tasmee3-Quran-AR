#!/usr/bin/env bash
# Runs inside the emulator (see .github/workflows/screenshots.yml). Takes one screenshot per page.
set -uo pipefail
PKG=com.tasmee3.app
mkdir -p screenshots
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
for p in 1 2 3 50 503 600 604; do
  adb shell am force-stop $PKG
  adb shell am start -n $PKG/.MainActivity --ei page "$p"
  sleep 5
  adb exec-out screencap -p > "screenshots/page-$p.png"
done
adb logcat -d -b crash > screenshots/crash.log 2>&1
exit 0
