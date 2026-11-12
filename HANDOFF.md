# Dukhan handoff

## Read first

1. `docs/IMPLEMENTATION_PLAN.md` is the approved specification.
2. `docs/RESEARCH.md` records sources, evidence limits and library provenance.
3. `AGENTS.md` gives concise working rules.

Salman approved implementation on 2026-11-12. Do not restart the product interview. The requested change is to perform development on Dukhan and test APKs locally on his Samsung S23 Ultra, using GitHub for code, CI artifacts and feedback.

## Actual implementation state

Documentation-only repository. No Android app, Gradle project, calculation adapters, CI pipeline, signing keys or production code have been created. No builds/tests have run. Do not mistake the detailed plan for completed functionality.

Start milestone 1: verify the Dukhan environment, scaffold the native Android/JVM modules, pin a compatible stable toolchain, add license/dependency notices, headless CI, and stable QA artifact delivery. Then proceed through the milestones with real validation. The full public v1 scope remains agreed; internal slices can be small.

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
