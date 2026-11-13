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
SDK/JDK. First it executes real profile generation, merges actual H/S/P flags while
excluding fixture code, then benchmarks the optimized target and requires the binary
profile to be packaged. Commit generated baseline/startup profiles only after successful
execution and review; never manufacture a profile or claim one from compilation.

CI emulator results detect regressions and validate the harness. Suppressing its
EMULATOR error is explicit and does not qualify those results for the designated
physical mid-range budgets in IMPLEMENTATION_PLAN.md. Physical p95 startup/useful data,
frame overrun, <=120 MiB PSS and power/wakeup acceptance remain separate. No physical
performance or battery-percent claim has been made.
