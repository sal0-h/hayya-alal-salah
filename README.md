# حي على الصلاة

Hayya ‘ala-s-Salah is a free, open-source, offline Android prayer application.

**Internal development preview; not a public v1 release.** Implemented features include
15 local calculation profiles across two reused engines, confirmed polar estimation,
explicit prayer records/history/pause/streaks, alerts and imported audio, a separate
Fajr wake-up challenge, optional Prayer Focus, offline locations, Qibla, Hijri dates
and two widgets. English, Russian, Azerbaijani Latin and Arabic are bundled.

The [validation ledger](docs/PROGRESS.md) distinguishes source implementation,
automated execution and physical testing. Emulators do not establish Samsung/OEM,
overnight, compass, audible playback, TalkBack or mid-range performance acceptance.

Build with the [pinned toolchain](docs/TOOLCHAIN.md):

```sh
./gradlew :core:test :praytimes:test :app:lintDebug :app:assembleDebug :app:assembleQa
python3 scripts/check_manifest.py
```

Debug uses `org.hayya.salah.dev`; stable signed internal QA uses `org.hayya.salah.qa`.
Download commit-labeled Actions artifacts using [QA instructions](docs/QA.md).
Performance fixtures use a third isolated ID and synthetic data. Production builds
remain unsigned during ordinary CI. No production credentials are needed to build.

No accounts, ads, telemetry, billing, runtime Internet permission or backend. Manual
location works without location access. Data stays local; uninstall/device loss has
no app-provided recovery in v1. [Privacy policy](PRIVACY.md).

- [Approved full plan](docs/IMPLEMENTATION_PLAN.md) and [current handoff](HANDOFF.md)
- [Architecture](docs/ARCHITECTURE.md), [contributing](CONTRIBUTING.md), [security](SECURITY.md)
- [Calculation research](docs/RESEARCH.md), [polar policy](docs/POLAR.md)
- [Third-party rights](THIRD_PARTY_NOTICES.md), [release preparation](docs/RELEASE.md)

Original code is GPL-3.0-or-later. Bundled libraries, geographic databases, recordings
and fonts retain their own free redistribution licenses and notices.
