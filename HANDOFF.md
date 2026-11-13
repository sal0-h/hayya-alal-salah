# Dukhan handoff

## Read first

1. `docs/IMPLEMENTATION_PLAN.md` is the approved specification.
2. `docs/RESEARCH.md` records sources, evidence limits and library provenance.
3. `AGENTS.md` gives concise working rules.

Salman approved implementation on 2026-11-12. Do not restart the product interview. The requested change is to perform development on Dukhan and test APKs locally on his Samsung S23 Ultra, using GitHub for code, CI artifacts and feedback.

## Actual implementation state

Active branch: `implement/validation-release-hardening`, stacked on draft location/
Qibla/widget PR #4, then alerts PR #3, tracking PR #2 and foundation PR #1.
The draft is an internal checkpoint. Salman explicitly requires autonomous continuation
through **all** server-implementable milestones; do not stop at a runnable slice,
wait for a merge/device test, or ask for another go-ahead. Physical QA remains an
external release gate, not a reason to leave independent implementation unfinished.
See `docs/PROGRESS.md` for the distinction between code, automated and physical tests.

Temporary JDK 21, Gradle 9.6.0, SDK platform 37.0 and build tools 36.0.0 are under
`/tmp/hayya-toolchain` on this server. Set JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME
accordingly, or install the documented pinned toolchain independently. No KVM here.
Do not rely on /tmp surviving a reboot. `.local-qa/` holds an ignored private QA key;
never commit it. Dedicated QA secrets are configured in repository Actions settings.

Foundation clean CI, signed QA delivery and representative Today screenshots passed
at `7d1a109`. Calculation controls, confirmed polar policy and persistent tracking are
implemented and passed CI. Alerts/audio/Fajr/Direct Boot/Focus are in PR #3 with
local compilation/lint/optimized assembly; the seven-test API35 emulator suite passed.
Offline location, visible-only true-north Qibla and two RemoteViews widgets are now
implemented in PR #4 and passed clean CI (18 core + upstream + eight API35 tests).
The current branch adds full-route/dialog captures at real 100%/200% system fonts,
320dp/tablet widths, accessibility/navigation tests, actual alarm/idle/actions,
actual Focus overlay checks and a reboot-before-unlock fixture. These new emulator
checks are being prepared/run; do not claim them passed from compilation.
Continue through CI fixes/review, Macrobenchmark/generated Baseline Profiles,
three-year records, reproducibility and concrete release preparation. Do not stop
when a milestone PR turns green. Never
interpret a milestone or foundation PR as completion of the approved full plan.

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
