# حي على الصلاة — approved implementation plan

Approved by Salman on 12 November 2026 with “Implement the plan.” Development is to move to Dukhan through GitHub. This is the durable specification distilled from the completed research and interactive requirements interview. Implementation has not started.

## 1. Product and release scope

Build an original, free, open-source, offline Android prayer app. Priorities, in order: correctness/reliability; performance/battery; beautiful native UI; maintainability; minimal dependencies; practical future portability. Minimal by default, configurable when needed. No proprietary code, assets, branding, or visual cloning of Just Pray.

Android 8/API 26 minimum. Target the current stable SDK after checking its build-tool compatibility. Public international launch, with all agreed v1 features complete before the first public app release. Source may be public during development. Small internal milestones are encouraged; do not reinterpret the scope as a tiny public MVP.

### Agreed launch features

- Local Sunni and Ja‘fari calculation profiles; both Asr conventions; high-latitude settings; individual minute corrections; advanced angle/fixed-interval controls where supported.
- Automatic location refresh during app use, explicit refresh, and manual city/coordinate selection without device location access. Manual and automatic modes belong in one app, not separate APK products. No background travel tracking at launch.
- Prayer-first home, next prayer/time remaining, daily schedule and sunrise, inline completion actions.
- User-confirmed ON_TIME, MADE_UP, MISSED, EXCUSED records; UNRECORDED is distinct. No inference that a late check-in means late prayer or that silence means missed prayer. Backfilling/corrections supported.
- Prayer-history calendar, daily and per-prayer on-time streaks, statistics, explainable local insights. No written journal notes.
- Individual excuses and manual dated period/pause mode. No cycle prediction. Suppress relevant prayer alerts, Fajr alarms and blocking while paused without overwriting completed records.
- Per-prayer at-time alerts plus optional before/after reminders. Extra reminders are best-effort under battery restrictions. Suppress completed/excused targets and discard stale reminders rather than bursting them later.
- Small legally redistributable Adhan collection, distinct Fajr option, per-prayer audio choices and local audio import. Normal Adhan respects silent mode. Dedicated Fajr wake-up alarm uses Android alarm behavior; DND remains user-controlled.
- Fajr-relative wake-up alarm with configurable offset and optional adjustable arithmetic challenge. Emergency stop always available. Stopping the alarm or solving the task does not record prayer completion.
- Opt-in Prayer Focus: selected apps/prayers, blocks until prayer logged; firm behavior with emergency bypass, no routine snooze/skip. Expires at next prayer, except Fajr expires at sunrise. Essential system/phone functions remain accessible. Bypass must not fabricate completion.
- Qibla compass, numerical bearing fallback, calibration/sensor-quality feedback. Use actual coordinates even when prayer times use a polar reference.
- Compact next-prayer widget and resizable daily timetable widget. No widget check-off in v1.
- Named Hijri date convention and manual day adjustment; no events/fasting suite.
- English, Russian, Azerbaijani Latin and Arabic. Salman can review the first three; require an Arabic reviewer for launch. RTL, mixed text, large fonts and TalkBack are foundational.
- System/light/dark themes, a few reviewed palettes, time/numeral preferences, reduced motion. No deep font/layout customization.

### Deferred / excluded

Deferred: export/import (prioritize after v1), sync, circles, memory challenges, mat/sink recognition, iOS, app-level KMP builds. Local object recognition is a research item, not a promise: dataset rights, low-light behavior, false detections, size and energy need evaluation.

No learning guides, notes, gardens, badges, goals, generative AI, accounts, analytics, ads, payments, subscriptions or runtime backend in v1. Local insights are deterministic, explainable summaries, never generated religious advice.

## 2. UI/UX

Three main destinations: Today, History, Settings. Qibla opens from Today. History contains calendar, streaks, statistics and insights, not a separate tab for every feature.

### Original visual identity

Salman's explicit requirement: the frontend must be **very non-AI-slop**. Selected direction: contemporary Islamic geometry, composition A (geometric time marker). Native Compose implementation, not a web UI.

A compact original geometric frame surrounds the next prayer and its time. A readable daily list follows. Geometry should organize information, not become repetitive background ornament. Avoid generic rounded-card dashboards, decorative gradient washes, stock template styling and a feature-for-feature Just Pray clone. Use carefully judged typography, spacing, alignment and restraint.

| Token | Light | Dark |
| --- | --- | --- |
| Background | #F8FAFC | #101D26 |
| Main text | #172D3A | #E4EDF2 |
| Secondary text | #526875 | #A9B9C5 |
| Dividers | #CFD9DF | #354A59 |
| Primary accent | #086D79 | #76C9CF |

