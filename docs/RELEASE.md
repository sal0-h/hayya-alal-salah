# Release preparation — review packet, not publication

No public release or store submission is authorized by this file. All code milestones
are internal. Remaining device/reviewer gates are listed in PROGRESS.md. Release
version/tag, owner contact, production certificate and public policy URL must be
filled from reviewed decisions; do not invent them or use the QA key.

## Build and reproducibility

Use the pinned toolchain, committed dependency locks/verification and committed actual
generated profiles. `./gradlew :app:assembleRelease :app:bundleRelease :app:lintRelease`
produces unsigned production artifacts. `scripts/check_release.py` checks the actual
35 MiB APK budget, packaged license bytes, reviewed AndroidX native helpers/64-bit ELF 16 KiB alignment, lack of synthetic components/profiling and
unsigned identity. `scripts/check_manifest.py` verifies privacy/backup declarations.

The reproducibility workflow builds the same source on two independent clean workers
under different checkout paths with the build cache disabled, then requires identical
unsigned APK bytes and retains SHA256SUMS. This is a reproducibility test, not an
independent supply-chain audit or F-Droid acceptance. Preserve the exact source commit,
profiles, toolchain, checksum reports, R8 mapping and build outputs with a release.

## Signing and update compatibility

Production ID is `org.hayya.salah`; QA `.qa`, debug `.dev` and benchmark `.benchmark`
are intentionally incompatible installations. Keep the QA key stable for internal
updates and retain a private backup. Production signing must be prepared in a separate
reviewed environment; no production key is created by these scripts.

Before the first store release, decide a single production signing lineage. Android
requires compatible certificates and monotonically increasing version codes for
in-place updates. A Play-generated app-signing key that is unavailable for direct
signing can prevent cross-store updates. Review Play App Signing enrollment and key
custody before uploading the first AAB. Losing/changing identity risks local records
because export is deferred. Never advise uninstalling as an ordinary update step.

Aim to publish the same upstream-signed APK on GitHub and F-Droid, with F-Droid
rebuilding and verifying the signature against the upstream binary. Their documented
`Binaries`/signing-key configuration must use the reviewed release URL/certificate.
The disabled recipe in release/fdroid is a preparation artifact, not an accepted
submission. See https://f-droid.org/docs/Reproducible_Builds/ and
https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/ .

## Concrete submission materials

`fastlane/metadata/android/` contains four-language title, short description and full
description drafts. Require Salman’s EN/RU/AZ review and an Arabic reviewer before
using them publicly. Use real approved captures from the final signed build; exclude
synthetic fixture components, system error dialogs and private user data. Store
feature artwork must be original and separately reviewed.

Publish PRIVACY.md at a stable public URL and establish a verified private contact.
For Play, prepare Data safety from the final binary: no app collection/sharing;
location, records, audio and package selections are processed locally. The developer
must review the form against current definitions, dependencies and final behavior.
No server encryption/deletion/account claims apply because there is no service/account.

Prayer Focus is not a disability tool: keep `isAccessibilityTool=false`, include its
use in the listing, demonstrate the in-app disclosure, affirmative consent, narrow
package/window access, selected-app overlay, essential exclusions and emergency exit.
Prepare the AccessibilityService permission declaration and a synthetic demonstration
video; do not claim prior Play approval. Policy source checked 2026-11-13:
https://support.google.com/googleplay/android-developer/answer/10964491 .

Explain Fajr wake-up as the user-enabled alarm use case for exact access, full-screen
intent and media playback foreground service. Include notification fallback when
full-screen access is denied and respect DND/channels. Complete current Play permission
and FGS declarations with a video from an actual accepted build.

## Review gate

Require green correctness/privacy/license/reproducibility jobs, reviewed calculation
comparisons, both QA update tests, full-route large-text/RTL evidence, Arabic review,
S23 plus physical mid-range/overnight/API-boundary reliability, TalkBack/sensors/audio,
performance/power budgets and no unresolved calculation/data-loss defects. Keep each
evidence row honest. Only then prepare a concrete release approval request with
commit/tag, artifact hashes, certificate, changelog, store text and unresolved risks.
No script in this repository uploads to a store, signs production or merges PRs.

## Executed unsigned reproducibility evidence

Source `957230dd55ce109dde05f69416c7c2b7c348fe38` passed both clean release workers,
release lint, privacy/license/native-alignment/profile/budget checks and exact-byte
comparison in [run 43509300940](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43509300940).
Both APKs were 11,042,687 bytes (10.5 MiB), SHA-256
`4106456083b7083ce6ec645f9356afcedb4cb8033614a7b1c944f8c3128a77f8`.
The source/review kit and hashes are retained as commit-labeled Actions artifacts.
This unsigned internal build is not a reviewed public release or production certificate.
Later source changes require their own matching evidence.
