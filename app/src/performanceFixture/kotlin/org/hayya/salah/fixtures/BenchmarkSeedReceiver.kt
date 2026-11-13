// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.fixtures

import android.app.Activity
import android.content.*
import kotlinx.coroutines.*
import org.hayya.core.*
import org.hayya.salah.*
import java.time.*

/** Shell-only synthetic fixtures, isolated application ID. Absent from QA/release. */
class BenchmarkSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        check(context.packageName.endsWith(".benchmark"))
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.appContainer
                val zone = ZoneId.of("Asia/Baku")
                val today = LocalDate.now(zone)
                val first = today.minusYears(3)
                val now = Instant.now().toEpochMilli()
                var count = 0
                app.database.runInTransaction {
                    val dao = app.database.prayers(); dao.reset()
                    var day = first
                    while (day < today) {
                        Prayer.entries.forEach { prayer ->
                            val ordinal = day.toEpochDay() * 5 + prayer.ordinal
                            if (ordinal % 11 != 0L) {
                                val status = when (ordinal % 17) { 0L -> PrayerStatus.MADE_UP; 1L -> PrayerStatus.MISSED; 2L -> PrayerStatus.EXCUSED; else -> PrayerStatus.ON_TIME }
                                dao.setRecord(day.toEpochDay(),prayer.name,status.name,now,zone.id); count++
                            }
                        }
                        day = day.plusDays(1)
                    }
                }
                app.settings.update { AppPreferences(ManualSetup("Public benchmark fixture",GeoPoint(40.4093,49.8671),zone,Profile.MWL),
                    language = "en",trackingSince = first) }
                DeliveryLocale.save(context,"en")
                result.setResult(Activity.RESULT_OK,"seeded=$count",null)
            } catch (_: Exception) { result.setResult(Activity.RESULT_CANCELED,"fixture-error",null) }
            finally { result.finish() }
        }
    }
}
