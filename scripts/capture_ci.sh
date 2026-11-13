#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=org.hayya.salah.AlarmPermissionDeniedTest,org.hayya.salah.DirectBootFixtureTest --no-daemon
python3 scripts/check_screenshots.py
