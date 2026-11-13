#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hayya.salah.AppearanceCaptureTest,org.hayya.salah.WidgetAppearanceTest --no-daemon
python3 scripts/check_screenshots.py
