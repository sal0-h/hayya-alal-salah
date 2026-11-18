# Physical release acceptance

This is a prepared test protocol, not a completed result. Use the stable signed QA
application and synthetic records/public locations. Record commit, QA version code,
device model, exact Android/One UI build, locale/font scale and relevant Android
permission/channel/battery settings. Keep personal coordinates/history/audio out of
shared screenshots and logs. Record observed timing and failures; do not mark a row
passed because the feature compiles or an emulator passed it.

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
higher version codes for the update test. Use version 74 for the other device journeys
if the phone can install it. Record physical results separately from artifact verification.

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
