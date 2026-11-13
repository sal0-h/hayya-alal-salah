// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.benchmarktests

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import android.os.ParcelFileDescriptor

const val TARGET = "org.hayya.salah.benchmark"
internal fun seed() {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val command = "am broadcast --receiver-foreground -n $TARGET/org.hayya.salah.fixtures.BenchmarkSeedReceiver"
    val output = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    check(output.contains("result=-1") && output.contains("seeded=")) { output }
}
internal fun ready() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    check(device.wait(Until.hasObject(By.res("prayer-data-ready")),10000))
}
internal fun journey() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun click(tag: String) { checkNotNull(device.wait(Until.findObject(By.res(tag)),5000)).click(); device.waitForIdle() }
    click("nav-history"); click("nav-settings"); click("settings-calculation")
    device.pressBack(); click("nav-today"); ready()
    repeat(3) {
        if (!device.hasObject(By.res("record-open-fajr"))) device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,20)
    }
    click("record-open-fajr"); click("record-on_time"); click("nav-history"); click("nav-today")
    ready()
}