Bundle IBM Plex Sans (Latin/Cyrillic) and IBM Plex Sans Arabic under OFL. Initial in-memory glyph inspection found required Azerbaijani/Russian characters and sampled Arabic prayer labels; this is not shaping/layout QA. Initial additional curated accents: ink blue and plum, subject to contrast review. Draw geometry as native vectors, with no bitmap-heavy background.

Short action-responsive motion only; no constant animated background. Status meaning requires labels/icons, not color alone. Minimum 48dp touch targets, 200% text scaling, logical TalkBack order and RTL handling.

### Screen responsibilities

- Today: location/date, geometric next prayer, remaining time, daily times/sunrise and explicit status actions.
- History: past dates, correction, calendar and statistical views.
- Qibla: bearing/live dial, sensor quality and fallback.
- Fajr settings: enabled state, offset/sound/difficulty/test alarm.
- Focus settings: selected packages/prayers, permission disclosure and emergency behavior.
- Settings: location, calculation, alerts/audio, appearance, languages, pause, deletion, credits.
- Alert readiness: contextual permission/channel problem explanations and test alert, not an always-visible technical dashboard.
- Ringing screen: arithmetic task and emergency stop, accessible from lock screen without exposing private history.

Onboarding: language, location mode, confirmation of calculation profile. Suggest regional profiles but never infer religious practice from language/country. Request permissions contextually on enabling their feature. No setup permission gate prevents manual prayer-time use.

## 3. Technology and modules

Native Kotlin + Jetpack Compose. AndroidX lifecycle/navigation, coroutines/Flow, Room and DataStore. Constructor injection and small composition root; no DI framework without demonstrated need. No Rust or app-level KMP now. Keep Android APIs outside core rules for later reuse.

Four modules:

| Module | Responsibility |
| --- | --- |
| app | UI, persistence and Android integrations |
| core | Calculation adapters, schedule compiler, tracking/streak/insight rules |
| praytimes | Pinned existing Kotlin PrayTimes sources and documented adaptations |
| benchmark | Startup/interactions/calculation journeys and Baseline Profiles |

Use feature packages rather than one module per screen. Pin compatible stable toolchain/dependency versions; no floating dependencies. No runtime network API.

### Calculation engine reuse

Adhan Kotlin for supported Sunni profiles. Existing Kotlin PrayTimes for Tehran/Ja‘fari plus additional supported profiles such as Russia/France. One app-facing interface with explicit profile-to-engine mapping, only selected profile evaluated. Preserve MIT/LGPL notices and document source revisions/adaptations. Do not write new astronomy formulas.

Initial inventory: MWL, ISNA, Egyptian, Karachi, Umm al-Qura, Dubai, Qatar, Kuwait, Moonsighting Committee, Singapore, Turkey approximation, France, Russia, Tehran and Ja‘fari. Label approximations honestly. Do not claim exact agreement with every authority's timetable.

Contracts:

- Calculation engine: date, coordinates, profile/overrides; returns correctly dated instants or structured Unavailable.
- Result provenance: engine/profile version, actual/effective coordinates, adjustments and estimation quality.
- Schedule compiler: calculated events + preferences + records + clock; returns desired event set.
- Repository: atomic record changes, one observable source for UI/services/actions.
- Reconciler: idempotent desired-versus-installed alarm reconciliation with stable event IDs/configuration generations.

Use IANA zones and UTC instants. Preserve day rollover, including Isha after midnight. PrayTimes' numeric-offset/clock-hour output needs careful adaptation to preserve previous/next dates; expose unwrapped hour/day information instead of guessing from formatted strings. Keep existing algorithms, with narrow documented API adaptations for custom parameters.

Apply profile adjustments, manual corrections and consistent rounding so displayed and scheduled times agree. Make Umm al-Qura Ramadan interval behavior explicit/tested. Hijri display correction must not silently shift prayer calculations. Location/settings changes recalculate future schedules without reclassifying historical records. Invalid/NaN/unordered results never reach alarms or UI.

### Polar behavior

When normal/high-latitude calculation cannot produce a valid schedule, offer nearest-valid-latitude estimation (established Adhan approach), keeping longitude and selected timezone. Require confirmation when first needed, disclose reference latitude and label estimated boundaries. Permit manual reference-latitude selection or disabling estimation. No nearest-day mode in v1. Validate Sunni and Ja‘fari cases independently. Qibla still uses actual location. Estimation is not a universal religious ruling.

## 4. Data and tracking semantics

Room: records and pause intervals; DataStore: preferences. Atomic record updates and observable repositories synchronize Today, History, notification actions and Focus.

