// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.UiAutomation
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class FocusIntegrationTest {
    @Test fun actualOverlayReleasesOnCompletionPauseBypassEssentialAppAndPermissionRemoval() = runBlocking {
        assumeTrue("OS-service fixture is isolated to emulators", Build.HARDWARE in setOf("ranchu","goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val targetPackage = instrumentation.context.packageName
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        fun quote(value: String): String {
            require(Regex("[A-Za-z0-9_./:-]+").matches(value))
            return value // UiAutomation executes argv directly; quotes would become part of the setting.
        }
        val services = shell("settings get secure enabled_accessibility_services")
        val enabled = shell("settings get secure accessibility_enabled")
        val app = context.appContainer
        val original = app.settings.preferences.first()
        val now = Instant.now()
        val date = now.atZone(ZoneOffset.UTC).toLocalDate()
        val choice = (-180..180 step 30).firstNotNullOf { longitude ->
            val place = ManualSetup("Synthetic Focus fixture",GeoPoint(40.0,longitude.toDouble()),ZoneId.of("UTC"),Profile.MWL)
            val schedules = (-1L..1L).mapNotNull { day -> (LocalPrayerCalculator().calculate(date.plusDays(day),place.point,place.zone,place.calculation) as? CalculationResult.Available)?.schedule }
            val window = focusWindow(schedules,Prayer.entries.toSet(),emptyMap(),emptyList(),now)
            if (window != null && window.target.prayer != Prayer.FAJR && window.expiresAt > now.plusSeconds(180)) place to window else null
        }
        val (place,window) = choice
        val pause = PauseInterval(window.target.date,window.target.date)
        assertFalse(app.repository.snapshot().records.containsKey(window.target))
        assertFalse(pause in app.repository.snapshot().pauses)
        suspend fun overlay(show: Boolean) {
            withTimeout(15000) {
                while (automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && it.root?.packageName?.toString() == context.packageName } != show) delay(100)
            }
        }
        fun launchTarget() { shell("am start -W -n $targetPackage/org.hayya.salah.FocusTargetActivity") }
        var phase = "connect"
        try {
            app.settings.update { AppPreferences(place,focus = FocusPreferences(true,setOf(targetPackage))) }
            shell("settings put secure enabled_accessibility_services ${quote(context.packageName + "/org.hayya.salah.focus.PrayerFocusService")}")
            shell("settings put secure accessibility_enabled 1")
            withTimeout(15000) { org.hayya.salah.focus.PrayerFocusService.connected.first { it } }
            phase = "initial overlay"; launchTarget(); overlay(true)
            phase = "completion"; app.repository.record(window.target,PrayerStatus.ON_TIME,place.zone); overlay(false)
            phase = "restore unrecorded"; app.repository.record(window.target,null,place.zone); overlay(true)
            phase = "pause"; app.repository.addPause(pause); overlay(false)
            phase = "remove pause"; app.repository.deletePause(pause); overlay(true)
            fun find(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
                if (node == null || node.text?.toString() == text) return node
                for (index in 0 until node.childCount) find(node.getChild(index),text)?.let { return it }
                return null
            }
            val root = automation.windows.first { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }.root
            checkNotNull(find(root,context.getString(R.string.emergency_bypass))).performAction(AccessibilityNodeInfo.ACTION_CLICK)
            overlay(false)
            assertNull(app.repository.snapshot().records[window.target])
            assertEquals(window.target,app.settings.preferences.first().focus.bypass?.target)
            app.settings.update { it.copy(focus = it.focus.copy(bypass = null)) }; overlay(true)
            shell("am start -W -n ${context.packageName}/org.hayya.salah.MainActivity"); overlay(false)
            launchTarget(); overlay(true)
            shell("am start -W -a android.settings.SETTINGS"); overlay(false)
            launchTarget(); overlay(true)
            shell("settings delete secure enabled_accessibility_services")
            shell("settings put secure accessibility_enabled 0"); overlay(false)
        } catch (error: Exception) {
            val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { java.io.File(it,"focus-diagnostics").apply { mkdirs() } }
            directory?.resolve("accessibility.txt")?.writeText("Phase: $phase\n" + shell("dumpsys accessibility"))
            throw AssertionError("Focus lifecycle failed during $phase",error)
        } finally {
            app.repository.record(window.target,null,place.zone); app.repository.deletePause(pause)
            app.settings.update { original }
            shell(if (services == "null" || services.isBlank()) "settings delete secure enabled_accessibility_services" else "settings put secure enabled_accessibility_services ${quote(services)}")
            shell(if (enabled == "null") "settings delete secure accessibility_enabled" else "settings put secure accessibility_enabled ${quote(enabled)}")
            shell("am start -W -n ${context.packageName}/org.hayya.salah.MainActivity")
        }
    }
}
