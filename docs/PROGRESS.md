# Implementation and validation ledger

Updated 2026-11-13. This ledger distinguishes code, automated checks and physical
validation. A planned acceptance criterion is not a passed test.

## Active continuation

The full approved plan remains active. `implement/calculation-tracking` is stacked
on draft foundation PR #1; neither merge nor physical testing blocks independent
server work. New implementation includes confirmed polar estimation and advanced
parameters, Room records/pauses/import metadata, atomic versioned preferences,
Today/History/Settings navigation, explicit correction/backfill, dated pauses,
daily/per-prayer verified streaks, recording coverage and explainable statistics,
language/appearance/time/numeral settings, and local deletion/reset.

17 core tests plus the preserved upstream fixture passed locally. Android persistence
instrumentation was added and compiled, but has not yet run on an emulator. Full
Android lint/APK validation is in progress; do not treat compilation as device QA.
The city dataset and freely licensed developer recordings are staged for Android
integrations; staging assets is not a working alert or location feature.

The older foundation ledger below records validation at its cited commit. Its
remaining-work list is historical; new work must be checked against the full plan.

## Implemented

- Native Android/Compose app (API 26 minimum), isolated JVM core, separate vendored
  PrayTimes module, reserved benchmark module, pinned toolchain and Gradle wrapper.
- Adhan 0.0.7 and pinned Kotlin PrayTimes adapters with explicit profile routing,
  all 15 initial profiles, IANA zone conversion, solar/civil date alignment,
  minute corrections, two Asr conventions and high-latitude rules in core.
- Explicit Umm al-Qura Ramadan interval (120 minutes, determined with the unadjusted
  platform Umm al-Qura chronology); normal interval 90. Display Hijri corrections
  are not involved. Dates outside 1900–2100 return structured unsupported results.
- Unwrapped PrayTimes output retains date rollover; engine astronomy unchanged.
  Non-finite/unordered results are unavailable, including unsupported polar schedules.
- Pure explicit record/status, day classification and verified daily-streak rules;
  pure desired-alert compiler with original service-date targets, stable IDs,
  generations, stale-event filtering and completed/excused/paused suppression.
- Saved manual coordinate/IANA-zone/profile setup in DataStore; local Today schedule,
  next prayer including tomorrow, foreground-only countdown, geometric time marker,
  system theme and 24-hour preference, bundled IBM Plex fonts.
- Complete shell resources for EN/RU/AZ/AR, RTL declaration and system per-app locale
  configuration. Translation quality is not yet reviewed.
- No Internet or location permissions; backup/cloud-transfer exclusions.
- CI builds/tests/privacy checks, dedicated QA ID and isolated signing job, debug-only
  synthetic screenshot activity and a 16-configuration capture matrix.

## Automated validation

Local command passed on 2026-11-13 using the pinned toolchain:

```
gradle :core:test :praytimes:test :app:lintDebug :app:assembleDebug :app:assembleQa :app:assembleDebugAndroidTest --continue --write-locks --write-verification-metadata sha256
python3 scripts/check_manifest.py
```

- 12 core tests passed: pinned upstream Adhan Raleigh expected-time fixture; all 15
  profiles; both engines across fractional zones, DST, leap date and date line;
  Asr conventions; Ja‘fari Maghrib/midnight; explicit Ramadan interval; polar failure;
  invalid inputs/order; explicit unknown/excused/made-up streak behavior; suppression,
  stable event IDs/generations, original after-midnight service-date targeting, and
  next-prayer selection of previous-service-date Isha after civil midnight.
- Preserved upstream PrayTimes correctness fixture passed (one test, multiple cases).
- Android lint passed with warnings treated as errors. Only new-version advisories
  are excluded because versions are deliberately pinned; compatibility checks stay on.
- Debug, optimized/shrunk unsigned QA and screenshot-test APK assembly passed.
- Merged debug/QA manifests passed no-Internet/no-location and backup-control checks.
- All resource XML parsed and four language key sets match. `fc-query` confirmed
  sampled Azerbaijani/Russian/Arabic glyph coverage in all four bundled font files.
- Strict offline build passed without regenerating locks or verification metadata.
- Clean-checkout CI build/lint/privacy, optimized QA assembly/signing and emulator
  capture passed at `7d1a109` in [run 43345790379](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43345790379).
  CI ran 12 core tests, the upstream fixture and Android screenshot instrumentation.
- All 16 actual PNG files were retained and validated (four languages × two text scales
  × two themes), on an Android 15/API 35 x86_64 emulator. Downloaded final images were
  inspected: EN and AR at 100%/200% in both themes, plus RU/AZ at 200%. The review caught
  global-locale date formatting and wrapped Russian-label alignment; both were corrected
  and the final captures confirmed localized dates/times and centered wrapping.
- This is a representative Today-component review, not full UI/accessibility approval.
  Full MainActivity/onboarding/navigation, narrow screens, system font-scale/inset behavior
  and TalkBack still need review. The context-overridden fixture has imperfect system-bar
  rendering; its snapshots do not establish production system-bar behavior.
- Final downloaded QA checksum and v2/v3 signature verified. Actual APK: separate QA ID,
  min API 26/target 37, version code 9, 1,856,869 bytes. Its certificate matches the earlier
  CI APK (version code 5); actual installation/update preservation remains untested.
- Dependency locks/checksum metadata generated. Checksum metadata is first-resolution
  trust, not an independent audit of upstream supply chains.

## Physical validation

None. No APK has been installed on Salman's S23 Ultra. No alarm, Doze, reboot, OEM,
TalkBack, sensor, overnight or performance claim has passed physical testing.
No KVM device is available on this server. Emulator capture/review is delegated to CI.
Arabic reviewer and physical mid-range device remain public release gates.

## Remaining milestone work

- Milestone 1: clean CI and signed QA artifacts are verified. Physical installation and
  two successive QA updates preserving records remain pending; record persistence is
  not implemented.
- Milestone 2: confirmed nearest-valid-latitude/manual polar policy, supported advanced
  overrides, larger upstream/published timetable comparison and seasonal boundary suites.
- Milestone 3: offline city search, automatic foreground location, in-app language
  selection on all supported Android versions, Asr/high-latitude/offset settings UI,
  full-route/narrow-screen/system-inset captures, TalkBack and product design review.
- Milestone 4: Room record repository, explicit prayer actions, correction/history,
  pause persistence, per-prayer streaks, coverage/statistics/insights.
- Milestones 5–10: Android alarm delivery, audio/import, Fajr/Direct Boot, Focus, Qibla,
  Hijri display, widgets, performance/physical/accessibility/release hardening.

The benchmark module reserves a boundary; no benchmark harness or measurements yet.
No store release is approved or published. Full public-v1 scope is unchanged.
