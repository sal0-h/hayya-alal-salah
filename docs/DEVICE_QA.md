# Physical release acceptance

Use the stable signed QA
application and synthetic records/public locations. Record commit, QA version code,
device model, exact Android/One UI build, locale/font scale and relevant Android
permission/channel/battery settings. Keep personal coordinates/history/audio out of
shared screenshots and logs. Record observed timing and failures; do not mark a row
passed because the feature compiles or an emulator passed it.

## Current QA build

[Signed QA build 150](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252759/artifacts/15146924960)
(source `1d6978c`, package `org.hayya.salah.qa`) is installed on Salman's S23 Ultra.
Seven navigation-only checks passed and Salman accepted the feel. See PROGRESS.md
for the exact device/build and evidence. Alarm/audio/Focus physical acceptance is
still open. Use higher-version signed QA updates as the maintenance work proceeds;
check records, settings, imports and both widgets after each update. Do not uninstall
or downgrade an installation with records to repeat a historical sequence below.

The compatible maintenance update is [signed QA157](https://github.com/sal0-h/hayya-alal-salah/actions/runs/46097403012/artifacts/15243952747)
(source `d25bea9`). Its checksum and established QA certificate verified. It has
not been installed or physically accepted in the maintenance pass. A later
reliability build may supersede it; always preserve the QA installation and data.

## Update preservation

Use these three signed builds in order on a QA installation that starts at version
code 48 or earlier. Each APK passed its artifact checksum and APK signature checks.
All three use `org.hayya.salah.qa` and the same QA signing certificate in [QA.md](QA.md).

| Step | Source | Version code | Download |
| --- | --- | --- | --- |
| Baseline | `8087095` | 48 | [Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43506421797/artifacts/14077311683) |
| First update | `7f5df5f` | 56 | [Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627420/artifacts/14078573268) |
| Second update | `5bff9bb` | 74 | [Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43553892997/artifacts/14097078942) |

After installing the baseline, create records with different statuses, a dated pause,
non-default calculation and appearance settings, and a small redistributable audio
import. Use `adb install -r <apk>` for both updates. Keep the installation and its data
between builds. After each update, verify records, status, origin date, pause, settings,
import playback, and alert readiness. Add both widgets at version 56 and verify that
they remain bound and display the correct times at version 74.

If the phone already has a newer QA build with data, preserve that installation and use
higher version codes for the update test. Use the latest signed QA build for the other
device journeys. Record physical results separately from artifact verification.

Salman confirmed record, theme, and imported audio preservation through this sequence
on his S23 Ultra. ADB confirmed that both widget bindings survived 56 → 74. This result
covers those updates. The S23 follow-up build still needs its own physical testing.

For the follow-up, download the [signed QA build 94 and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128442/artifacts/14833472002)
from source `8b2eda7`. Install it over build 74 with
`adb install -r hayya-qa-8b2eda74ec097220333a43d59c762bc804e7beb2.apk`.
Repeat the preservation checks before the spot checks below. The APK SHA-256 is
`727adcc063f67cd546a97f19d0706588ded015313198134397315345acc13ae1`.

## Reliability and privacy journeys

| Journey | Procedure and acceptance |
| --- | --- |
| Manual/offline setup | Deny location; choose a public city or coordinates and confirmed IANA zone/method. All times/history/Qibla numeric bearing work without network. Manual mode makes no location requests. |
| Calculation/date changes | Compare an explicitly matched method/location against the documented references; verify both Asr rules, custom offsets, midnight/date rollover and polar disclosure/confirmation. Record unexplained differences for review. |
| Explicit records/pause | Backfill and correct each status; remove a record and verify unrecorded. Check original dates after zone changes, unknown coverage/streak behavior, and suppression while paused without overwriting completed records. |
| Primary/extra alerts | Exercise notification-only and audio choices, notification/exact access denial/revocation, disabled channels, battery saver/idle and overlapping targets. Measure actual timing; no duplicates, stale bursts or fabricated records. Extras remain best-effort. |
| Audio interruptions | Verify each bundled/imported choice, ordinary silent mode, DND configurations, a phone call, transient competing audio, headphones/disconnection, Fajr priority and missing-import fallback. Check actual sound and cleanup, not just a playing indicator. |
| Fajr/lock screen | Test difficulty/offset/sound and keyboard at 100%/200% in both directions. The answer, scrolling content and emergency stop remain usable. Solving/dismissing/emergency stop never records a prayer. Verify full-screen denial fallback. |
| Overnight/reboot | Schedule a real future Fajr alarm, reboot with a PIN and leave the phone locked through the alarm. Verify bundled fallback before first unlock, then import/history preservation after unlock. Repeat overnight idle/OEM scenarios on S23 and mid-range hardware. |
| Lifecycle recovery | Exercise manual clock/zone changes, app update and reopening after force-stop. Android force-stop restrictions are respected; future events reconcile on reopening without replaying stale ones. Verify widgets/reboot recovery. |
| Focus | Enable only after disclosure/consent. Try selected/unselected apps and each prayer window; complete/unrecord/pause/expire, return to the app, open phone/settings/keyboard/emergency functions, bypass and revoke accessibility permission. Never trap essential functions or invent completion. |
| Location/Qibla | Compare explicit refresh/automatic foreground refresh, approximate permission, stale/unavailable location and border/zone confirmation. Check real bearing/calibration/rotation and sensor-quality fallback. Sensors stop when leaving Qibla. |
| Widgets | Bind both widgets on the actual launcher, resize narrow/tall, scroll all six rows at 200%, change language/theme/time/numerals/zone and reboot. Confirm countdown/boundary refresh and cleared displays after reset. No widget recording actions. |
| Local deletion | Remove an import and verify fallback; delete records; perform full reset and verify records/pauses/settings/imports/alerts/Focus/widget contents clear. Do not confuse the loss of local data on uninstall with a backup/recovery feature. |

Do not install debug-only fixtures or run `integration_ci.sh`, `direct_boot_ci.sh` or
benchmark seed scripts on a personal phone. They deliberately reset isolated emulator
state and are not the physical acceptance procedure.

## S23 follow-up spot checks

On Today, check that Fajr through Isha and sunrise fit at normal font size. At
200%, scroll to the last row and check that the Settings tab stays on one line.
Open the explicit status editor for an elapsed and an upcoming prayer on the
current date, then open the legend with the question-mark button. In Appearance, tap Ink Blue and check the
swatch, selected marker, and immediate recoloring. Open History and try the recent dated shortcuts
and the date picker. Check that coverage distinguishes missing entries from explicit
records and pause days.

For Qibla, use a known public city and note the numeric bearing and any quality
message. Hold the phone flat, away from a magnetic case or metal. Turn it slowly,
first in portrait and then with Android auto-rotate enabled. Record whether the
live arrow points the wrong way, jumps, or disappears, and note how the phone was
held when it happens. The numeric bearing should stay the same as the phone turns.
Tilt the phone up and verify that the live arrow yields to the numeric fallback.

## QA 94 follow-up spot checks

Download [signed QA build 108](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475811/artifacts/15136427874) and install over the existing QA app with
`adb install -r hayya-qa-f9540dc61453a01b142efb865da991fa5ac7f8a0.apk`.
The APK SHA-256 is `5545e071037207787d2390f238ef3f66309ce51afa96b400b3100b72f902f190`. Check record,
theme, audio import and widget preservation after the update, then follow the
spot checks below. [Build 103](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45739619637/artifacts/15080697574)
remains available as the first follow-up to build 94.

- From Today, open Location by tapping the city and return to Today. Tap the date,
  choose tomorrow, and check that tomorrow's times appear without a countdown or
  record controls. View a past date, correct a record if desired, and return to today.
  Confirm that an elapsed and an upcoming prayer both open the same record editor.
- Check recent dates in History and the divider above the navigation bar. At 200%,
  scroll through the schedule and try the calendar's text input.
- Swipe the Today timetable forward and back by one date, then use the date picker
  and Return to today. Check RTL direction, vertical scrolling at 200%, and reduced
  motion. Verify that future dates have no record controls.
- In Appearance, try midnight alone, last third alone, both and neither. The rows
  should line up with Sunrise. After-midnight times show their actual dates and
  unavailable solar times are announced as unavailable. On update from build 103,
  the previously selected night row should still be enabled.
- Under Prayer alerts, set a before and after reminder, verify the summary, then
  turn one off. Test initial notification access and a disabled channel separately.
- In Fajr settings, compare the next timestamp with an offset before and after
  Fajr. Switch to a fixed time, confirm it in the native picker and compare the
  next timestamp to the saved location's time zone. Try a pause that spans the
  next occurrence. Test a real future alarm and a locked overnight alarm separately.
- Set a dated pause and check that unrecorded rows show their paused state while
  saved records stay intact. End an active pause and check that today resumes and
  already paused dates stay paused.

## Human design/accessibility/language review

Review all destinations and dialogs in EN/RU/AZ with Salman and AR with a fluent Arabic
reviewer. Include mixed scripts, RTL dates/time, normal/200% system fonts, light/dark,
palettes, reduced motion, narrow/tablet widths, keyboard and actual widgets. Use
TalkBack to navigate and operate records, calendar, permissions, alarm challenge and
Focus emergency escape. Verify logical reading/focus order, labels, touch targets,
contrast and clipping. Screenshots alone do not establish spoken accessibility.

## Performance and power

Use the designated physical mid-range device with three years of synthetic records.
Keep cold/warm/useful-data p95, selected frame-overrun, settled PSS and APK size against
the unchanged targets in IMPLEMENTATION_PLAN.md. Measure Focus on/off and audio on/off
separately. Use system profiling traces to record CPU/wakeups/energy with event settings
and duration, including an idle period. Do not infer battery reliability from a single
percentage or software-rendered emulator measurements.

Track each row as pass/fail/not run with evidence and exact build/device conditions.
Review failures before preparing the separate production-signing/store approval packet.
