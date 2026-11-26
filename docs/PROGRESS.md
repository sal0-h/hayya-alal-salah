# Implementation and validation ledger

Updated 2026-11-13. Code, automated execution and physical verification are separate.
The entire approved v1 plan remains active. Draft PRs are internal review checkpoints;
no merge, physical test or additional go-ahead is needed for independent server work.

## Code and evidence

| Area | Implemented | Automated execution | Physical testing |
| --- | --- | --- | --- |
| Foundation | Native Compose/API26–37, four modules, pinned engines/tools/dependencies, dedicated QA signing/ID, privacy CI | Clean CI, signed QA artifacts and 16 Today captures at `7d1a109` | None |
| Calculation | 15 profiles/two engines, Asr/high latitude/corrections, advanced parameters, explicit polar proposal/confirmation/manual reference, provenance, rollover/Ramadan | 21 core tests passed locally, including seasonal/public-provider comparisons; preserved upstream fixture | No local authority/device comparison |
| Tracking | Room records/pauses/audio metadata, original date/zone, explicit correction/backfill/deletion, calendar, verified daily/per-prayer streaks, coverage/insights | Three API35 persistence/concurrency/preference tests passed at `a8d2981`; lint/debug/optimized QA/test builds passed | No install/update/process-kill/device QA |
| UI/settings | Today/History/Settings, calculation/location/pause/appearance/language/privacy/credits, four translations, fonts, RTL, time/numeral/theme controls | Today capture matrix passed; representative EN/AR normal/200% plus RU/AZ200 reviewed | No TalkBack/Arabic reviewer/full design approval |
| Alerts/Fajr | AlarmManager rolling window, exact primary/inexact fallback/extras, stale/duplicate/token checks, lifecycle reconciliation, original-target actions, minimal Direct Boot snapshot, contextual readiness | Local lint/debug/test APK and optimized QA assembly passed; API35 snapshot/media tests passed at `07225e4`; real idle-capable delivery/duplicate/original-zone actions passed API26/31/33/37; locked-reboot probe rerun pending | No timing/Doze/reboot-before-unlock/OEM/overnight tests |
| Audio | Three specifically licensed Ahmed Khalaf CC BY recordings, private bounded import with actual decoder validation, audio focus/foreground playback, silent/DND handling, Fajr priority/challenge/emergency stop | API35 bundled media preparation passed; PCM import and corrupt-file rejection passed four API levels; audible/call checks external | No audible/call/headphone/DND verification |
| Focus | Explicit disclosure/consent, selected apps/prayers, narrow accessibility overlay, essential exclusions, completion/pause/bypass/expiry, pinned emergency bypass | Core expiry/bypass rules passed; API35 installed capability test passed; actual completion/pause/bypass/essential-app/permission-removal lifecycle passed API26/31/33/37 | No OEM/overlay/TalkBack tests; Play review pending |
| Location | Bundled GeoNames cities/all native aliases, manual coordinates/IANA zone, bounded foreground-only platform requests, cached fallback, confirmed zone changes, explicit approximate offline zone suggestion | Core lookup fixtures/world edges passed locally; API35 four-script alias/resource/privacy test passed at `a6fd568` | No GPS/travel/border/device verification |
| Qibla/Hijri | Actual-coordinate Adhan bearing, visible-only magnetic/rotation sensors and true-north correction, quality/numeric fallback, named Umm al-Qura display and independent day correction | Core Qibla/Hijri tests passed; Android compile/lint passed | No sensor/calibration/Arabic review |
| Widgets | Next/countdown and resizable daily RemoteViews; host countdown and boundary updates; no polling/bitmap payloads/check-off; pre-unlock placeholder | scrollable daily collection and actual host/resize/clear fixture compile/lint passed; OS execution pending | None |

Tracking CI evidence: [run 43389685112](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43389685112).
Downloaded JUnit XML confirms four API35 tests: the screenshot capture plus all three
persistence tests, zero failures/skips. Actual Today EN dark and AR200 light screenshots
were inspected again after tracking integration. These captures do not exercise the
full shell, dialogs, permissions, widgets or real system font-scale/inset behavior.

