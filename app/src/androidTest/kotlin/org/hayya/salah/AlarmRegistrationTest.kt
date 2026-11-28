// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class AlarmRegistrationTest {
    @Test fun onlyPrimaryAlertsRequestIdleDelivery() = runBlocking {
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.appContainer
        val original = app.settings.preferences.first()
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }

        // AOSP Android 8 uses bit 2 for both public idle APIs. Newer versions
        // also use bit 5 for the compatibility idle path. Bit 3 is an OS exemption.
        val quotaFlags = (1 shl 2) or (1 shl 5)
        val unrestricted = 1 shl 3
        val alarmHeader = Regex("(?m)^\\s*(?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED|ELAPSED_REALTIME_WAKEUP|ELAPSED_REALTIME) #\\d+:.*$")
        fun registrations(action: String): List<String> {
            val dump = shell("dumpsys alarm")
            val headers = alarmHeader.findAll(dump).toList()
            val alarms = headers.mapIndexed { index, header ->
                dump.substring(header.range.first, headers.getOrNull(index + 1)?.range?.first ?: dump.length)
            }.filter { alarm ->
                alarm.lineSequence().take(14).any { it.trim() == "tag=*walarm*:org.hayya.salah.$action" } &&
                    alarm.lineSequence().take(14).any { it.trimStart().startsWith("operation=PendingIntent{") && context.packageName in it }
            }
            assertTrue("Missing $action registration in API ${Build.VERSION.SDK_INT}:\n" +
                dump.lineSequence().filter { it.contains("org.hayya.salah") }.take(45).joinToString("\n"), alarms.isNotEmpty())
            return alarms
        }

        fun flags(alarm: String): Int {
            val details = alarm.lineSequence().firstOrNull { it.trimStart().startsWith("type=") }
            assertNotNull("Unrecognized alarm details:\n$alarm", details)
            val result = Regex("flags=0x([0-9a-fA-F]+)").find(checkNotNull(details))
            // API 26 omits the flags field when the value is zero.
            if (Build.VERSION.SDK_INT != 26) assertNotNull("Missing alarm flags in: $details", result)
            return result?.groupValues?.get(1)?.toInt(16) ?: 0
        }
        try {
            app.settings.update { AppPreferences() }
            AlarmCoordinator.reconcile(context)
            if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
            if (Build.VERSION.SDK_INT >= 31) shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
            assertTrue(AlarmCoordinator.exactAccess(context))
            val target = PrayerKey(LocalDate.now(ZoneOffset.UTC), Prayer.FAJR)
            val at = Instant.now().plusSeconds(1_800)
            for (kind in listOf(EventKind.AT_TIME, EventKind.BEFORE, EventKind.AFTER)) {
                val entry = AlarmEntry("registration-$kind", target, kind, at, "UTC", null, ChallengeDifficulty.OFF)
                AlarmSnapshot(context).replace(listOf(entry))
                AlarmCoordinator.install(context, entry)
                val registration = registrations("DELIVER").single()
                val bits = flags(registration)
                if (kind == EventKind.AT_TIME) {
                    assertTrue("Primary prayer has no idle delivery: $registration", bits and (quotaFlags or unrestricted) != 0)
                } else {
                    assertEquals("$kind requests idle quota: $registration", 0, bits and quotaFlags)
                }
                AlarmCoordinator.cancelAll(context)
            }
            val place = ManualSetup("Synthetic registration fixture", GeoPoint(40.0, 50.0), ZoneId.of("UTC"), Profile.MWL)
            app.settings.update { AppPreferences(place, fajr = FajrPreferences(enabled = true, mode = FajrMode.FIXED)) }
            AlarmCoordinator.reconcile(context)
            assertTrue(registrations("DELIVER").all { alarm -> alarm.lineSequence().take(14).any { it.trim() == "Alarm clock:" } })
            val maintenance = registrations("MAINTAIN").single()
            assertEquals("Maintenance requests idle quota: $maintenance", 0, flags(maintenance) and quotaFlags)
        } finally {
            app.settings.update { original }
            AlarmCoordinator.cancelAll(context)
            AlarmCoordinator.reconcile(context)
        }
    }
}
