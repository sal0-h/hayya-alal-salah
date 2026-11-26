# حي على الصلاة

Hayya ‘ala-s-Salah: a free, open-source, offline Android prayer application.

**Status: internal development preview.** The first native slice provides manual
coordinate/time-zone setup, 15 local calculation profiles, and a geometric Today
screen in English, Russian, Azerbaijani and Arabic. It is not the agreed public v1.
Prayer tracking, alarms, Focus, widgets and Qibla remain under implementation.

Use JDK 21 and the Android SDK packages documented in [toolchain setup](docs/TOOLCHAIN.md).

```sh
./gradlew :core:test :praytimes:test :app:lintDebug :app:assembleDebug :app:assembleQa
python3 scripts/check_manifest.py
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (`org.hayya.salah.dev`).
Dedicated signed QA APKs are delivered by GitHub Actions for trusted pushes using
`org.hayya.salah.qa`. See [QA delivery](docs/QA.md) and the [progress ledger](docs/PROGRESS.md)
for what has actually been validated.

- [Approved full implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Research and sources](docs/RESEARCH.md)
- [Current development handoff](HANDOFF.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

Original code is GPL-3.0-or-later. Vendored PrayTimes, Adhan, fonts and dependencies
retain their licenses. No accounts, ads, telemetry, runtime Internet permission or
backend. No production signing credentials are used by ordinary builds.
