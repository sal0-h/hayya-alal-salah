// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.benchmarktests
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()
    @Before fun prepare() { seed() }
    @Test fun startup() = profile.collect(packageName = TARGET,includeInStartupProfile = true) {
        pressHome(); startActivityAndWait(); ready()
    }
    @Test fun interactions() = profile.collect(packageName = TARGET) {
        pressHome(); startActivityAndWait(); ready(); journey()
    }
}
