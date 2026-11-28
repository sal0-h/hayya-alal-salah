# Implementation and validation ledger

Updated 2026-11-27. Source implementation, automated execution, visual inspection and
physical acceptance are separate. The full approved v1 scope is implemented in
PRs #1–#8. See Source consolidation below for merge approval and verification.
QA baseline `5bff9bb` passed the initial server checks. The S23 review follow-up and its verification are tracked below.

## Implemented scope and executed evidence

| Area | Implemented | Automated evidence | Physical acceptance |
| --- | --- | --- | --- |
| Foundation | Native Compose/API26–37, four modules, pinned tools/engines/dependencies, separate stable QA signing/ID | Clean builds, lint, manifest/privacy checks and signed QA artifacts | Reported S23 record/theme/import preservation for 48 → 56 → 74. QA 150 installed in place; visible settings/marks and navigation checked. |
| Calculation | 15 profiles/two reused engines, both Asr rules, advanced controls/corrections, high latitude, confirmed nearest-valid polar policy/manual reference/disabled, provenance, rollover/Ramadan | 21 core tests plus preserved upstream fixture; six seasonal provider references/36 times differ by at most one minute | No local religious-authority signoff |
| Tracking | Room records/pauses/audio metadata, explicit correction/backfill/deletion, original date/zone, calendar, daily/per-prayer streaks, coverage/insights | Persistence/reopen/concurrency/preferences, UI correction, actual original-target actions and full private-data deletion fixtures | Records reportedly survived the S23 updates to build 74. Process-kill QA pending. |
| Navigation/design | Today/History/Settings, all setup/settings/dialog routes, original geometric marker, bundled fonts, four languages/RTL, time/numerals/themes/palettes/reduced motion | Full-route/dialog capture matrix, navigation/labeled-control checks, real 100%/200% system fonts and narrow/tablet widths | QA 150 seven navigation checks passed and Salman accepted the result. TalkBack and fluent Arabic review remain open. |
| Alerts/Fajr | Seven-day stable alarms, exact/inexact readiness, best-effort extras, stale/duplicate claims, lifecycle reconciliation, authenticated original-target actions, challenge/emergency stop | Actual idle-capable AlarmManager delivery on APIs26/31/33/37; locked-reboot API37 probe verified active Fajr/bundled audio before unlock and preservation afterward | S23/OEM/overnight/Doze timing unverified |
| Audio | Three redistributable CC BY recordings, per-prayer/Fajr choices, private bounded validated imports/removal, finite foreground playback, ringer/DND guards, Fajr priority | Actual media preparation/import/corrupt rejection and MediaPlayer/audio-focus pause/regain/priority/emergency stop on all four APIs | Audible/calls/headphones/DND behavior unverified |
| Focus | Disclosure/consent, selected apps/prayers, metadata-only accessibility, essential exclusions, completion/pause/bypass/expiry, pinned emergency control | Real overlay lifecycle and permission removal passed all four APIs at `5bff9bb` with audio in a separate session | OEM/TalkBack and Play policy review pending |
| Location | 70,101 offline places with aliases and regional disambiguation, coordinates/IANA zones, bounded visible foreground requests/cache, confirmed zone changes and approximate border suggestion | Four-script city/zone fixtures and packaged privacy/resource checks | GPS/travel/border review pending |
| Qibla/Hijri | Actual-coordinate bearing, visible-only sensors/true-north correction, quality/numeric fallback, named Umm al-Qura display with independent correction | Core bearing/Hijri fixtures; Android lint and UI captures | Sensor/calibration and Arabic review pending |
| Widgets | Compact next/countdown and resizable scrollable six-row daily RemoteViews, native countdown/boundary updates, no check-off/polling, pre-unlock/reset placeholder | Actual host bind/render/resize/scroll/clear on four APIs; 32 four-language/font/theme captures | Both bindings survived 56 → 74 on the S23. Launcher rendering and reboot QA pending. |
| Performance/release | Three-year synthetic history, real generated profiles, optimized benchmarks, unsigned reproducibility, privacy/licenses/16 KiB/budget checks, review kits/store drafts/disabled F-Droid recipe | Two profile-generation and five benchmark/memory tests; independent exact-byte release builds; details below | Physical mid-range/power budgets and release/signing/store review pending |

## Earlier runs and retained evidence