| Model | Contents |
| --- | --- |
| Prayer record | Service date + prayer ID unique key, status, created/updated instants, originating timezone |
| Location | Coordinates, name, IANA zone, mode, last refresh |
| Calculation settings | Profile/version, Asr/high-latitude/polar choices, supported parameters, individual offsets |
| Alert preferences | Per-prayer mode, before/after offsets, audio asset |
| Fajr settings | Enabled, offset, sound, arithmetic difficulty |
| Focus settings | Packages/prayers, enabled, current emergency bypass |
| Pause interval | Dates and suppression state |
| Audio asset | ID, private file reference, duration, provenance/attribution |
| Scheduled event | Stable ID, occurrence instant, prayer/service date, kind, generation |

Deleting a record restores unrecorded. Made-up completion replaces missed for the original prayer date. Do not retarget actions to whichever date/prayer happens to be current when tapped.

Daily streak requires all non-excused prayers on time. Per-prayer streak uses that prayer's on-time records. Excused dates pause without advancing or breaking. Unknown dates interrupt verified continuity but are labeled unconfirmed, not missed. Make-ups do not restore on-time streaks. Corrections recompute history. Percentages display recording coverage, never silently count missing data as failures.

## 5. Android integration

### Alarms and lifecycle

AlarmManager, not WorkManager, drives time-critical delivery. Appropriate exact idle-capable alarms for primary prayer alerts; setAlarmClock for explicitly enabled Fajr wake-up. Extra reminders best-effort. Rolling seven-day schedule, refreshed after deliveries/app use/system changes, plus lightweight inexact maintenance. No background countdown/polling.

Reconcile after reboot, unlock, package replacement, clock/timezone changes, preferences/location changes and permission changes. Stable explicit immutable PendingIntents, generation checks, duplicate suppression and stale-event dropping. Coalesce overlapping playback; Fajr wake-up takes precedence.

Permission/readiness handling must be honest. Without exact access do not claim a precise wake-up alarm. Force-stop/OEM restrictions cannot be defeated or hidden. Battery-optimization bypass should not be demanded universally without evidence of need.

For pre-unlock reboot support, device-protected storage contains only minimal upcoming alarm snapshot/configuration, not prayer history/coordinates. Credential-protected records remain private. Imported audio unavailable before unlock falls back to bundled audio. Reconcile fully after unlock.

### Playback and notifications

Foreground media-playback service exists only while ringing/playing full Adhan. Stop/release resources and audio focus promptly. Normal Adhan respects silent state; Fajr uses alarm behavior under Android DND policy. Explicitly test Android 17 background audio requirements. Respect user-modified notification channels; never recreate channels to bypass choices. Full-screen access may be denied; notification-based fallback remains usable. Arithmetic completion/emergency stop does not log prayer.

### Other integrations

- Platform foreground location with bounded requests/cached fallback; bundled offline city/zone data; no remote geocoding. Manual mode makes no location requests.
- Narrow accessibility service, foreground package/window events and accessibility overlay for Focus. No screen content, keystroke reading, screenshots or gesture automation. Consent/disclosure and Play review required. Keep system settings, launcher, input and essential phone functions reachable.
- Simple RemoteViews widgets, event-driven updates and host-rendered countdown where appropriate. No per-minute background worker; observe Android 17 bitmap payload limits.
- System document picker for audio, validate decoding then copy to app-private storage; no broad storage access.
- Qibla sensors active only while visible, true-north correction and sensorless fallback.
- Named Umm al-Qura Hijri display with manual day correction, separate from calculation offsets.

## 6. Privacy, licensing and maintainability

Original code GPL-3.0-or-later. No accounts, telemetry, ads, runtime Internet permission or app-originated network requests. Keep app data local, exclude from cloud backup/device transfer using supported controls. OS location services remain outside app control; manual mode avoids location access entirely.

Provide record deletion/full reset. Export/import deferred means no app-provided recovery after uninstall/device loss in v1; explain this plainly. Use app-private credential-protected storage and platform encryption; do not add an encryption framework without need. Minimal alarm snapshot is the only Direct Boot exception.

Every dependency/font/recording/art asset needs verified redistribution rights and attribution. Repository code license does not cover all bundled assets. Prefer properly free audio; noncommercial-only assets are not suitable for a genuinely FOSS distribution. Keep signing secrets out of Git/logs.

Contributor documentation, architecture decisions, calculation references, issue templates, security reporting, pinned toolchain/dependencies/CI actions, dependency verification/locks and reproducible builds. Aim for upstream-signed reproducible F-Droid APKs. Document cross-store signing/update compatibility before release, especially while export is deferred.

## 7. Cloud coding and local device testing

GitHub repository: sal0-h/hayya-alal-salah. Coding and headless builds happen on Dukhan; GitHub Actions runs checks and publishes test artifacts. Local real-device testing on Salman's Samsung S23 Ultra (exact OS/build recorded at test time). Obtain a physical mid-range test device before public release; flagship results alone are insufficient.

