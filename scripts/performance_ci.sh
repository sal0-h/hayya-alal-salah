#!/usr/bin/env bash
set -euo pipefail
# The nonminified generator and optimized target share an isolated synthetic ID.
./gradlew :app:assembleProfilegen --no-daemon
python3 scripts/check_benchmark_apks.py app/build/outputs/apk/profilegen/app-profilegen.apk
./gradlew :benchmark:connectedProfilegenAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hayya.salah.benchmarktests.BaselineProfileGenerator -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR --no-daemon
python3 scripts/merge_profiles.py benchmark/build/outputs/connected_android_test_additional_output app/src/main
./gradlew :benchmark:connectedPerformanceAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hayya.salah.benchmarktests.PrayerBenchmarks,org.hayya.salah.benchmarktests.MemoryCaptureTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR --no-daemon
python3 - <<'PY'
from pathlib import Path
import zipfile
apk=Path('app/build/outputs/apk/performance/app-performance.apk')
assert apk.is_file() and apk.stat().st_size <= 35*1024*1024
with zipfile.ZipFile(apk) as package:
    assert 'assets/dexopt/baseline.prof' in package.namelist(), 'Generated profile was not packaged'
print('Optimized target contains the generated Baseline Profile; emulator metrics are not physical acceptance budgets.')
PY
