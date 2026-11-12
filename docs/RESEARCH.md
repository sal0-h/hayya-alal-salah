# Research ledger

Research date: 12 November 2026. Public descriptions are developer claims, not hands-on verification. Store platforms can differ. Reviews are qualitative anecdotes, sometimes historical. No production implementation or independent accuracy benchmark has been completed.

## Products

- [Just Pray Android](https://play.google.com/store/apps/details?id=com.justprayapp.justpray): documents times, Qibla, tracking, streaks/history/statistics, three before/during/after reminders, guides, AI reflections, garden, circles and optional AccessibilityService-based blocking. Subscription boundaries, exact calculation options, export/sync/offline guarantees and Android widget parity were not fully established.
- [Just Pray website](https://justprayapp.co/): advertises mat/sink scanning Fajr alarm, period pause, freezes and accountability. Marketing for both platforms does not establish identical implementations.
- [Just Pray iOS](https://apps.apple.com/us/app/just-pray-track-muslim-prayer/id6747154163): explicitly advertises home/lock widgets and multilingual support. Do not transfer iOS claims automatically to Android.
- [Pillars Android](https://play.google.com/store/apps/details?id=com.pillars.pillars), [FAQ](https://thepillarsapp.com/faqs), [tracker](https://thepillarsapp.com/prayer-tracker-app): restrained utility reference; location refresh on open/manual city support; tracking, streaks and period pause. Website/Android descriptions differ for some features. Privacy statements are declarations, not audits.
- [IslamicFinder Athan Android](https://play.google.com/store/apps/details?id=com.athan): broader suite with daily goals, tracking, audio, widgets, Quran/duas, ads and subscriptions.
- [Muslim Pro prayer features](https://www.muslimpro.com/prayer-times-and-adhan-app/): broader content/community product, regional settings, reminders/audio and progress logging.

Recurring reported frustrations: paywalls/ads, inconsistent tracking views, follow-up reminders after completion, lost records, notification failure, unclear calculation mismatch, guilt-inducing messaging. Individual reports are not verified current bugs or representative prevalence measurements.

## Open-source ecosystem

- [Al-Azan Compose](https://github.com/meypod/al-azan-compose), [releases](https://github.com/meypod/al-azan-compose/releases): AGPL-3.0; native Compose; offline search, audio/reminders/widgets/Qibla/backup reference. Release 2.4.1 observed 2026-11-03. [Old React Native repository](https://github.com/meypod/al-azan) archived and points to Compose rewrite. Study behavior; do not copy AGPL code into a GPL-only app without resolving license obligations.
- [Bilal](https://github.com/cdjalel/Bilal): GPL-3.0; local city database and Java calculations. Release 2.6 observed 2026-08-02.
- [Mihrab](https://github.com/MihrabHQ/Mihrab): AGPL-3.0-or-later; broad app, regional timetable work, journal/widgets. Accuracy claims are project reports, not independently verified by us.
- [Nedaa](https://github.com/NedaaDevs/nedaa): GPL-3.0; React Native/Expo with native alarm/compass/widget modules. Fetches prayer times and supports offline use after sync; not equivalent to wholly local calculation.
- [Unrelated Athan project](https://github.com/nandxorandor/Athan): illustrates asset-license differences: MIT code; some developer audio CC BY 4.0, other recordings noncommercial, launcher artwork reserved. Do not infer asset rights from the code license.

Recent activity signals do not guarantee maintenance, correctness or suitability. Pin exact reviewed revisions and inspect tests/issues before use.

## Calculations

- [Adhan Kotlin](https://github.com/batoulapps/adhan-kotlin), [releases](https://github.com/batoulapps/adhan-kotlin/releases): MIT. Existing Kotlin Multiplatform implementation and Java branch; version 0.0.7 observed released June 2026. Regional Sunni presets, Asr/high-latitude settings and adjustments. Documented public configuration lacks Ja‘fari Maghrib-angle support.
- [Kotlin PrayTimes](https://github.com/persian-calendar/praytimes): LGPL-3.0. Existing implementation contains Tehran/Ja‘fari/France/Russia profiles. Current source uses numeric timezone offset and wrapped hour output, requiring careful dated-instant adaptation. Main source revision observed: `1d1b4b5df73fbe6186c93d203c3ad0715fb989f6`. Do not assume the latest release flag equals the newest source tag. Current build files are not automatically suitable for Android; integrate/pin sources with notices and narrow documented adaptations.
- [PrayTime manual](https://praytimes.org/docs/manual), [changelog](https://praytimes.org/docs/changelog): upstream JavaScript v3 changed to MIT in 2025; older ports retain their own licenses. A new JVM JS runtime or new language port is unnecessary without evidence.
- [Adhan JS polar resolver](https://github.com/batoulapps/adhan-js/blob/develop/src/PolarCircleResolution.ts): nearest valid latitude and nearest valid day strategies. Port/reuse an established approach with attribution and validation, rather than new astronomy.

### Polar findings requested during interview

Pillars documents twilight-angle, middle-night and one-seventh-night rules, but its FAQ did not establish complete true-polar behavior. [Muslim Pro support](https://support.muslimpro.com/help/en/articles/high-latitude-adjustment) describes high-latitude settings without a complete textual polar algorithm.

[Mihrab source](https://github.com/MihrabHQ/Mihrab/blob/main/src/providers/localAdhan.ts) and [tests](https://github.com/MihrabHQ/Mihrab/blob/main/__tests__/polarDays.test.ts) substitute a fixed 48.5-degree latitude in the same hemisphere when ordinary local results are invalid/unordered. This differs from searching the nearest valid latitude. Implementation feasibility does not establish universal religious acceptance. Just Pray's exact polar behavior was not publicly established.

Approved product policy: explicit nearest-valid-latitude estimation, disclosure and manual override, not hidden automatic religious assumptions. High-latitude twilight correction is distinct from absent sunrise/sunset. Validate both hemispheres and Ja‘fari profiles.

## Android primary documentation

- [Alarms and permissions](https://developer.android.com/develop/background-work/services/alarms)
- [Doze/App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby): idle-capable alarm quotas matter; arbitrary closely spaced reminders cannot be promised exact.
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Notification channels](https://developer.android.com/develop/ui/compose/notifications/channels): user channel choices must be respected.
- [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Background FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Full-screen intent restrictions](https://developer.android.com/about/versions/14/behavior-changes-14)
- [Android 15 force-stop behavior](https://developer.android.com/about/versions/15/behavior-changes-all)
- [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17): background audio permission/stream/FGS requirements and widget bitmap memory limits need explicit testing.
- [Location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)
- [Widget updates](https://developer.android.com/develop/ui/compose/glance/glance-app-widget): avoid per-minute background updates; our selected simple widgets can use RemoteViews.
- [Android backup](https://developer.android.com/identity/data/autobackup): configure backup/transfer exclusions; allowBackup alone has platform/OEM nuances.
- [Accessibility](https://developer.android.com/develop/ui/compose/accessibility)
- [HijrahChronology](https://developer.android.com/reference/java/time/chrono/HijrahChronology)

## Architecture alternatives considered

Native Kotlin/Compose chosen for direct Android integration and maintainability. No measured evidence that Rust improves this bounded, infrequent computation workload. Hybrid Rust is feasible but adds foreign bindings, ABIs, native packaging and debugging. Predominantly Rust UI is possible but has no demonstrated advantage for our service-heavy Android scope.

- [UniFFI](https://mozilla.github.io/uniffi-rs/next/Motivation.html): bindings do not supply complete Android compilation/packaging.
- [Dioxus mobile](https://dioxuslabs.com/learn/0.7/guides/platforms/mobile/): WebView implementation / experimental alternative renderer.
- [Slint Android](https://docs.slint.dev/latest/docs/slint/guide/platforms/mobile/android/): Android backend exists.
- [KMP overview](https://kotlinlang.org/docs/multiplatform/kmp-overview.html), [stability](https://kotlinlang.org/docs/multiplatform/supported-platforms.html): viable future selective core sharing; not required now.

## Design, performance, build and licensing

- [IBM Plex](https://github.com/IBM/plex): OFL; glyph availability inspected but screens/shaping not tested.
- [Compose performance](https://developer.android.com/develop/ui/compose/performance)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview), [metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics)
- [Power Profiler](https://developer.android.com/studio/profile/power-profiler)
- [Headless builds](https://developer.android.com/build/building-cmdline), [real devices](https://developer.android.com/studio/run/device)
- [GitHub workflow artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data)
- [F-Droid reproducibility](https://f-droid.org/docs/Reproducible_Builds/), [signing](https://f-droid.org/docs/Signing_Process/)
- [GPLv3](https://opensource.org/license/gpl-3.0), [Apache-2.0](https://opensource.org/license/apache-2.0), [MIT](https://opensource.org/license/mit): GPL-3.0-or-later chosen for original code. Free-software licenses permit commercial redistribution; the official app's free/no-paywall policy is a project commitment.

Recheck current APIs, store policies, artifacts and dependency versions during implementation. Never turn proposed performance budgets or marketing accuracy claims into assertions that the implementation has already passed tests.
