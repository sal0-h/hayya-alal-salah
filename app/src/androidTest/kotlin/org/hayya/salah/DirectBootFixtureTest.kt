// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.time.temporal.ChronoUnit

/** Run prepare/cleanup separately with scripts/direct_boot_ci.sh, never as ordinary tests. */
@RunWith(AndroidJUnit4::class)
class DirectBootFixtureTest {
    @Test fun prepare() = runBlocking {
        check(Build.HARDWARE in setOf("ranchu","goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.appContainer
        val prefs = context.getSharedPreferences("direct-boot-test-restore",0)
        check(!prefs.contains("original"))
        val original = app.settings.preferences.first()
        val audio = AudioImporter.import(context,"content://org.hayya.salah.test.audio/valid.wav".toUri(),app.repository)
        val recordKey = PrayerKey(LocalDate.of(1999,1,2),Prayer.ISHA)
        check(recordKey !in app.repository.snapshot().records)
        val count = app.repository.snapshot().records.size + 1
        app.repository.record(recordKey,PrayerStatus.EXCUSED,ZoneId.of("UTC"),Instant.parse("2026-11-01T00:00:00Z"))
        prefs.edit(commit = true) { putString("original",PreferencesCodec.encode(original)); putString("audio",audio.id); putInt("records",count) }
        val target = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(240)
        val date = target.atZone(ZoneOffset.UTC).toLocalDate()
        val choice = (-1L..1L).flatMap { offset -> (-180..180 step 15).mapNotNull { longitude ->
            val point = GeoPoint(40.0,longitude.toDouble())
            val schedule = (LocalPrayerCalculator().calculate(date.plusDays(offset),point,ZoneId.of("UTC"),CalculationSettings()) as? CalculationResult.Available)?.schedule ?: return@mapNotNull null
            val minutes = Duration.between(schedule.prayers.getValue(Prayer.FAJR),target).toMinutes().toInt()
            if (minutes in -180..180) point to minutes else null
        } }.minBy { kotlin.math.abs(it.second) }
        app.settings.update { AppPreferences(ManualSetup("Synthetic reboot fixture",choice.first,ZoneId.of("UTC"),Profile.MWL),fajr = FajrPreferences(true,choice.second,audio.id,ChallengeDifficulty.EASY)) }
        AlarmCoordinator.reconcile(context)
        assertTrue(AlarmSnapshot(context).read().any { it.isFajr && it.at == target })
        assertFalse(AlarmPlayback.active?.isFajr == true)
    }
    @Test fun cleanup() = runBlocking {
        check(Build.HARDWARE in setOf("ranchu","goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.appContainer
        val prefs = context.getSharedPreferences("direct-boot-test-restore",0)
        val original = PreferencesCodec.decode(checkNotNull(prefs.getString("original",null)))
        assertEquals(prefs.getInt("records",-1),app.repository.snapshot().records.size)
        val audioId = checkNotNull(prefs.getString("audio",null))
        val snapshot = app.repository.snapshot()
        val recordKey = PrayerKey(LocalDate.of(1999,1,2),Prayer.ISHA)
        val record = checkNotNull(snapshot.records[recordKey])
        assertEquals(PrayerStatus.EXCUSED,record.status)
        assertEquals(ZoneId.of("UTC"),record.originatingZone)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"),record.createdAt)
        app.repository.record(recordKey,null,ZoneId.of("UTC"))
        val audio = checkNotNull(snapshot.audio.firstOrNull { it.id == audioId })
        assertEquals(32044L,context.filesDir.resolve("audio").resolve(audio.fileName).length())
        app.repository.deleteAudio(audio.id); context.filesDir.resolve("audio").resolve(audio.fileName).delete()
        app.settings.update { original }; AlarmCoordinator.cancelAll(context); AlarmCoordinator.reconcile(context)
        prefs.edit(commit = true) { clear() }
    }
}
