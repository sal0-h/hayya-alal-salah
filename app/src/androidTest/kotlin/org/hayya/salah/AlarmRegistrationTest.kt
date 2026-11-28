// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
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
            // Android 8 prints a separate window/flags line. Later releases put
            // both fields on the type line.
            val result = alarm.lineSequence().take(14).firstNotNullOfOrNull {
                Regex("window=\\S+ .*flags=0x([0-9a-fA-F]+)").find(it)
            }
            assertNotNull("Unrecognized alarm details:\n$alarm", result)
            return checkNotNull(result).groupValues[1].toInt(16)
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
                try {
                    AlarmCoordinator.install(context, entry)
                    val registration = registrations("DELIVER").single()
                    val bits = flags(registration)
                    if (kind == EventKind.AT_TIME) {
                        assertTrue("Primary prayer has no idle delivery: $registration", bits and (quotaFlags or unrestricted) != 0)
                    } else {
                        assertEquals("$kind requests idle quota: $registration", 0, bits and quotaFlags)
                    }
                } finally {
                    // Match the tested operation without modifying the app's live snapshot.
                    val intent = Intent(context, AlarmReceiver::class.java).setAction("org.hayya.salah.DELIVER")
                        .setData("hayya://alarm/${entry.id}".toUri())
                    PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                        context.getSystemService(AlarmManager::class.java).cancel(it)
                        it.cancel()
                    }
                }
            }
            val place = ManualSetup("Synthetic registration fixture", GeoPoint(40.0, 50.0), ZoneId.of("UTC"), Profile.MWL)
            val fixedTime = LocalTime.now(ZoneOffset.UTC).plusHours(2).withSecond(0).withNano(0)
            app.settings.update { AppPreferences(place, fajr = FajrPreferences(enabled = true, mode = FajrMode.FIXED, fixedTime = fixedTime)) }
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