Workflow: branch on server; PR; CI lint/unit/calculation/build/emulator screenshots; download commit-labeled QA APK; local install/test; report commit/device/OS/reproduction through GitHub; fix on server.

Use stable dedicated QA signing and separate app ID so updates preserve test records without sharing production identity. Fork PRs get no signing secrets. Production release signing is protected/separate. Benchmark release-like builds, not debug. Android Studio need not run on server. Server emulation depends on KVM; use CI for emulator checks if absent.

### Proposed budgets, not measured claims

| Measure | Initial acceptance target on designated mid-range device |
| --- | --- |
| Cold initial display | p95 <= 1 second |
| Warm initial display | p95 <= 400 ms |
| Useful prayer data | p95 <= 1.5 seconds |
| Selected interaction journeys | p95 frame overrun <= 0ms |
| Settled Today PSS | <=120 MiB, three years of records, audio inactive |
| Release APK | <=35 MiB with bundled fonts/audio; user imports excluded |
| Idle | No polling, wakeups attributable to enabled events/maintenance |
| Manual location | Zero requests |
| Runtime app network | Zero requests |

Macrobenchmark, Baseline Profiles, R8, Perfetto/Android Studio, real-device power measurements. Separate Focus on/off and audio on/off scenarios. Record wakeups/CPU/energy, not unsupported battery-percent claims. Calibrate after first working slice and explicitly justify any budget revisions.

## 8. Tests

- Calculation: upstream fixtures, parameters, local published timetable comparisons with known method/location, Asr variants, Ja‘fari Maghrib/midnight, Ramadan, DST transitions, fractional zones, date line, leap dates, polar transitions, invalid input. Timetable deviations must be explained rather than claiming universal exact matching.
- Data: atomic updates, process death, migrations, correction, pause precedence, streaks, synchronized views/actions.
- Android: notification/exact-alarm denial/revocation, Doze, battery saver, reboot-before-unlock, timezone/clock, package update, force-stop recovery, channel changes, audio focus/calls, silent/DND, missing imports, overlapping events.
- Focus: selected/unselected apps, essential exclusions, disable permission, bypass/expiry, completed/paused cases.
- UI: all four languages, RTL/mixed scripts, TalkBack, 200% text, themes, narrow screens/tablets, widget sizes/recreation.
- Privacy/release: merged manifest permission checks, no unexpected network SDKs, backup exclusions, licenses, clean/reproducible builds.

Emulator matrix at key API/permission boundaries and physical overnight testing on Samsung and a mid-range device. Do not claim tested physical behavior from compilation/emulator results.

## 9. Milestones and acceptance

| Milestone | Deliverable | Acceptance criteria |
| --- | --- | --- |
| 1 | Repository/cloud build, license, pinned tools, CI, QA delivery | Clean checkout builds; CI passes; two QA APK updates preserve test records |
| 2 | Both calculation adapters, profiles, timezone/polar handling | Fixture suite passes; invalid results never schedule; provenance/estimates explicit |
| 3 | Four-language UI shell, setup, geometric Today | Offline setup works; original design reviewed; RTL and large text usable |
| 4 | Internal tracking MVP, history, pause, streaks/insights | Changes consistent everywhere; persistence/process death and unknown/excused/missed cases pass |
| 5 | Primary/extra alerts, audio/import, readiness | Controlled timing checks pass; completed targets suppress; no stale bursts/duplicate audio |
| 6 | Fajr alarm/challenge/emergency stop/Direct Boot | Overnight S23 tests; bundled pre-unlock fallback; no completion inferred from dismissal |
| 7 | Focus blocking | Selected apps only; essential functions accessible; bypass/completion/pause/expiry clear blocks |
| 8 | Qibla, Hijri, widgets | Sensor fallback, separate Hijri correction, resizing/reboot persistence |
| 9 | Localization, accessibility, device/performance/privacy hardening | Arabic review; Salman reviews EN/RU/AZ; physical mid-range/overnight tests meet gates |
| 10 | Public v1 release preparation | No unresolved calculation/data-loss defects; rights verified; signing/update paths documented; reproducibility/release checks pass |

Prepare GitHub releases, F-Droid submission and Google Play submission. External store acceptance is not guaranteed. Do not publish an unfinished app as the agreed broad v1. Implementation approval has been given; prepare concrete release artifacts for approval before store publication.

## 10. Risks and dependencies

Need Arabic reviewer, mid-range physical tester, suitable redistributable audio and external store approval. OEM restrictions, AccessibilityService compatibility, calculation-library defects and timezone/polar mistakes require specific validation gates. No Rust, backend, ML model or extra framework without concrete justification and scope discussion. Later local recognition must remain a researched optional feature.
