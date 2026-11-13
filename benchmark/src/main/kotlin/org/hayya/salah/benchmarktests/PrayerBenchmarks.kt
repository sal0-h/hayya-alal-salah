// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.benchmarktests
import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrayerBenchmarks {
    @get:Rule val benchmark = MacrobenchmarkRule()
    @Before fun prepare() { seed() }
    private fun startup(mode: StartupMode, compilation: CompilationMode) = benchmark.measureRepeated(
        packageName = TARGET,metrics = listOf(StartupTimingMetric()),iterations = 5,startupMode = mode,compilationMode = compilation,
        setupBlock = { pressHome() }) { startActivityAndWait(); ready() }
    @Test fun coldNoCompilation() = startup(StartupMode.COLD,CompilationMode.None())
    @Test fun warmNoCompilation() = startup(StartupMode.WARM,CompilationMode.None())
    @Test fun coldBaseline() = startup(StartupMode.COLD,CompilationMode.Partial(BaselineProfileMode.Require))
    @Test fun recordedHistoryNavigation() = benchmark.measureRepeated(packageName = TARGET,
        metrics = listOf(FrameTimingMetric()),iterations = 5,compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        setupBlock = { pressHome(); startActivityAndWait(); ready() }) { journey() }
}
