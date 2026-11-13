// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.benchmarktests

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MemoryCaptureTest {
    @Test fun settledTodayWithThreeYearsOfExplicitRecords() {
        seed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("am start -W -n $TARGET/org.hayya.salah.MainActivity")
        ready(); device.waitForIdle(); Thread.sleep(6000)
        val directory = File(checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")),"memory").apply { mkdirs() }
        fun capture(command: String, name: String) {
            val text = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
            check(text.isNotBlank()); directory.resolve(name).writeText(text)
        }
        capture("dumpsys meminfo $TARGET","today-three-years.txt")
        capture("dumpsys package $TARGET","benchmark-package.txt")
        capture("dumpsys alarm","alarm-state.txt")
    }
}
