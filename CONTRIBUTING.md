# Contributing

Read AGENTS.md, HANDOFF.md and the approved implementation plan before changing scope.
Use small reviewable commits. Never equate a draft milestone with a completed v1.

Use the pinned JDK/Gradle/SDK and wrapper checksums in docs/TOOLCHAIN.md. Run core and
vendored upstream tests, relevant lint/build tasks and scripts/check_manifest.py.
Use meaningful domain tests for dates, unknown records, pause precedence, concurrency
and Android lifecycle behavior. Do not infer physical reliability from compilation.

Keep Android code outside core, reuse the pinned engines rather than writing astronomy,
and preserve the documented license/provenance for copied code, audio, fonts and data.
Review dependency locks and verification changes; never accept a checksum mismatch by
replacing a trusted value. No runtime network, backend, telemetry or new framework
without an explicit scope decision.

Production signing is outside ordinary development. Synthetic fixtures exist only in
debug or isolated benchmark variants. Do not run destructive emulator fixture scripts
on a real phone, commit credentials, or upload private user data.

UI changes need actual normal/200% screenshots in both directions, all four languages,
contrast/touch-target/semantics checks and deliberate typography. Arabic shaping and
TalkBack review require human/device evidence. Record test evidence and limitations
in docs/PROGRESS.md. Include the problem, resulting behavior and relevant validation
in PR descriptions. Do not merge/publish without the agreed review.
