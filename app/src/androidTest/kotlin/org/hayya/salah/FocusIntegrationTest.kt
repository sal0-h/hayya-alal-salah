// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.UiAutomation
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.focus.EssentialApps
import org.hayya.salah.focus.PrayerFocusService
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class FocusIntegrationTest {
    @Test(timeout = 180_000) fun actualOverlayReleasesOnCompletionPauseBypassEssentialAppAndPermissionRemoval() = runBlocking {
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
        val component = quote(context.packageName + "/org.hayya.salah.focus.PrayerFocusService")
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
        fun enableService() {
            shell("settings put secure enabled_accessibility_services $component")
            shell("settings put secure accessibility_enabled 1")
        }
        suspend fun sleepScreen() {
            shell("input keyevent KEYCODE_SLEEP")
            withTimeout(5_000) {
                while (context.getSystemService(PowerManager::class.java).isInteractive) delay(50)
            }
            overlay(false)
        }
        suspend fun wake() {
            shell("input keyevent KEYCODE_WAKEUP")
            shell("wm dismiss-keyguard")
            withTimeout(5_000) {
                while (!context.getSystemService(PowerManager::class.java).isInteractive ||
                    context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) delay(50)
            }
        }
        var phase = "connect"
        try {
            app.settings.update { AppPreferences(place,focus = FocusPreferences(true,setOf(targetPackage))) }
            enableService()
            withTimeout(15000) { PrayerFocusService.connected.first { it } }
            phase = "initial overlay"; launchTarget(); overlay(true)
            phase = "unselected target"
            app.settings.update { it.copy(focus = it.focus.copy(packages = emptySet())) }; overlay(false)
            app.settings.update { it.copy(focus = it.focus.copy(packages = setOf(targetPackage))) }; overlay(true)
            phase = "completion"; app.repository.record(window.target,PrayerStatus.ON_TIME,place.zone); overlay(false)
            phase = "restore unrecorded"; app.repository.record(window.target,null,place.zone); overlay(true)
            phase = "pause"; app.repository.addPause(pause); overlay(false)
            phase = "remove pause"; app.repository.deletePause(pause); overlay(true)
            fun find(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
                if (node == null || node.text?.toString() == text) return node
                for (index in 0 until node.childCount) find(node.getChild(index),text)?.let { return it }
                return null
            }
            phase = "emergency bypass"
            val bypass = withTimeout(5000) {
                var button: AccessibilityNodeInfo? = null
                while (button == null) {
                    val root = automation.windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }?.root
                    button = find(root,context.getString(R.string.emergency_bypass))
                    if (button == null) delay(100)
                }
                button
            }
            assertTrue(checkNotNull(bypass).performAction(AccessibilityNodeInfo.ACTION_CLICK))
            overlay(false)
            assertNull(app.repository.snapshot().records[window.target])
            withTimeout(5000) { app.settings.preferences.first { it.focus.bypass?.target == window.target } }
            phase = "clear bypass"; app.settings.update { it.copy(focus = it.focus.copy(bypass = null)) }; overlay(true)
            phase = "own app exclusion"; shell("am start -W -n ${context.packageName}/org.hayya.salah.MainActivity"); overlay(false)
            phase = "settings exclusion"; shell("am start -W -a android.settings.SETTINGS"); overlay(false)
            phase = "rapid returns"
            repeat(3) {
                shell("am start -W -n ${context.packageName}/org.hayya.salah.MainActivity")
                launchTarget()
                shell("am start -W -a android.settings.SETTINGS")
                launchTarget()
                overlay(true)
            }
            phase = "launcher, dialer, and input method exclusions"
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val dial = Intent(Intent.ACTION_DIAL)
            fun resolve(intent: Intent) = checkNotNull(context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)).activityInfo.packageName
            val launcher = resolve(home)
            val dialer = resolve(dial)
            val keyboards = context.getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
            assertTrue(keyboards.isNotEmpty())
            val essential = setOf(launcher, dialer) + keyboards
            assertTrue(EssentialApps.excluded(context).containsAll(essential))
            // Exercise runtime exclusions even if an essential package was saved earlier.
            app.settings.update { it.copy(focus = it.focus.copy(packages = essential + targetPackage)) }
            shell("input keyevent KEYCODE_HOME"); overlay(false)
            launchTarget(); overlay(true)
            assertTrue(shell("am start -W -a android.intent.action.DIAL").contains("Status: ok"))
            overlay(false)
            launchTarget(); overlay(true)
            phase = "screen off/on during active window"
            sleepScreen()
            wake(); overlay(true)
            phase = "permission removal"; shell("settings delete secure enabled_accessibility_services")
            shell("settings put secure accessibility_enabled 0"); overlay(false)
            withTimeout(15_000) { PrayerFocusService.connected.first { !it } }
            phase = "service reconnection"
            shell("am start -W -a android.settings.SETTINGS")
            enableService()
            withTimeout(15_000) { PrayerFocusService.connected.first { it } }
            overlay(false)
            launchTarget(); overlay(true)

            phase = "expiry while screen is off"
            val current = Instant.now()
            val expires = current.plusSeconds(25).truncatedTo(ChronoUnit.MINUTES).plusSeconds(60)
            val shortPlace = placeNearAsr(expires)
            val recordsBeforeExpiry = app.repository.snapshot().records
            app.settings.update { it.copy(place = shortPlace, focus = FocusPreferences(true, setOf(targetPackage), setOf(Prayer.DHUHR))) }
            shell("am start -W -a android.settings.SETTINGS"); overlay(false)
            launchTarget(); overlay(true)
            sleepScreen()
            withTimeout(90_000) { while (Instant.now() <= expires.plusSeconds(1)) delay(200) }
            wake(); overlay(false)
            delay(600); overlay(false)
            assertEquals(targetPackage, automation.rootInActiveWindow?.packageName?.toString())
            assertEquals(recordsBeforeExpiry, app.repository.snapshot().records)

            if (Build.VERSION.SDK_INT >= 31) {
                phase = "system clock and zone recovery"
                val originalZone = java.util.TimeZone.getDefault().id
                val alternateZone = if (originalZone == "Asia/Baku") "Europe/Paris" else "Asia/Baku"
                val wallTime = Instant.now()
                val elapsed = SystemClock.elapsedRealtime()
                val automaticTime = shell("settings get global auto_time")
                val automaticZone = shell("settings get global auto_time_zone")
                fun restore(setting: String, value: String) {
                    shell(if (value == "null") "settings delete global $setting" else "settings put global $setting ${quote(value)}")
                }
                try {
                    shell("settings put global auto_time 0")
                    shell("settings put global auto_time_zone 0")
                    val before = expires.minusSeconds(20)
                    assertEquals("", shell("cmd alarm set-time ${before.toEpochMilli()}"))
                    assertTrue(Instant.now() in before..before.plusSeconds(5))
                    overlay(true)
                    assertEquals("", shell("cmd alarm set-timezone $alternateZone"))
                    withTimeout(5_000) { while (java.util.TimeZone.getDefault().id != alternateZone) delay(100) }
                    overlay(true)
                    val after = expires.plusSeconds(2)
                    assertEquals("", shell("cmd alarm set-time ${after.toEpochMilli()}"))
                    assertTrue(Instant.now() in after..after.plusSeconds(5))
                    overlay(false)
                } finally {
                    shell("cmd alarm set-time ${wallTime.plusMillis(SystemClock.elapsedRealtime() - elapsed).toEpochMilli()}")
                    shell("cmd alarm set-timezone $originalZone")
                    restore("auto_time", automaticTime)
                    restore("auto_time_zone", automaticZone)
                }
            }
        } catch (error: Exception) {
            val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { java.io.File(it,"focus-diagnostics").apply { mkdirs() } }
            val state = runCatching {
                val type = org.hayya.salah.focus.PrayerFocusService::class.java
                val service = (type.getDeclaredField("instance").apply { isAccessible = true }.get(null) as? java.lang.ref.WeakReference<*>)?.get()
                fun field(name: String) = type.getDeclaredField(name).apply { isAccessible = true }.get(service)
                val prefs = field("config") as? AppPreferences
                val view = (field("ownWindow") as? java.lang.ref.WeakReference<*>)?.get() as? android.view.View
                "foreground=${field("foreground")}; configured=${prefs?.focus?.enabled}; selected=${prefs?.focus?.packages}; ownFocused=${view?.hasWindowFocus()}; overlay=${field("overlay") != null}; connected=${org.hayya.salah.focus.PrayerFocusService.connected.value}"
            }.getOrElse { "State unavailable: $it" }
            directory?.resolve("accessibility.txt")?.writeText("Phase: $phase\n$state\n" + shell("dumpsys accessibility"))
            throw AssertionError("Focus lifecycle failed during $phase",error)
        } finally {
            shell("input keyevent KEYCODE_WAKEUP")
            shell("wm dismiss-keyguard")
            app.repository.record(window.target,null,place.zone); app.repository.deletePause(pause)
            app.settings.update { original }
            shell(if (services == "null" || services.isBlank()) "settings delete secure enabled_accessibility_services" else "settings put secure enabled_accessibility_services ${quote(services)}")
            shell(if (enabled == "null") "settings delete secure accessibility_enabled" else "settings put secure accessibility_enabled ${quote(enabled)}")
            shell("am start -W -n ${context.packageName}/org.hayya.salah.MainActivity")
        }
    }

    private fun placeNearAsr(expires: Instant): ManualSetup {
        val zone = ZoneId.of("UTC")
        val expiryDate = expires.atZone(zone).toLocalDate()
        val calculator = LocalPrayerCalculator()
        return (-180..180 step 20).firstNotNullOf { longitude ->
            val point = GeoPoint(40.0, longitude.toDouble())
            val place = ManualSetup("Synthetic expiry fixture", point, zone, Profile.MWL)
            listOf(expiryDate, expiryDate.minusDays(1)).firstNotNullOfOrNull { date ->
                val schedule = (calculator.calculate(date, point, zone, place.calculation) as? CalculationResult.Available)?.schedule
                    ?: return@firstNotNullOfOrNull null
                val correction = Duration.between(schedule.prayers.getValue(Prayer.ASR), expires).toMinutes()
                if (correction !in -60..60) return@firstNotNullOfOrNull null
                val adjusted = place.copy(calculation = place.calculation.copy(corrections = mapOf(Prayer.ASR to correction.toInt())))
                val verified = (calculator.calculate(date, point, zone, adjusted.calculation) as? CalculationResult.Available)?.schedule
                    ?: return@firstNotNullOfOrNull null
                adjusted.takeIf { verified.prayers.getValue(Prayer.ASR) == expires && verified.prayers.getValue(Prayer.DHUHR) < expires.minusSeconds(90) }
            }
        }
    }
}
