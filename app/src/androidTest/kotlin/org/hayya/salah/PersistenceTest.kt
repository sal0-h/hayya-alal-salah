// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.data.PrayerDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    @Test fun recordsSurviveDatabaseReopenAndCorrectionsPreserveOrigin() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "persistence-fixture.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, PrayerDatabase::class.java, name).build()
        val key = PrayerKey(LocalDate.of(2026, 11, 1), Prayer.FAJR)
        val zone = ZoneId.of("Asia/Baku")
        val created = Instant.parse("2026-11-01T05:00:00Z")
        try {
            var repo = PrayerRepository(db)
            repo.record(key, PrayerStatus.MISSED, zone, created)
            repo.record(key, PrayerStatus.MADE_UP, ZoneId.of("Europe/Oslo"), created.plusSeconds(3600))
            repo.addPause(PauseInterval(key.date, key.date.plusDays(3)))
            assertEquals(1, repo.snapshot().records.size)
            db.close()
            db = Room.databaseBuilder(context, PrayerDatabase::class.java, name).build()
            repo = PrayerRepository(db)
            val snapshot = repo.snapshot()
            val record = requireNotNull(snapshot.records[key])
            assertEquals(PrayerStatus.MADE_UP, record.status)
            assertEquals(created, record.createdAt)
            assertEquals(zone, record.originatingZone)
            assertEquals(PrayerStatus.MADE_UP, effectiveStatus(key, snapshot.records, snapshot.pauses))
            assertEquals(PrayerStatus.EXCUSED, effectiveStatus(PrayerKey(key.date, Prayer.ASR), snapshot.records, snapshot.pauses))
            repo.record(key, null, zone)
            repo.deletePause(snapshot.pauses.single())
            assertTrue(repo.snapshot().records.isEmpty())
            assertNull(effectiveStatus(key, repo.snapshot().records, repo.snapshot().pauses))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun observableSnapshotsAndAtomicConcurrentCorrectionsAgree() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, PrayerDatabase::class.java).build()
        try {
            val repo = PrayerRepository(db)
            val key = PrayerKey(LocalDate.of(2026, 11, 13), Prayer.ISHA)
            val observed = async { withTimeout(10_000) { repo.changes.first { it.records[key]?.status == PrayerStatus.ON_TIME } } }
            repo.record(key, PrayerStatus.ON_TIME, ZoneId.of("Asia/Baku"))
            assertEquals(PrayerStatus.ON_TIME, observed.await().records.getValue(key).status)
            coroutineScope {
                repeat(20) { index -> launch(Dispatchers.IO) {
                    repo.record(key, if (index % 2 == 0) PrayerStatus.MISSED else PrayerStatus.MADE_UP, ZoneId.of("UTC"))
                } }
            }
            assertEquals(1, repo.snapshot().records.size)
            assertEquals(ZoneId.of("Asia/Baku"), repo.snapshot().records.getValue(key).originatingZone)
        } finally { db.close() }
    }
    @Test fun preferencesRoundTripCalculationConsentAndAllFeatureSettings() {
        val place = ManualSetup("Public fixture", GeoPoint(78.2, 15.6), ZoneId.of("Europe/Oslo"),
            CalculationSettings(Profile.JAFARI, AsrConvention.HANAFI, HighLatitude.SEVENTH_OF_NIGHT,
                mapOf(Prayer.ISHA to 3), ParameterOverrides(fajrAngle = 16.0, ishaInterval = 100, maghribAngle = 4.0),
                PolarMode.REFERENCE, 48.5), automatic = true, refreshedAt = Instant.EPOCH)
        val config = AppPreferences(place, alerts = Prayer.entries.associateWith { PrayerAlert(AlertMode.ADHAN, 10, 15, "user-sound") },
            fajr = FajrPreferences(true, -30, "builtin_fajr", ChallengeDifficulty.HARD),
            focus = FocusPreferences(true, setOf("example.selected"), setOf(Prayer.FAJR)),
            appearance = AppearancePreferences(ThemePreference.DARK, Palette.PLUM, TimePreference.TWENTY_FOUR, NumeralPreference.ARABIC_INDIC, true),
            language = "ar", hijriAdjustment = 2, trackingSince = LocalDate.of(2026, 1, 1))
        assertEquals(config, PreferencesCodec.decode(PreferencesCodec.encode(config)))
        val defaults = PreferencesCodec.decode("{\"version\":1}")
        assertNull(defaults.place)
        assertFalse(defaults.fajr.enabled)
        assertFalse(defaults.focus.enabled)
        assertTrue(defaults.alerts.values.all { it.mode == AlertMode.OFF })
    }
}
