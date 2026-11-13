# Implementation and validation ledger

Updated 2026-11-13. Code, automated execution and physical verification are separate.
The entire approved v1 plan remains active. Draft PRs are internal review checkpoints;
no merge, physical test or additional go-ahead is needed for independent server work.

## Code and evidence

| Area | Implemented | Automated execution | Physical testing |
| --- | --- | --- | --- |
| Foundation | Native Compose/API26–37, four modules, pinned engines/tools/dependencies, dedicated QA signing/ID, privacy CI | Clean CI, signed QA artifacts and 16 Today captures at `7d1a109` | None |
| Calculation | 15 profiles/two engines, Asr/high latitude/corrections, advanced parameters, explicit polar proposal/confirmation/manual reference, provenance, rollover/Ramadan | 17 core tests + preserved upstream fixture; clean tracking CI at `a8d2981` | No local authority/device comparison |
| Tracking | Room records/pauses/audio metadata, original date/zone, explicit correction/backfill/deletion, calendar, verified daily/per-prayer streaks, coverage/insights | Three API35 persistence/concurrency/preference tests passed at `a8d2981`; lint/debug/optimized QA/test builds passed | No install/update/process-kill/device QA |
| UI/settings | Today/History/Settings, calculation/location/pause/appearance/language/privacy/credits, four translations, fonts, RTL, time/numeral/theme controls | Today capture matrix passed; representative EN/AR normal/200% plus RU/AZ200 reviewed | No TalkBack/Arabic reviewer/full design approval |
| Alerts/Fajr | AlarmManager rolling window, exact primary/inexact fallback/extras, stale/duplicate/token checks, lifecycle reconciliation, original-target actions, minimal Direct Boot snapshot, contextual readiness | Local lint/debug/test APK and optimized QA assembly passed; snapshot/media instrumentation added, CI in progress for PR #3 | No timing/Doze/reboot-before-unlock/OEM/overnight tests |
| Audio | Three specifically licensed Ahmed Khalaf CC BY recordings, private bounded import with actual decoder validation, audio focus/foreground playback, silent/DND handling, Fajr priority/challenge/emergency stop | Compilation/lint; media instrumentation added, CI pending | No audible/call/headphone/DND verification |
| Focus | Explicit disclosure/consent, selected apps/prayers, narrow accessibility overlay, essential exclusions, completion/pause/bypass/expiry, pinned emergency bypass | Core expiry/bypass rules passed; installed service capability test added, CI pending | No OEM/overlay/TalkBack tests; Play review pending |
| Location | Bundled GeoNames cities/all native aliases, manual coordinates/IANA zone, bounded foreground-only platform requests, cached fallback, confirmed zone changes, explicit approximate offline zone suggestion | Core lookup fixtures/world edges passed locally; Android alias/resource/privacy test added, CI pending | No GPS/travel/border/device verification |
| Qibla/Hijri | Actual-coordinate Adhan bearing, visible-only magnetic/rotation sensors and true-north correction, quality/numeric fallback, named Umm al-Qura display and independent day correction | Core Qibla/Hijri tests passed; Android compile/lint validation in progress | No sensor/calibration/Arabic review |
| Widgets | Next/countdown and resizable daily RemoteViews; host countdown and boundary updates; no polling/bitmap payloads/check-off; pre-unlock placeholder | Android compile/lint validation in progress; host/resize/reboot tests pending | None |

Tracking CI evidence: [run 43389685112](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43389685112).
Downloaded JUnit XML confirms four API35 tests: the screenshot capture plus all three
persistence tests, zero failures/skips. Actual Today EN dark and AR200 light screenshots
were inspected again after tracking integration. These captures do not exercise the
full shell, dialogs, permissions, widgets or real system font-scale/inset behavior.

Foundation QA signing/checksum evidence remains in [QA.md](QA.md). QA keys are private,
ignored and separate from production. Dependency checksums are first-resolution trust,
not an independent supply-chain audit. Audio/geographic provenance and reproducible
asset extraction are documented separately in AUDIO.md and GEOGRAPHY.md.

## Server work still in progress

- Complete clean CI and meaningful Android delivery/Focus/widget/API-boundary checks;
  investigate failures and preserve actual reports. No compilation-as-delivery claims.
- Full-route/navigation/onboarding/recording/permission screenshots across four languages,
  normal/200% system text, themes, narrow/tablet widths; inspect actual artifacts and fix
  clipping, insets, RTL, labels and accessibility semantics.
- Additional seasonal/calculation/timetable fixtures with documented method differences.
- Macrobenchmark journeys, generated Baseline Profiles, three-year seeded history,
  release-like profiling and reproducible release checks. No performance figures yet.
- Final deletion/lifecycle/privacy/license/reproducibility checks, architecture/contributor
  docs, and concrete GitHub/F-Droid/Play release preparation. No merge/store publication.

## External release gates

No physical-device testing has run. Salman owns S23 Ultra QA; exact OS/One UI build is
still to be recorded. Arabic reviewer and physical mid-range testing remain required.
Alarm/Doze/OEM/Direct Boot/calls/sensors/TalkBack/overnight reliability and physical
performance budgets are unverified. No KVM exists on this server; CI runs emulators.
These gates do not block remaining independent implementation or server checks.
