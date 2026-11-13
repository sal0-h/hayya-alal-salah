#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
hayya_package=org.hayya.salah.dev
hayya_api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
adb shell appops set "$hayya_package" POST_NOTIFICATION deny
if (( hayya_api >= 31 )); then adb shell appops set "$hayya_package" SCHEDULE_EXACT_ALARM deny; fi
mkdir -p app/build/reports/integration
adb shell am instrument -w -r -e class org.hayya.salah.AlarmPermissionDeniedTest -e expectDenied true "$hayya_package.test/androidx.test.runner.AndroidJUnitRunner" > "app/build/reports/integration/denied-api-$hayya_api.txt"
python3 - "$hayya_api" <<'PY'
from pathlib import Path
import sys
text=Path(f"app/build/reports/integration/denied-api-{sys.argv[1]}.txt").read_text()
assert "OK (1 test)" in text and "FAILURES" not in text and "INSTRUMENTATION_FAILED" not in text, text
PY
adb shell appops set "$hayya_package" POST_NOTIFICATION allow
if (( hayya_api >= 31 )); then adb shell appops set "$hayya_package" SCHEDULE_EXACT_ALARM allow; fi
if (( hayya_api >= 33 )); then adb shell pm grant "$hayya_package" android.permission.POST_NOTIFICATIONS; fi
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=org.hayya.salah.AppearanceCaptureTest,org.hayya.salah.AlarmPermissionDeniedTest,org.hayya.salah.DirectBootFixtureTest --no-daemon

if (( hayya_api >= 37 )); then bash scripts/direct_boot_ci.sh; fi
