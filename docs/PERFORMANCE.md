# Performance validation

The benchmark module uses AndroidX Macrobenchmark 1.5.0 with ProfileInstaller 1.4.1.
It is self-instrumenting in a separate process. `profilegen` is non-minified and
`performance` uses production R8/resource optimization; both target only the isolated
`org.hayya.salah.benchmark` package. Their shell-protected seed receiver and shell
profiling are absent from QA/release.

The seed creates three years of public Baku synthetic records with explicit statuses
and unknown gaps. It disables alerts/Focus; no personal records are read or uploaded.
Journeys cover useful Today data, History, calculation settings and explicit recording.
Macrobenchmark collects cold/warm startup and interaction frame metrics. A separate
settled-Today memory dump and alarm state are retained as diagnostic artifacts.

Run `bash scripts/performance_ci.sh` only on an isolated emulator with the pinned
SDK/JDK. When profiles are missing or `HAYYA_GENERATE_PROFILES=true`, it executes real
profile generation and merges actual H/S/P flags while excluding fixture code. Ordinary
pushes benchmark the committed profiles and require the binary profile to be packaged.
Manual workflow dispatch (after the workflow exists on the default branch) can regenerate;
a source-changing push tagged `[profiles]` also requests generation. Commit generated baseline/startup profiles only after successful
execution and review; never manufacture a profile or claim one from compilation.

CI emulator results detect regressions and validate the harness. Suppressing its
EMULATOR error is explicit and does not qualify those results for the designated
physical mid-range budgets in IMPLEMENTATION_PLAN.md. Physical p95 startup/useful data,
frame overrun, <=120 MiB PSS and power/wakeup acceptance remain separate. No physical
performance or battery-percent claim has been made.

## First executed emulator baseline

[Run 43504979193](https://github.com/sal0-h/hayya-alal-salah/actions/runs/43504979193),
source `d11e566`, API35 Pixel6 emulator: two generation tests and all five optimized
performance/memory tests passed. The generated binary Baseline Profile was packaged.
Committed human profiles contain 18,903 baseline and 15,718 startup rules, with synthetic
receiver rules filtered. Provenance, raw measurements and hashes are retained in
[evidence/performance-d11.json](evidence/performance-d11.json).

| Journey | Emulator median initial display | Emulator median useful/full display |
| --- | --- | --- |
| Cold, no compilation | 1,254 ms | 2,193 ms |
| Warm, no compilation | 326 ms | 1,811 ms |
| Cold, required Baseline Profile | 1,261 ms | 2,317 ms |

Settled Today PSS was 73,791 KiB (72.1 MiB) with three years of seeded records. These
five-iteration startup samples do not establish a physical p95. The profile did not
produce a startup improvement in this emulator run; no speedup is claimed. Frame
samples and variation are in the raw evidence. Emulator/software-rendering results
do not replace physical frame-overrun, startup, power or memory acceptance.

## Useful-display trace correctness

Repeated emulator runs exposed a missing RenderThread slice after `reportFullyDrawn`,
on different cold/warm startup journeys. This is not accepted as a passing sample.
The activity now requests one dirty traversal when useful-data readiness reports, so
that signal is followed by a RenderThread draw; metrics and budgets are unchanged.
This follows the same dirty-frame requirement documented in the
[AndroidX first-frame correction](https://android.git.googlesource.com/platform/frameworks/support/+/258dc5586df930667094d970975322d8e6f16b78).
Current rerun evidence is recorded in PROGRESS.md. No benchmark exception is suppressed
and failed captures are not substituted with successful-looking measurements.
