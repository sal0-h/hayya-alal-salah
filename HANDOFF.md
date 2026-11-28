# Dukhan handoff

## Read first

1. `docs/IMPLEMENTATION_PLAN.md` is the approved specification.
2. `docs/RESEARCH.md` records sources, evidence limits and library provenance.
3. `AGENTS.md` gives concise working rules.

Salman approved implementation on 2026-11-12. Do not restart the product interview. The requested change is to perform development on Dukhan and test APKs locally on his Samsung S23 Ultra, using GitHub for code, CI artifacts and feedback.

## Actual implementation state

PRs #1–#8 and #15 are merged. Start new work from `main`; the original milestone
branch refs are retained. The approved v1 feature set is implemented, with physical
release gates still open. [PROGRESS.md](docs/PROGRESS.md) separates implementation,
automated checks and physical results; use it for historical source/build evidence.

[Signed QA build 150](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45881252759/artifacts/15146924960)
(source `1d6978c`, merged through PR #15) is installed on Salman's S23 Ultra,
SM-S918B, Android 17/API 37, build `CP2A.260605.016.S918BXXUAGZIF`.
It includes Option 05, the earlier review fixes and the navigation/cache improvements.
All four API integrations, capture, performance and reproducibility jobs passed.
Seven navigation checks passed physically, and Salman accepted the result. Private
captures stay local; the original USB stay-awake setting was restored to `0`.

Maintenance PR #16 consolidates the compatible dependency proposals, closes the
superseded queue and refreshes this handoff. Its strict preflight, full Android,
performance and reproducibility checks passed at `d25bea9`. Signed QA157 is verified
and available; the phone remains on accepted QA150. See PROGRESS.md for evidence.
Next is alarm and Prayer Focus reliability. Do not claim new physical
alarm/Doze/OEM acceptance from existing emulator evidence. Qibla, audible playback,
TalkBack, Arabic review and physical mid-range/power acceptance remain open.

Production signing and store publication require their separate release review.
The QA identity/key is stable and private; never uninstall/reset a personal QA
installation or run synthetic CI seed scripts on it. Both update preservation and
alarm behavior must be tested with ordinary app controls and non-destructive tools.

JDK21 and Android SDK37 are under `/tmp/hayya-toolchain` on Dukhan. Task-owned
RAM build copies/caches are under `/dev/shm/hayya-*`; do not assume temporary paths
survive reboot. There is no KVM on Dukhan. Use GitHub Actions emulators and avoid
overlapping Gradle builds. Keep source, keys and private device evidence intact.

## Non-negotiable decisions

- Kotlin/Compose, Android 8+, Android-first isolated Kotlin core. Two reused prayer engines: Adhan plus existing Kotlin PrayTimes. No custom astronomy, Rust, runtime backend or KMP app build initially.
- International public launch, four languages: EN/RU/AZ Latin/AR. Original contemporary Islamic geometry, geometric next-prayer marker, Today/History/Settings. Frontend must be very non-AI-slop.
- Entirely local data initially; export/import deferred. No accounts, ads, telemetry, app Internet permission or AI coach. Explainable local insights only.
- Explicit prayer statuses and on-time streaks; no inferred missed prayers. Pause mode. No guides/notes/gamification.
- Configurable per-prayer alerts/audio; Fajr arithmetic challenge; firm optional app blocking with emergency bypass. Detailed semantics are in the plan.
- Manual no-location mode in same app; foreground refresh for automatic mode. No background travel tracking.
- GPL-3.0-or-later. GitHub, F-Droid, Play release preparation. Verify audio/font/data licenses separately.

## Development / testing split

Server does source changes, Gradle/JVM checks, APK builds, Git/PR work. GitHub Actions handles repeatable CI and emulator checks. Salman downloads commit-labeled APKs and physically tests on S23 Ultra. Collect exact Android/One UI build then; “latest software” was supplied, not an exact version. Additional physical mid-range testing is a public release gate.

Use separate QA application ID and stable QA signing so successive CI builds preserve local records. Keep production signing separate and out of Git. Do not assume emulators or KVM are available on Dukhan. Don't send private logs/location/history automatically.

## Prior machine setup (not app implementation)

The original local workspace had a protected read-only `.git` mount. Git was initialized there with elevated access. Local Codex config was migrated to a named workspace permission profile allowing only that project's `.git`; a fresh sandbox successfully tested Git writes while `.codex`/`.agents` remained protected. This machine-specific configuration was NOT copied into this repository or onto Dukhan. Check Dukhan independently; do not disable sandboxing globally.

Scratch toolchain downloads on the original machine under `/tmp` are not repository dependencies and must not be relied on. Availability checks observed AGP 9.5.1, Gradle 9.6.0, Kotlin 2.4.21, Compose BOM 2026.09.00, Room 2.8.5 and SDK platform 37.0 packages, but NO compatibility build was performed. Verify compatibility and freeze versions in the first milestone.

## Continuation prompt

> Implement the approved plan in docs/IMPLEMENTATION_PLAN.md. Read HANDOFF.md, AGENTS.md and docs/RESEARCH.md first. Work in this Dukhan checkout. Start with a verified cloud build and a real native vertical slice, then continue through the milestones. Preserve the agreed full v1 scope, original geometric visual identity and privacy/reliability constraints. Report actual tests and remaining work honestly. Use GitHub for commits/PRs and downloadable QA APKs; physical device validation happens with Salman on his S23 Ultra. Implementation is already approved; store publication needs a concrete release review.
