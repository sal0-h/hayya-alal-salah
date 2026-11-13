# Contributing

Read AGENTS.md, HANDOFF.md and docs/IMPLEMENTATION_PLAN.md. The approved scope is
native Kotlin/Compose, an Android-free JVM core, offline calculations, explicit prayer
records and four-language accessibility. Avoid new frameworks without demonstrated need.

Start a small branch and PR with the concrete behavior, validation and limitations.
Run the checks in README.md. Test astronomical edge cases and record semantics when
changing core rules; never replace unknown records with assumed misses. Keep displayed
and scheduled instants identical. Include provenance and license notices for assets.
Review dependency lock/checksum changes; do not regenerate trust metadata merely to
silence a checksum mismatch. First-resolution checksum metadata is not an independent
security audit of upstream artifacts.

UI changes need actual normal/large-text screenshots in both directions, light/dark,
and human review. Synthetic CI captures may be shared; private user data may not.
Device reports must identify commit, device and exact OS build. Compilation/emulators
do not establish Doze, OEM reliability, overnight alarms or battery performance.

Never commit keys, local.properties, private location/history, imported audio or
production signing credentials. QA signing is separate from production and fork PRs.
Original contributions are GPL-3.0-or-later. Third-party licenses remain applicable.
