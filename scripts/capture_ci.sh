#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:connectedDebugAndroidTest --no-daemon
python3 scripts/check_screenshots.py
