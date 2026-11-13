// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.time.temporal.ChronoUnit

/** Isolated synthetic dev installation: actual platform alarm, not a direct receiver call. */
@RunWith(AndroidJUnit4::class)
class AlarmDeliveryIntegrationTest {
    @Test fun actualIdleCapableAlarmDeliversOnceAndExplicitActionKeepsOriginalZone() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.appContainer
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
        shell("appops set ${context.packageName} POST_NOTIFICATION allow")
        if (Build.VERSION.SDK_INT >= 31) shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        val original = app.settings.preferences.first()
        val manager = context.getSystemService(NotificationManager::class.java)
        var inserted: PrayerKey? = null
        try {
            val current = Instant.now()
            val minute = current.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60)
            val targetAt = if (Duration.between(current,minute).seconds < 15) minute.plusSeconds(60) else minute
            val date = targetAt.atZone(ZoneOffset.UTC).toLocalDate()
            // Reuse the engine to select a public synthetic point near Dhuhr, then an ordinary minute correction.
            val candidate = (-180..180 step 15).mapNotNull { longitude ->
                val point = GeoPoint(40.0,longitude.toDouble())
                val schedule = (LocalPrayerCalculator().calculate(date,point,ZoneId.of("UTC"),CalculationSettings()) as? CalculationResult.Available)?.schedule
                schedule?.let { point to Duration.between(it.prayers.getValue(Prayer.DHUHR),targetAt).toMinutes().toInt() }
            }.minBy { kotlin.math.abs(it.second) }
            assertTrue(kotlin.math.abs(candidate.second) <= 60)
            val place = ManualSetup("Synthetic platform alarm fixture",candidate.first,ZoneId.of("UTC"),
                CalculationSettings(corrections = mapOf(Prayer.DHUHR to candidate.second)))
            app.settings.update { AppPreferences(place,alerts = Prayer.entries.associateWith { PrayerAlert(if (it == Prayer.DHUHR) AlertMode.NOTIFICATION else AlertMode.OFF) }) }
            AlarmCoordinator.reconcile(context)
            val event = AlarmSnapshot(context).read().single { it.target == PrayerKey(date,Prayer.DHUHR) }
            assertEquals(targetAt,event.at)
            assertTrue(AlarmCoordinator.exactAccess(context))
            manager.cancelAll()
            context.sendBroadcast(Intent(context,AlarmReceiver::class.java).putExtra("id",event.id).putExtra("token","obsolete"))
            delay(250)
            assertTrue(manager.activeNotifications.none { it.tag == event.id })
            val recordsBefore = app.repository.snapshot().records
            shell("dumpsys battery unplug")
            shell("input keyevent KEYCODE_SLEEP")
            shell("dumpsys deviceidle force-idle")
            withTimeout(90000) { while (manager.activeNotifications.none { it.tag == event.id }) delay(200) }
            assertEquals(recordsBefore,app.repository.snapshot().records) // Delivery did not fabricate completion.
            context.sendBroadcast(Intent(context,AlarmReceiver::class.java).putExtra("id",event.id).putExtra("token",event.token))
            delay(500)
            assertEquals(1,manager.activeNotifications.count { it.tag == event.id })
            shell("dumpsys deviceidle unforce"); shell("dumpsys battery reset"); shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
            if (Build.VERSION.SDK_INT >= 31) {
                val notification = manager.activeNotifications.single { it.tag == event.id }.notification
                val action = notification.actions.single { it.title.toString() == context.getString(R.string.record_on_time) }
                app.settings.update { it.copy(place = place.copy(zone = ZoneId.of("Asia/Baku"))) }
                action.actionIntent.send()
                inserted = event.target
                withTimeout(10000) { while (app.repository.snapshot().records[event.target]?.status != PrayerStatus.ON_TIME) delay(50) }
                assertEquals(ZoneId.of("UTC"),app.repository.snapshot().records.getValue(event.target).originatingZone)
            }
        } finally {
            shell("dumpsys deviceidle unforce"); shell("dumpsys battery reset"); shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
            inserted?.let { app.repository.record(it,null,ZoneId.of("UTC")) }
            app.settings.update { original }; AlarmCoordinator.cancelAll(context); AlarmCoordinator.reconcile(context)
        }
    }
}
