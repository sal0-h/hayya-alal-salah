# Dukhan handoff

## Read first

1. `docs/IMPLEMENTATION_PLAN.md` is the approved specification.
2. `docs/RESEARCH.md` records sources, evidence limits and library provenance.
3. `AGENTS.md` gives concise working rules.

Salman approved implementation on 2026-11-12. Do not restart the product interview. The requested change is to perform development on Dukhan and test APKs locally on his Samsung S23 Ultra, using GitHub for code, CI artifacts and feedback.

## Actual implementation state

Active branch: `implement/release-preparation`, draft PR #7, stacked on performance
PR #6, API hardening PR #5, location/Qibla/widgets PR #4, alerts/Focus PR #3,
calculation/tracking PR #2 and foundation PR #1. Nothing is merged or published.
The full approved feature scope is implemented. Do not restart the interview or stop
at the foundation milestone. `docs/PROGRESS.md` is the evidence authority.

Latest code `71a7c4b` fixes Focus transitions and Fajr system/keyboard insets after
actual CI/capture inspection; complete matrix reruns are in progress. Previous
`7f5df5f` passed all Android/API26/31/33/37, 672 captures, optimized performance and
unsigned reproducibility. `839b4d8` added an actual MediaPlayer/audio-focus/priority
fixture that passed four APIs, but found Focus transition races on API31/37. Do not
call that run fully green. The current rerun must verify the fixes and actual captures.

Real generated profiles and raw emulator measurements are committed. Clean independent
unsigned builds are byte-identical; exact source/hash evidence is in PROGRESS.md and
RELEASE.md. Release review kits, four-language store drafts, private security reporting
and a disabled F-Droid recipe are prepared. No production signing or submission.

Temporary JDK21, Gradle9.6.0 and AndroidSDK37 are in `/tmp/hayya-toolchain`; set
JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME accordingly. Do not rely on /tmp surviving.
No KVM exists here; use GitHub CI emulators. Avoid overlapping local Gradle builds.
`.local-qa/` holds an ignored private QA key; never commit it. Disk space is limited;
remove only task-owned reproducible caches/downloads when necessary.

Finish remaining CI failures/capture inspection/documentation autonomously. Physical
S23/mid-range/overnight/TalkBack/sensors/power and Arabic review are external release
gates, not coding blockers. Do not merge/publish without the agreed concrete review.

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
