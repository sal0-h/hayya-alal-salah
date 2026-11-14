# Implementation and validation ledger

Updated 2026-11-13. Source implementation, automated execution, visual inspection and
physical acceptance are separate. The full approved v1 scope is implemented in the
stacked draft PRs; these are review checkpoints, not merged or published releases.
Final Focus/audio-session and keyboard-resize verification is running at `250d740`.

## Implemented scope and executed evidence

| Area | Implemented | Automated evidence | Physical acceptance |
| --- | --- | --- | --- |
| Foundation | Native Compose/API26–37, four modules, pinned tools/engines/dependencies, separate stable QA signing/ID | Clean builds, lint, manifest/privacy checks and signed QA artifacts | Not run; two successive QA updates remain a gate |
| Calculation | 15 profiles/two reused engines, both Asr rules, advanced controls/corrections, high latitude, confirmed nearest-valid polar policy/manual reference/disabled, provenance, rollover/Ramadan | 21 core tests plus preserved upstream fixture; six seasonal provider references/36 times differ by at most one minute | No local religious-authority signoff |
| Tracking | Room records/pauses/audio metadata, explicit correction/backfill/deletion, original date/zone, calendar, daily/per-prayer streaks, coverage/insights | Persistence/reopen/concurrency/preferences, UI correction, actual original-target actions and full private-data deletion fixtures | No physical update/process-kill QA |
| Navigation/design | Today/History/Settings, all setup/settings/dialog routes, original geometric marker, bundled fonts, four languages/RTL, time/numerals/themes/palettes/reduced motion | Full-route/dialog capture matrix, navigation/labeled-control checks, real 100%/200% system fonts and narrow/tablet widths | TalkBack, Arabic and final design review pending |
| Alerts/Fajr | Seven-day stable alarms, exact/inexact readiness, best-effort extras, stale/duplicate claims, lifecycle reconciliation, authenticated original-target actions, challenge/emergency stop | Actual idle-capable AlarmManager delivery on APIs26/31/33/37; locked-reboot API37 probe verified active Fajr/bundled audio before unlock and preservation afterward | S23/OEM/overnight/Doze timing unverified |
| Audio | Three redistributable CC BY recordings, per-prayer/Fajr choices, private bounded validated imports/removal, finite foreground playback, ringer/DND guards, Fajr priority | Actual media preparation/import/corrupt rejection and MediaPlayer/audio-focus pause/regain/priority/emergency stop on all four APIs | Audible/calls/headphones/DND behavior unverified |
| Focus | Disclosure/consent, selected apps/prayers, metadata-only accessibility, essential exclusions, completion/pause/bypass/expiry, pinned emergency control | Real overlay lifecycle/permission removal passed four APIs at `7f5df5f`; later transitions fixed; audio/accessibility session isolation rerun pending | OEM/TalkBack and Play policy review pending |
| Location | 34,215 offline cities/all native aliases, coordinates/IANA zones, bounded visible foreground requests/cache, confirmed zone changes and approximate border suggestion | Four-script city/zone fixtures and packaged privacy/resource checks | GPS/travel/border review pending |
| Qibla/Hijri | Actual-coordinate bearing, visible-only sensors/true-north correction, quality/numeric fallback, named Umm al-Qura display with independent correction | Core bearing/Hijri fixtures; Android lint and UI captures | Sensor/calibration and Arabic review pending |
| Widgets | Compact next/countdown and resizable scrollable six-row daily RemoteViews, native countdown/boundary updates, no check-off/polling, pre-unlock/reset placeholder | Actual host bind/render/resize/scroll/clear on four APIs; 32 four-language/font/theme captures | Launcher/reboot/device QA pending |
| Performance/release | Three-year synthetic history, real generated profiles, optimized benchmarks, unsigned reproducibility, privacy/licenses/16 KiB/budget checks, review kits/store drafts/disabled F-Droid recipe | Two profile-generation and five benchmark/memory tests; independent exact-byte release builds; details below | Physical mid-range/power budgets and release/signing/store review pending |

## Current runs and retained evidence

The entire `7f5df5f` push passed [Android checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627420),
[performance](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627620) and
[reproducibility](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43511627558).
Its ordinary suite executed 13 tests on each API26/31/33/37, plus one isolated denied-
permission test per API and the API37 locked-reboot preparation/preservation checks.
No empty/incomplete report is accepted. Reinstall/reset is restricted to isolated
synthetic emulator fixtures; ordinary QA updates must preserve data.

At `839b4d8`, the new actual audio-focus test passed all four APIs, bringing each
ordinary suite to 14 tests. [Android run 43514118846](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118846)
passed API26/33, build, QA signing and all 672 captures. API31 failed the initial Focus
overlay transition; API37 failed returning from the own-app exclusion. These failures
are not called green. `2303e42` removes window-event coalescing, rejects stale own-window
metadata and preserves external transitions arriving before focus loss; PR #6 also
contains that fix. `71a7c4b` reruns the complete matrix with stricter actual IME/control-
bounds assertions. Its API26/37 tests and locked reboot passed; API31 still missed
the initial overlay after the new audio fixture. `8a2708b` isolates audio and Focus
in separate instrumentation sessions (13 ordinary tests plus one real audio test per
API) and adds synthetic failure-state diagnostics. `250d740` also explicitly resizes
keyboard windows and checks the scrolling viewport against the actual status bar.

