# Dukhan handoff

## Read first

1. `docs/IMPLEMENTATION_PLAN.md` is the approved specification.
2. `docs/RESEARCH.md` records sources, evidence limits and library provenance.
3. `AGENTS.md` gives concise working rules.

Salman approved implementation on 2026-11-12. Do not restart the product interview. The requested change is to perform development on Dukhan and test APKs locally on his Samsung S23 Ultra, using GitHub for code, CI artifacts and feedback.

## Actual implementation state

Salman approved source consolidation of PRs #1–#8 on 2026-11-26. The
reviewed stack is merged into `main` at `e4cb6ee`, including the Option 05
launcher icon. Its file tree matches the final PR #8 source. Base new feature
PRs on `main`; the original feature branch refs are retained. See [the verification
ledger](docs/PROGRESS.md) for implemented, automated and physical results.

[Signed QA build 141](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45869770771/artifacts/15142447273)
comes from this consolidated commit. Its checksum, package, version and established
QA signing certificate verified. The post-merge Android run passed all four APIs
after an isolated Android 8 retry for a Kotlin plugin download failure. Screenshots,
performance and reproducibility passed. See the ledger for the exact evidence.

The original server implementation and verification baseline is source `5bff9bb`. Android
run43553892997 passed all four APIs (13 ordinary + one separate audio test each),
672 captures, denial/locked-reboot preservation, lint/privacy/build and QA signing.
Performance43553892993 and reproducibility43553893029 passed. Downloaded reports and
actual large-text keyboard images were reviewed. PROGRESS.md has exact evidence.
The S23 follow-up source is `8b2eda7` from PR #7. The
[signed QA build 94](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45205128442/artifacts/14833472002)
is the in-place update from build 74. See PROGRESS.md for its verification and
DEVICE_QA.md for the S23 spot checks. Salman confirmed record, theme and import
preservation from 48 → 56 → 74. ADB confirmed both widget bindings from 56 → 74.
Salman then reviewed build 94. The follow-up at `8798816` adds Today date
browsing and a city shortcut, one explicit record editor, night-time options,
simpler reminders, fixed or relative Fajr timing and a clearer Pause screen.
The next follow-up at `f9540dc` adds horizontal Today date browsing and lets
Appearance show either or both night times.
[Signed QA build 108](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45856475811/artifacts/15136427874) validated the swipe and night-time changes. The Option 05 icon and those
changes are in [signed QA build 117](https://github.com/sal0-h/hayya-alal-salah/actions/runs/45862654353/artifacts/15138784269).
See [the launcher icon notes](docs/branding/LAUNCHER_ICON.md) for previews and
[device QA](docs/DEVICE_QA.md) for the earlier spot checks.
Physical Qibla and overnight alarms, Arabic review, production signing and store
review remain open.

Real generated profiles and raw emulator measurements are committed. Clean independent
unsigned builds are byte-identical; exact source/hash evidence is in PROGRESS.md and
RELEASE.md. Release review kits, four-language store drafts, private security reporting
and a disabled F-Droid recipe are prepared. No production signing or submission.

Temporary JDK21, Gradle9.6.0 and AndroidSDK37 are in `/tmp/hayya-toolchain`; set
JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME accordingly. Do not rely on /tmp surviving.
No KVM exists here; use GitHub CI emulators. Avoid overlapping local Gradle builds.
`.local-qa/` holds an ignored private QA key; never commit it. Disk space is limited;
remove only task-owned reproducible caches/downloads when necessary.

Physical S23, mid-range, overnight, TalkBack, sensor and power checks and Arabic
review are the remaining release gates. Production signing and public release
publication require the agreed release review.

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
