# Implementation and validation ledger

Updated 2026-11-13. This ledger distinguishes code, automated checks and physical
validation. A planned acceptance criterion is not a passed test.

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
- Clean-checkout CI build/lint/privacy and isolated QA signing passed at `45860c2`.
  Downloaded QA artifact checksum and v2/v3 signature verified; QA ID, min/target SDK
  inspected from the actual APK. Certificate SHA-256 is recorded in QA.md.
- The initial emulator test completed, but its artifact had reports only. Capture
  collection now uses AGP additional test output and CI requires all 16 actual PNGs;
  corrected capture execution and visual review remain pending.
- Dependency locks/checksum metadata generated. Checksum metadata is first-resolution
  trust, not an independent audit of upstream supply chains.

## Physical validation

None. No APK has been installed on Salman's S23 Ultra. No alarm, Doze, reboot, OEM,
TalkBack, sensor, overnight or performance claim has passed physical testing.
No KVM device is available on this server. Emulator capture/review is delegated to CI.
Arabic reviewer and physical mid-range device remain public release gates.

## Remaining milestone work

- Milestone 1: prove clean CI build and installable signed QA artifact. Two successive
  QA updates preserving records remain pending; record persistence is not implemented.
- Milestone 2: confirmed nearest-valid-latitude/manual polar policy, supported advanced
  overrides, larger upstream/published timetable comparison and seasonal boundary suites.
- Milestone 3: offline city search, automatic foreground location, in-app language
  selection on all supported Android versions, Asr/high-latitude/offset settings UI,
  actual normal/large LTR/RTL screenshot inspection, TalkBack and design review.
- Milestone 4: Room record repository, explicit prayer actions, correction/history,
  pause persistence, per-prayer streaks, coverage/statistics/insights.
- Milestones 5–10: Android alarm delivery, audio/import, Fajr/Direct Boot, Focus, Qibla,
  Hijri display, widgets, performance/physical/accessibility/release hardening.

The benchmark module reserves a boundary; no benchmark harness or measurements yet.
No store release is approved or published. Full public-v1 scope is unchanged.
