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

This is a times-only preview: no prayer records, alarm delivery or app blocking yet.
Use manual decimal coordinates and an IANA time zone. Each method is chosen explicitly;
regional labels are not promises of exact published-timetable agreement.

For each physical report record commit, app version, exact device/Android/One UI build,
reproduction steps, expected/actual result and whether an update preserved saved setup.
Do not attach private coordinates/history or unreviewed logs. Do not uninstall between
update tests. The planned two-update record-preservation test awaits the Room slice.

CI screenshot fixtures contain only a fixed Baku example and date, never saved user
settings. `screenshots-<commit>` contains EN/RU/AZ/AR at 100%/200%, light/dark. Capturing
screenshots is not a claim that accessibility or design review passed.

Production signing, cross-store update compatibility, reproducible/F-Droid release
checks and public store publication remain separate release work.