The entire `7f5df5f` push passed [Android checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627420),
[performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627620) and
[reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627558).
Its ordinary suite executed 13 tests on each API26/31/33/37, plus one isolated denied-
permission test per API and the API37 locked-reboot preparation/preservation checks.
No empty/incomplete report is accepted. Reinstall/reset is restricted to isolated
synthetic emulator fixtures; ordinary QA updates must preserve data.

At `839b4d8`, the new actual audio-focus test passed all four APIs, bringing each
ordinary suite to 14 tests. [Android run 43514118846](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118846)
passed API26/33, build, QA signing and all 672 captures. API31 failed the initial Focus
overlay transition; API37 failed returning from the own-app exclusion. These failures
are not called green. `2303e42` removes window-event coalescing, rejects stale own-window
metadata and preserves external transitions arriving before focus loss; PR #6 also
contains that fix. `71a7c4b` reruns the complete matrix with stricter actual IME/control-
bounds assertions. Its API26/37 tests and locked reboot passed; API31 still missed
the initial overlay after the new audio fixture. `8a2708b` isolates audio and Focus
in separate instrumentation sessions (13 ordinary tests plus one real audio test per
API) and adds synthetic failure-state diagnostics. `250d740` also explicitly resizes
keyboard windows and checks the scrolling viewport against the actual status bar.

The same `839b4d8` passed [all five performance checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118876)
and [two independent release builds](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118906).
Both unsigned APKs were 11,043,311 bytes, SHA-256
`053741d56ded522b9575a5ef7a29c74003c4a574c61de4494475e7fc9a4fe6ac`.
Later source changes require their own matching evidence.

Real profile generation and all five optimized benchmarks passed at `d11e566`,
[run 43504979193](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43504979193).
Committed actual profiles contain 18,903 baseline and 15,718 startup rules; fixture
rules are excluded and the binary profile is checked in the optimized APK. Settled
Today PSS was 72.1 MiB with three years of synthetic records. Emulator startup did
not improve with the profile; no speedup or physical budget acceptance is claimed.
Raw measurements/provenance/hashes are in PERFORMANCE.md and evidence/performance-d11.json.

Frozen calculation comparisons, exact methods/coordinates, checksums and limitations
are in CALCULATION_FIXTURES.md. Published AlAdhan calculations may share upstream
ancestry; they are not independent local religious-authority approval. No new astronomy.

Earlier clean milestone evidence remains available: [foundation](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43345790379),
[tracking](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43389685112),
[alerts](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43394277151),
[location](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43395225120) and
[API hardening](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43506421797).
Actions artifacts expire; preserve reviewed evidence with an approved release.

## Actual visual inspection

The matrix totals 672 captures: 592 full-shell/routes/dialogs across four languages,
themes, real 100%/200% fonts and phone/tablet widths; 48 native Focus/Fajr/keyboard
captures; and 32 actual widget-host captures. Representative EN/AR normal/200% and
RU/AZ large-text artifacts were opened and inspected, not merely counted.

Inspection found and drove fixes for large-font calendar widths, compact widget
countdown clipping, asynchronous widget resize/scroll races and Fajr keyboard/body
insets. Current compact Arabic widget time/countdown is readable; daily content scrolls
to all six rows. The `839b4d8` Fajr answer was visible above the keyboard, but scrolled
text could paint under the status bar. The `71a7c4b` captures passed automated field bounds but visual inspection still
rejected the status-bar overlap. `12b5be9` sets explicit `adjustResize` and clips the
viewport; `250d740` checks its actual boundary and the pinned emergency control.
The 200% Arabic and English keyboard images confirm the corrected viewport.
Earlier captures obscured by a system error dialog were rejected, not approved.
Screenshot generation does not establish fluent Arabic review or physical TalkBack use.

## Privacy and release preparation

Runtime network permission, broad storage/app-usage/screen access, background location,
backup/transfer and exported-component protections are automatically checked. Full reset
and imported-audio deletion exercise actual private-file removal and error reporting.
Direct Boot keeps only upcoming event/sound/challenge/language/appearance data, never
coordinates/history/pause dates. Synthetic probes are absent from QA/release.

Dedicated QA credentials remain private and separate from production. Production
signing and public release or store submission require separate approval. Source,
unsigned APK, bundle, R8 mapping, hashes and four-language store drafts form a review kit.
The F-Droid recipe stays disabled until a reviewed release tag/certificate is supplied.
Private vulnerability reporting was verified enabled on 2026-11-13; SECURITY.md links
the private route. Public privacy hosting and store declarations await release review.
Dependency checksums are first-resolution trust, not an independent supply-chain audit.

