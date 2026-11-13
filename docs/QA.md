# Internal QA delivery

GitHub Actions `Android checks` runs correctness tests, upstream fixtures, Android lint,
APK builds and merged-manifest privacy checks. The `android-<commit>` artifact contains
unsigned QA/debug builds and reports. Use `qa-<commit>` for a stable signed internal
installation. Fork PRs never receive signing secrets. The signer job runs on repository
pushes, receives only unsigned artifacts and does not check out or execute project code.

QA uses `org.hayya.salah.qa`, separate from production `org.hayya.salah` and development
`org.hayya.salah.dev`. A dedicated RSA key is stored in Actions secrets
`QA_KEYSTORE_BASE64` and `QA_KEYSTORE_PASSWORD`. An ignored, mode-restricted local copy
is in `.local-qa/`; preserve it privately before moving machines. Losing/changing the
key prevents updates of existing QA installs. No production key exists here.
`QA_VERSION_CODE` is the GitHub run number; local default is 1.

Download the commit-labeled signed APK, verify the artifact SHA-256, then:

```sh
adb install -r hayya-qa-<commit>.apk
```

This internal preview now includes records, alerts, Fajr, Focus, Qibla and widgets.
Implementation does not establish physical reliability; consult PROGRESS.md.
Use manual decimal coordinates and an IANA time zone. Each method is chosen explicitly;
regional labels are not promises of exact published-timetable agreement.

For each physical report record commit, app version, exact device/Android/One UI build,
reproduction steps, expected/actual result and whether an update preserved saved setup.
Do not attach private coordinates/history or unreviewed logs. Do not uninstall between
update tests. Two successive signed QA update tests must preserve records, pauses, settings and
imported audio without uninstalling. This physical gate is still pending.

CI screenshot fixtures contain only a fixed Baku example and date, never saved user
settings. `screenshots-<commit>` contains EN/RU/AZ/AR at 100%/200%, light/dark. Capturing
screenshots is not a claim that accessibility or design review passed.

Unsigned reproducibility, privacy/license/native-alignment checks and source/APK/AAB/
mapping/hash review kits run in separate CI. Production signing, cross-store certificate
choice, F-Droid acceptance and public store publication remain external release work.

QA certificate SHA-256 (public, not a secret):
`081b779ae9d4f9c1714f212bcb5936fdc239a9941d3c1658920cef1229d08d7f`.

Screenshot instrumentation writes to AGP-provided `additionalTestOutputDir`, which is
collected before app cleanup. CI requires the complete named full-route/large-text/width matrix with valid headers
and dimensions; a successful instrumentation exit alone does not satisfy capture QA.

Verified foundation artifacts (code commit `7d1a109`, 2026-11-13):
- [Signed QA APK + SHA256SUMS](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43345790379/artifacts/14001602656)
- [Sixteen screenshots and Android test reports](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43345790379/artifacts/14001338533)

These are expiring CI artifacts, not a public app release. Future trusted pushes produce
new commit-labeled artifacts using the same dedicated QA identity.

## Current automated artifacts

Use PROGRESS.md for the exact passing source/run, rather than assuming the newest
push is green. `integration-api-<api>-<commit>` contains JUnit XML, per-test logs,
widget-host captures and denial/locked-reboot probes. Ordinary tests run on APIs
26/31/33/37. `screenshots-<commit>` contains 672 full-route/dialog/native/widget
captures; inspect actual images, including the 200% RTL keyboard and compact widgets.
A complete file count does not prove usable typography or accessibility.

`profiles-and-emulator-metrics-<commit>` contains optimized Macrobenchmark reports,
traces and settled-memory diagnostics using synthetic three-year history. Release
workflows retain `release-reports-a/b-<commit>`, `rebuild-a/b-<commit>` and
`reproducibility-<commit>`. These use the production application ID but remain unsigned;
use the separate stable signed QA artifact on the test phone.