The same `839b4d8` passed [all five performance checks](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118876)
and [two independent release builds](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43514118906).
Both unsigned APKs were 11,043,311 bytes, SHA-256
`053741d56ded522b9575a5ef7a29c74003c4a574c61de4494475e7fc9a4fe6ac`.
Later source changes require their own matching evidence.

Real profile generation and all five optimized benchmarks passed at `d11e566`,
[run 43504979193](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43504979193).
Committed actual profiles contain 18,903 baseline and 15,718 startup rules; fixture
rules are excluded and the binary profile is checked in the optimized APK. Settled
Today PSS was 72.1 MiB with three years of synthetic records. Emulator startup did
not improve with the profile; no speedup or physical budget acceptance is claimed.
Raw measurements/provenance/hashes are in PERFORMANCE.md and evidence/performance-d11.json.

Frozen calculation comparisons, exact methods/coordinates, checksums and limitations
are in CALCULATION_FIXTURES.md. Published AlAdhan calculations may share upstream
ancestry; they are not independent local religious-authority approval. No new astronomy.

Earlier clean milestone evidence remains available: [foundation](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43345790379),
[tracking](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43389685112),
[alerts](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43394277151),
[location](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43395225120) and
[API hardening](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43506421797).
Actions artifacts expire; preserve reviewed evidence with an approved release.

## Actual visual inspection

The matrix totals 672 captures: 592 full-shell/routes/dialogs across four languages,
themes, real 100%/200% fonts and phone/tablet widths; 48 native Focus/Fajr/keyboard
captures; and 32 actual widget-host captures. Representative EN/AR normal/200% and
RU/AZ large-text artifacts were opened and inspected, not merely counted.

Inspection found and drove fixes for large-font calendar widths, compact widget
countdown clipping, asynchronous widget resize/scroll races and Fajr keyboard/body
insets. Current compact Arabic widget time/countdown is readable; daily content scrolls
to all six rows. The `839b4d8` Fajr answer was visible above the keyboard, but scrolled
text could paint under the status bar. The `71a7c4b` captures passed automated field bounds but visual inspection still
rejected the status-bar overlap. `12b5be9` sets explicit `adjustResize` and clips the
viewport; `250d740` checks its actual boundary and the pinned emergency control.
These newer captures still need execution and visual review.
Earlier captures obscured by a system error dialog were rejected, not approved.
Screenshot generation does not establish fluent Arabic review or physical TalkBack use.

## Privacy and release preparation

Runtime network permission, broad storage/app-usage/screen access, background location,
backup/transfer and exported-component protections are automatically checked. Full reset
and imported-audio deletion exercise actual private-file removal and error reporting.
Direct Boot keeps only upcoming event/sound/challenge/language/appearance data, never
coordinates/history/pause dates. Synthetic probes are absent from QA/release.

Dedicated QA credentials remain private and separate from production. No production
signature, merged PR, public release or store submission exists. Source/unsigned APK/
bundle/R8 mapping/hashes and four-language store drafts form a concrete review kit;
the F-Droid recipe stays disabled until a reviewed release tag/certificate is supplied.
Private vulnerability reporting was verified enabled on 2026-11-13; SECURITY.md links
the private route. Public privacy hosting and store declarations await release review.
Dependency checksums are first-resolution trust, not an independent supply-chain audit.

## Remaining work and external gates

Server work: finish the current Focus/inset CI reruns, inspect their actual captures,
update final evidence and draft PR descriptions, and leave clean reviewable commits.
Local compilation/lint and privacy checks passed after the inset changes. There is
no observed usage-limit blocker. This server lacks KVM; GitHub CI provides
emulators. The interrupted local build is tracked separately and is not delivery proof.

No physical tests have run. Salman owns S23 Ultra QA and must record the exact Android/
One UI build. Arabic review, a physical mid-range tester, two QA update-preservation
tests, overnight alarms/Doze/OEM/Direct Boot/calls/headphones/sensors/TalkBack and physical
startup/frame/memory/power budgets remain public-release gates. They do not authorize
publication or excuse leaving independent implementation unfinished.

## Resumed verification, 2026-11-14

The prior automatic approval review stopped a push because of a usage limit. That
limit cleared on resumption; source `5bff9bb` is now pushed. Android run 43553892997,
performance 43553892993 and reproducibility 43553893029 are running; no green claim yet.
The source includes the isolated audio-fixture reinstall, emergency-bypass persistence
handoff refresh and useful-display dirty traversal. Local compilation/lint passed.
Actual `250d740` Arabic/English 200% keyboard captures were inspected: scrolling content
stays below the status bar and answer/emergency stop remain usable above the keyboard.
The earlier performance draft's pinned Kotlin artifact download failure is being retried
without changing versions/checksums. Physical acceptance remains not run.
