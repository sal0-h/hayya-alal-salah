#!/usr/bin/env bash
set -euo pipefail
hayya_hardware=$(adb shell getprop ro.hardware | tr -d '\r')
[[ "$hayya_hardware" == ranchu || "$hayya_hardware" == goldfish ]]
hayya_package=org.hayya.salah.dev
mkdir -p app/build/reports/integration
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell appops set "$hayya_package" POST_NOTIFICATION allow
adb shell appops set "$hayya_package" SCHEDULE_EXACT_ALARM allow
adb shell pm grant "$hayya_package" android.permission.POST_NOTIFICATIONS
cleanup_lock() {
  adb shell locksettings clear --old 2468 > /dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_WAKEUP > /dev/null 2>&1 || true
  adb shell wm dismiss-keyguard > /dev/null 2>&1 || true
}
trap cleanup_lock EXIT
adb shell am instrument -w -r -e class org.hayya.salah.DirectBootFixtureTest#prepare "$hayya_package.test/androidx.test.runner.AndroidJUnitRunner" > app/build/reports/integration/direct-boot-prepare.txt
python3 - <<'PY'
from pathlib import Path
text=Path('app/build/reports/integration/direct-boot-prepare.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES' not in text and 'INSTRUMENTATION_FAILED' not in text, text
PY
adb shell locksettings set-pin 2468
adb reboot
adb wait-for-device
python3 - <<'PY'
import json,subprocess,time
from pathlib import Path
report=Path('app/build/reports/integration')
deadline=time.monotonic()+300
last=''
while time.monotonic()<deadline:
    result=subprocess.run(['adb','shell','am','broadcast','--include-stopped-packages','--receiver-foreground','-n','org.hayya.salah.dev/org.hayya.salah.DebugAlarmProbe','-a','org.hayya.salah.DEBUG_ALARM_PROBE'],capture_output=True,text=True,timeout=20)
    last=result.stdout+result.stderr
    start,end=last.find('{'),last.rfind('}')
    if start>=0:
        state=json.loads(last[start:end+1])
        assert not state['unlocked'], 'Emulator unlocked before the Direct Boot check'
        if state['fajrActive'] and state['bundled'] and state['alarmAudioActive']:
            (report/'direct-boot-locked.json').write_text(json.dumps(state,indent=2)+'\n')
            with (report/'direct-boot-locked.png').open('wb') as output:
                subprocess.run(['adb','exec-out','screencap','-p'],stdout=output,check=True)
            break
    time.sleep(3)
else:
    (report/'direct-boot-failure.txt').write_text(last)
    raise SystemExit('No verified bundled Fajr audio while credential storage remained locked')
PY
adb shell am force-stop "$hayya_package"
cleanup_lock
adb shell input keyevent KEYCODE_MENU
sleep 2
adb shell am instrument -w -r -e class org.hayya.salah.DirectBootFixtureTest#cleanup "$hayya_package.test/androidx.test.runner.AndroidJUnitRunner" > app/build/reports/integration/direct-boot-cleanup.txt
python3 - <<'PY'
from pathlib import Path
text=Path('app/build/reports/integration/direct-boot-cleanup.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES' not in text and 'INSTRUMENTATION_FAILED' not in text, text
PY