Foundation QA signing/checksum evidence remains in [QA.md](QA.md). QA keys are private,
ignored and separate from production. Dependency checksums are first-resolution trust,
not an independent supply-chain audit. Audio/geographic provenance and reproducible
asset extraction are documented separately in AUDIO.md and GEOGRAPHY.md.

PR #3 clean CI: [run 43394277151](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43394277151).
PR #4 clean CI: [run 43395225120](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43395225120).
The downloaded PR #4 JUnit report confirms eight tests with zero failures/skips:
snapshot(2), bundled media preparation(covered in snapshot class), screenshot(1),
Focus capability(1), packaged city/zone data(1), persistence(3). The city test took
13.446s across its queries; this motivated a pre-normalized streaming search key.
The improvement is implemented but not yet measured on an emulator.

Expanded CI at `f2a5a25`: [run 43502522662](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43502522662).
The build, dedicated QA signer, full 592-image capture matrix and API26/31/33
integration jobs passed. The API37 ordinary integration suite also passed; its job
failed afterward in the locked-reboot probe parser, which mistook printed Intent
braces for JSON. `d90c684` fixes parsing; Direct Boot delivery is still unverified
until the rerun succeeds. Do not mark the whole run green.

Each ordinary API suite executed 11 tests, including actual idle-capable AlarmManager
delivery and stale/duplicate rejection, plus authenticated original-zone actions on
API31+; PCM import and
corrupt-file rejection; real Focus overlay completion, unrecorded restoration, pause,
emergency bypass, essential apps and permission removal; navigation/accessibility;
and earlier persistence/snapshot/media/privacy/city tests. API26 previously returned
a false green after zero tests due to an APK reinstall failure; CI now uninstalls only
its isolated fixture installation and explicitly rejects empty/incomplete reports.

Current performance branch: Macrobenchmark journeys/three-year seed, actual profile
generation workflow, useful-data reporting and settled memory/alarm capture are
implemented. Both variants compile, but profile/metrics execution remains pending.
A real run exposed an uncompiled synthetic receiver in AGP’s Java source directory;
the source now uses the Kotlin directory set and a DEX-content check verifies it.
No generated profile or measured performance is claimed yet. A second clean worker
workflow and concrete unsigned release-review kit are implemented, not yet run.

Local tests now total 21 core + one vendored fixture. Six frozen published AlAdhan
seasonal references (36 event times) differ by at most one minute; exact methods,
coordinates, source URLs/checksums, deltas and limitations are in
CALCULATION_FIXTURES.md. This is not local religious-authority signoff. Regional
presets are suggestions for comparison only; nothing automatically changes the
chosen calculation method. New widget host, native Focus/Fajr/keyboard visual and
release-reproducibility checks are being executed next. No physical test has run.
Direct Boot configuration includes minimal language/appearance preferences, never
coordinates, records or pause intervals. Synthetic components remain absent from
QA/release; only isolated benchmark builds are shell-profileable.

## Server work still in progress

- Complete clean CI and meaningful Android delivery/Focus/widget/API-boundary checks;
  investigate failures and preserve actual reports. No compilation-as-delivery claims.
- Full-route/navigation/onboarding/recording/permission screenshots across four languages,
  normal/200% system text, themes, narrow/tablet widths; inspect actual artifacts and fix
  clipping, insets, RTL, labels and accessibility semantics.
- Review new frozen seasonal/provider comparisons and retain local-authority review as an external gate.
- Macrobenchmark journeys, generated Baseline Profiles, three-year seeded history,
  release-like profiling and reproducible release checks. No performance figures yet.
- Execute final privacy/license/reproducibility checks; review prepared contributor, privacy,
  signing/update, four-language store drafts and disabled F-Droid recipe. No publication.

## External release gates

No physical-device testing has run. Salman owns S23 Ultra QA; exact OS/One UI build is
still to be recorded. Arabic reviewer and physical mid-range testing remain required.
Alarm/Doze/OEM/Direct Boot/calls/sensors/TalkBack/overnight reliability and physical
performance budgets are unverified. No KVM exists on this server; CI runs emulators.
These gates do not block remaining independent implementation or server checks.
