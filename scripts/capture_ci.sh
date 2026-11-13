#!/usr/bin/env bash
set -uo pipefail
./gradlew :app:connectedDebugAndroidTest --no-daemon
result=$?
mkdir -p .ci-screenshots
adb pull /sdcard/Android/data/org.hayya.salah.dev/files/screenshots .ci-screenshots/ || true
exit "$result"