## Remaining work and external gates

Physical acceptance, language review, production signing and publication review remain open.

The [device protocol](DEVICE_QA.md#update-preservation) lists the baseline signed APKs in order.
All use the same verified QA certificate and increasing version codes.
Salman reported that records, theme and imported audio persisted through builds 48 → 56 → 74 on his S23 Ultra. ADB confirmed both widget bindings survived 56 → 74. This verifies that update sequence only. The exact OS build was unrecorded for that early update sequence; QA 150 device details are recorded below. Arabic review, a physical mid-range tester, overnight alarms/Doze/OEM/Direct Boot/calls/headphones, live Qibla/TalkBack and physical
startup/frame/memory/power budgets remain public-release gates. They do not authorize
publication or excuse leaving independent implementation unfinished.

## Final server verification, 2026-11-14

Source `5bff9bb` passed [Android checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43553892997),
[optimized performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43553892993) and
[independent reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43553893029).
Downloaded JUnit XML confirms 13 ordinary tests on each API26/31/33/37, zero failures,
errors or skips. Each separate real audio session reports one passing test. Denial
checks, all 672 captures, build/lint/privacy and stable QA signing passed. The API37
locked-reboot probe confirms unlocked=false, active Fajr, bundled fallback and actual
alarm audio; post-unlock preservation passed. Actual 200% Arabic/English keyboard
captures were inspected and confirm the corrected viewport and pinned emergency stop.

Both independently built unsigned APKs have SHA-256
`5865029392bcd5602b5bc961dd2b3b334f831ce39fa67ccecc15769a73844a70`.
The source includes fixture reinstall, bypass persistence refresh and useful-display
dirty traversal fixes. Earlier draft #6 at `2a7ac4a` also passed Android/performance;
its transient pinned-artifact fetch failure cleared without checksum/version changes.
Historical failures above are retained as provenance and are superseded by these runs.
The prior approval-review usage limit cleared; one later approval timeout succeeded
on the permitted retry. No active blocker remains. This baseline run contains emulator and build evidence.

## S23 review follow-up, 2026-11-23

| Area | Implemented for the follow-up |
| --- | --- |
| Brand and appearance | Original rosette launcher mark. Sage surfaces with retained Ink Blue and Plum. Palette swatches expose selection and recolor the current screen. Compose, widgets and the native Focus overlay share palette values. |
| Today and dates | Shorter geometric marker and header. Six schedule entries with separate compact status controls for prayers. Already-started prayers can cycle explicit statuses, and the prayer name opens the editor. The legend explains clearing and pause behavior. Named localized Hijri months and local digits appear on Today. Appearance identifies the calendar convention, while Calculation owns the prayer time method. |
| Location and History | Ranked 70,101-place GeoNames index with aliases, regions and localized countries. First-run method confirmation finishes setup and opens Today. Existing location changes retain the calculation method. History uses recent-day shortcuts and an on-demand date picker, with explicit coverage and streak measures. |
| Qibla and Focus | Display-relative, declination-corrected heading rejects stale, low-quality and tilted measurements. The numeric bearing remains available. Focus allows staged bulk selection and cancellation, with unchanged opt-in and essential-app exclusions. |

Source `8b2eda7` passed 15 ordinary integration tests, one separate audio test
and one permission-denial test on each of APIs 26, 31, 33 and 37, with no failures
or skips. The API 37 locked-reboot report recorded active bundled Fajr audio while
credential storage was locked. The post-unlock fixture verified imported audio
and the saved record. The normal launcher flow covers first-run confirmation,
inline explicit statuses and clearing, the legend, and staged Focus selection
with cancel and apply. Android rotation and true-north correction use synthetic
matrices. [Android run](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128442)

Manual screenshot review covered Today, History, Settings and the selected Ink Blue
palette in English LTR and Arabic RTL at actual 100% and 200% system font sizes.
At normal size, all five prayers and sunrise appeared on Today without scrolling
in the compact phone capture. At 200%, Today scrolls and the English Settings tab
fits on one line. CI validated all 720 captures at `8b2eda7`.
[Reviewed captures at `9273199`](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45202921114/artifacts/14833108145) ·
[Build 94 capture matrix](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128442/artifacts/14835090790)

Source `8b2eda7` passed the optimized emulator benchmarks and profile checks.
Two clean workers produced byte-identical unsigned APKs.
[Performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128371) ·
[Reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128323)

Signed QA build 94 has package ID `org.hayya.salah.qa` and the previous QA
certificate. The downloaded APK checksum and signature verified locally. Its
SHA-256 is `727adcc063f67cd546a97f19d0706588ded015313198134397315345acc13ae1`.
[Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128442/artifacts/14833472002)

Physical evidence for the follow-up build is pending. Build 74's observed Qibla symptom still needs a description and reproduction on the S23 Ultra. Sensor behavior and overnight delivery remain open.


## QA 94 review implementation

Salman reviewed build 94 and requested changes to tracking, browsing, alarms and
Pause. Current source changes use the same explicit editor for all prayers within
the current city date. Today opens Location from the city label and an on-demand
calendar from the date. Past dates remain editable and future dates show schedules
without a live countdown or recording actions. History shows three dated shortcuts.
The optional night row uses the conventions documented in RESEARCH.md.

Enabled alerts show current optional reminders and open a focused editor. The Fajr
screen offers relative and fixed timing and shows the next eligible date and time.
Pause explains the effects and can end an active interval while preserving previous
dates. Alarm and Focus suppression cover the prayer’s service date and the applicable
local date. The QA 94 feedback is device observation of the previous build. The
follow-up requires its own Android, visual and physical evidence.

Build 101 at `e4ccf49` passed 16 integration tests on each of APIs 26, 31, 33 and
37, plus separate audio and permission-denial tests on each API. The API 37
locked-reboot fixture verified the fixed-clock alarm with bundled Fajr audio
before unlock and preservation of the saved record and imported audio afterward.
The Android 8 fixture opened the search keyboard and verified that Focus Apply
remained visible and tappable. [Android run](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45737860859)

CI validated 800 captures. The English LTR and Arabic RTL visual review at actual
100% and 200% covered Today, date input, History, the night time, prayer alerts,
the optional reminder dialog, Fajr settings and the native time picker, and Pause.
The follow-up captures confirmed that the first date characters and the narrow
picker actions fit after the screenshot-driven fixes. [Reviewed captures](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45737860859/artifacts/15079833916)

The build 101 APK checksum and QA signing certificate verified locally.
The benchmark run and its retry were rate-limited on the Kotlin JVM plugin marker
lookup. Commit `8798816` resolves the same pinned implementation module directly.
Core tests, Android test compilation, and the formerly failing Compose mapping
task passed offline in the RAM-backed checkout with dependency verification on.

The build 103 source `8798816` passed 16 integration tests on each of APIs 26,
31, 33 and 37, plus the audio and permission-denial fixtures on each API. The
API 37 report confirmed fixed-time Fajr playback with bundled audio while
credential storage was locked and preservation after unlocking. The benchmark
and the byte-for-byte unsigned APK comparison passed. Local Android lint and the
upstream PrayTimes fixture also passed. [Android](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45739619637) ·
[Performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45739619636) ·
[Reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45739619689)

Build 103 uses `org.hayya.salah.qa` and the established QA certificate. The
downloaded APK checksum and signature verified locally. The APK SHA-256 is
`d8a122cf9be0be948d368ce2821a89ff8e2e361ec9e77256f94791dd72314a44`.
[Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45739619637/artifacts/15080697574)

The new build still needs S23 update and physical acceptance checks. Overnight
delivery, live Qibla, Arabic review and the mid-range device checks remain open.

## Today swipes and night rows

Today pages horizontally one date at a time with RTL direction, short settling
motion and date-picker navigation. The date button exposes previous and next day
actions, and Return to today restores the live schedule. Appearance now has
independent midnight and last-third switches. Saved single-row preferences
migrate to their matching switch. The two rows use the Sunrise layout with dated
rollover and estimated or unavailable states.

Build 108 at `f9540dc` passed local core tests, Android test compilation and lint.
Each of APIs 26, 31, 33 and 37 passed 17 integration tests plus separate audio and
permission-denial fixtures. The API 37 emulator reported that Fajr was active and
playing bundled audio while credential storage was locked. The optimized emulator
benchmark passed, and independent unsigned builds were byte-identical with SHA-256
`8334fcebe1612ca4ca9f5d575f8b20d5fb37f435c0b4ba74c2ea0cc86ac62ff5`.
[Android](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475811) ·
[Performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475807) ·
[Reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475859)

CI validated 816 full-shell screenshots. Manual review of Today at actual
100% and 200% in English LTR and Arabic RTL checked the swiped date, its read-only
schedule, night-row alignment and after-midnight dates. The final large-text
layout keeps the English Midnight label on one line in the narrow phone capture.
[Reviewed captures](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475811/artifacts/15137426424)

Signed QA build 108 uses the existing `org.hayya.salah.qa` ID and QA certificate.
The downloaded signature and checksum verified. APK SHA-256
`5545e071037207787d2390f238ef3f66309ce51afa96b400b3100b72f902f190`.
[Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475811/artifacts/15136427874)

Build 108 swipe feel, in-place update, TalkBack, overnight alarms, live Qibla,
Arabic language review and mid-range checks remain pending on physical devices.

## Launcher Option 05

The selected prayer-mat mark replaces the rosette on a separate stack over the
build 108 release-preparation branch. The redraw uses editable vector paths, a
sage adaptive background and the same silhouette for Android's themed layer.
The selected reference, provenance, and mask fixture are documented in
[Launcher icon](branding/LAUNCHER_ICON.md).

Signed QA build 117 at `b6ed91e` uses the build 108 package ID
`org.hayya.salah.qa` and the same certificate. The downloaded signature,
resource variants, version and checksum verified. APK SHA-256
`3ca444c538e82af13d1d6c0aa7f0a3146d56cec0986fd4c6fa4fbd317db3d275`.
[Signed APK and checksum](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654353/artifacts/15138784269)

The source passed the Android build and lint checks and independent unsigned
release builds matched byte for byte. On API 33 all 18 integration tests passed,
plus the audio and denied-permission fixtures. The icon fixture verified three
separate shapes at 48 pixels, intact across the tested masks, and checked
that the themed layer has the same silhouette. [Android matrix and captures](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654353) ·
[Performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654414) ·
[Reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654399)

Visual review of the API 33 contact sheet covered circle, rounded square,
squircle, the emulator's native mask, and sample light and dark launcher
tints at 144 and 48 pixels. The Android 8 preview also kept the gaps intact at
48 pixels. [Reviewer contact sheet](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654353/artifacts/15139765801)

Samsung Home, its themed appearance, the system splash, and the physical
in-place update from build 108 still need S23 Ultra review.


## Navigation flash and date gestures

The Today and History calculation state now share a bounded in-memory cache scoped
to coordinates, time zone and calculation settings. Reopening a destination can
render its cached timetable immediately. Date arrows expose previous/next actions,
the date picker stays available, and each pager page scrolls vertically in its own
bounded viewport. Destination state preserves the browsed date across tab changes.
No record, alarm or persisted calculation semantics change.

Slow swipes now use a quarter-page threshold with native pager settling; small
nudges stay on the same date. Reduced motion still snaps. Only the three main
destinations retain state, and full reset clears that state. The next-prayer
service-date window is published atomically, including the previous date's Isha.

**Automated:** source `1d6978c` passed [Android checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252759),
including lint/build, 24 ordinary integration tests on each of API 26/31/33/37,
separate audio and permission/locked-reboot checks, and 816 full-shell/widget
captures plus the launcher sheet. Downloaded XML confirms all six new cache,
short-swipe, date-retention and reset regressions passed on all four APIs.
English/Arabic Today and future-day screenshots were visually inspected at
100%/200% system text size in light/dark themes. Existing
[performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252852)
and [independent rebuild](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252794)
checks passed; these are emulator results, not a physical performance claim.

**Physical:** signed [QA 150](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252759/artifacts/15146924960)
was installed over QA 141 on Salman's SM-S918B, Android 17/API 37, build
`CP2A.260605.016.S918BXXUAGZIF`. Its signing certificate matches the preceding QA
build and its SHA-256 is
`51ce539ff0b5fdbb29183a2f22ec18f66e9ad974322ddd9cd63204b4faeb2384`.
The running screen shows the new date controls and the previously visible city,
theme, night row and prayer marks. No prayer records were edited during QA.
A slow one-third-width swipe returning to the same day was reproduced on QA 141;
after reconnecting, QA 150 passed seven navigation-only checks: gentle forward
and backward day swipes, a small nudge retaining the date, the previous-day arrow,
return to today, selected-date retention across tabs and History scroll retention.
A private ten-tab-switch recording showed the full next-prayer panel whenever
Today reappeared; no collapse was observed in the captured frames. Screenrecord
sampling cannot rule out every display-frame issue or establish a latency speedup.
The phone's original USB stay-awake setting was restored and read back as `0`.
No prayer records were edited. Private device captures and records remain local
and are not uploaded.
The existing alarm, sensor, Arabic-review and mid-range release gates remain open.

## Source consolidation

Salman approved merging PRs #1–#8 in dependency order on 2026-11-26. The
approval covers source merge commits on `main`. GitHub records the merge
commits and checks for the stack. Use `main` as the base for subsequent feature
PRs once the stack lands. [Final stack PR](https://github.com/sal0-h/hayya-alal-salah/pull/8) ·
[Main CI](https://github.com/sal0-h/hayya-alal-salah/actions?query=branch%3Amain)

The Option 05 source behind signed QA build 117 passed all four Android
emulator integrations, 816 full-shell captures plus the launcher sheet,
performance and independent unsigned APK comparison. The S23 launcher and
update checks, overnight alarms, Qibla, TalkBack, Arabic review, and mid-range
device acceptance remain open. Public publication and production signing
require the separate release review.

## Post-merge verification, 26 November 2026

PRs #1–#8 are merged in dependency order. `main` at `e4cb6ee` has the same
Git tree as PR #8 head `729bcf7`; the merge preserved the reviewed app and icon.
Both the Dukhan main checkout and the local checkout were clean and synchronized
to this commit before this documentation follow-up.

[Main Android run 45869770771](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45869770771)
passed build/lint, screenshots and APIs 26, 31, 33 and 37. Downloaded JUnit reports
confirm 18 ordinary integration tests per API, with zero failures, errors or skips;
each separate audio and denied-permission report confirms one passing test. The
API 37 locked-boot probe reports bundled Fajr audio active before unlock.
The API 26 job failed resolving the pinned Kotlin plugin before app tests ran.
The isolated retry passed on attempt 2 without code, dependency or permission
changes. The overall Android run is successful. The build report also confirms
24 core tests and one preserved upstream PrayTimes fixture, all passing.
[Performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45869770650) and
[independent reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45869770631)
passed on the same main source.

[Signed QA build 141](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45869770771/artifacts/15142447273)
is `org.hayya.salah.qa`, version code 141. The downloaded APK's signature verifies,
its certificate matches the earlier QA builds, and its SHA-256 matches the artifact:
`305639a8260f4738544ad94141bf9e98adcb1c3de6768351a3fca6664eb00c3c`.
The actual main-source launcher sheet was inspected at 144 and 48 pixels across
circle, rounded square, squircle and light/dark themed variants; its gaps remain
visible. This verifies the update artifact; physical installation and data
preservation for this update, Samsung launcher appearance, sensors and overnight
alarms remain open.

Dukhan had 45 MiB free at takeover. Removing only ignored, regenerable
`benchmark/build` output recovered 347 MiB; source, signing keys, imported audio
and test evidence were preserved. No app behavior or dependencies changed in this
follow-up. Open Dependabot proposals remain separate from the merged app source.

## Repository maintenance, 27 November 2026

Salman authorized resolving all open PRs and cleaning the repository before alarm
and Focus reliability work. The maintenance branch combines proposals #9, #10,
#12 and #13 with the documentation from #14, retaining their source attribution.
It evaluates Compose BOM 2026.11.01 but retains 2026.09.00: the newer animation,
foundation and UI AARs require compile SDK 37.1 and fail the pinned 37.0 metadata
check. Proposal #10 is deferred to a dedicated SDK/UI upgrade. The accepted updates
are setup-java 6.3.0, Gradle 9.9.2 and AGP 9.5.3. Gradle wrapper JAR and distribution checksums match the official published
values. All 978 existing artifact checksum approvals remain unchanged. The 88 new approvals
were compared with published vendor sidecars (86 SHA-256, two SHA-1 with the cached
bytes also matching their committed SHA-256); see the dependency-maintenance JSON.
These same-origin checks do not establish an independent supply-chain audit.
Runtime dependency graphs are unchanged. The Windows wrapper index is normalized
under the existing CRLF checkout rule, and the integration report guard requires
24 ordinary tests with zero skips. Strict preflight, four-API integration,
screenshot, performance and reproducibility checks are pending. No new physical alarm or blocking acceptance is claimed.
