// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.*

class CoreTest {
    private val calculator = LocalPrayerCalculator()
    private val date = LocalDate.of(2026, 11, 13)
    private val zone = ZoneId.of("Asia/Baku")
    private val point = GeoPoint(40.4093, 49.8671)
    private fun schedule(d: LocalDate = date, p: GeoPoint = point, z: ZoneId = zone, profile: Profile = Profile.MWL,
        asr: AsrConvention = AsrConvention.STANDARD): DailySchedule =
        assertIs<CalculationResult.Available>(calculator.calculate(d, p, z, CalculationSettings(profile, asr))).schedule
    private fun record(d: LocalDate, p: Prayer, status: PrayerStatus) =
        PrayerRecord(PrayerKey(d, p), status, Instant.EPOCH, Instant.EPOCH, zone)

    // Adapted expected values from Adhan PrayerTimesTest.testPrayerTimes, MIT,
    // revision 6a0426fcd764785df70428abd21db0fae2db234c (adhan2-v0.0.7).
    @Test fun `Adhan adapter agrees with the pinned upstream Raleigh fixture`() {
        val z = ZoneId.of("America/New_York")
        val s = schedule(LocalDate.of(2015, 7, 12), GeoPoint(35.7750, -78.6336), z, Profile.ISNA, AsrConvention.HANAFI)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT)
        assertEquals(listOf("04:42", "13:21", "18:22", "20:32", "21:57"),
            Prayer.entries.map { s.prayers.getValue(it).atZone(z).format(formatter) })
        assertEquals("06:08", s.sunrise.atZone(z).format(formatter))
    }
    @Test fun `all profiles return ordered local dates and provenance in Baku`() {
        Profile.entries.forEach { profile ->
            val s = schedule(profile = profile)
            assertEquals(profile, s.provenance.profile)
            assertEquals(point, s.provenance.actual)
            assertEquals(date, s.prayers.getValue(Prayer.DHUHR).atZone(zone).toLocalDate())
            assertEquals(5, s.prayers.size)
        }
    }
    @Test fun `both engines preserve fractional zones DST dates leap date and date line`() {
        val cases = listOf(
            Triple(GeoPoint(27.7172, 85.324), ZoneId.of("Asia/Kathmandu"), LocalDate.of(2024, 2, 29)),
            Triple(GeoPoint(40.7128, -74.006), ZoneId.of("America/New_York"), LocalDate.of(2026, 3, 8)),
            Triple(GeoPoint(40.7128, -74.006), ZoneId.of("America/New_York"), LocalDate.of(2026, 11, 1)),
            Triple(GeoPoint(1.8721, -157.4278), ZoneId.of("Pacific/Kiritimati"), date),
        )
        for ((p, z, d) in cases) for (profile in listOf(Profile.MWL, Profile.JAFARI)) {
            val s = schedule(d, p, z, profile)
            assertEquals(d, s.prayers.getValue(Prayer.DHUHR).atZone(z).toLocalDate())
        }
    }
    @Test fun `Hanafi Asr is later for both engines`() {
        for (profile in listOf(Profile.MWL, Profile.TEHRAN)) {
            assertTrue(schedule(profile = profile, asr = AsrConvention.HANAFI).prayers.getValue(Prayer.ASR) >
                schedule(profile = profile).prayers.getValue(Prayer.ASR))
        }
    }
    @Test fun `Jafari Maghrib follows sunset and midnight preserves next date`() {
        val s = schedule(profile = Profile.JAFARI)
        assertTrue(s.prayers.getValue(Prayer.MAGHRIB) > schedule(profile = Profile.MWL).prayers.getValue(Prayer.MAGHRIB))
        assertNotNull(s.midnight)
        assertTrue(s.midnight > s.prayers.getValue(Prayer.MAGHRIB))
    }
    @Test fun `Umm al Qura Ramadan interval is explicitly 120 minutes`() {
        val ramadan = schedule(LocalDate.of(2026, 3, 1), GeoPoint(21.4225, 39.8262), ZoneId.of("Asia/Riyadh"), Profile.UMM_AL_QURA)
        assertEquals(120, Duration.between(ramadan.prayers.getValue(Prayer.MAGHRIB), ramadan.prayers.getValue(Prayer.ISHA)).toMinutes())
        val normal = schedule(profile = Profile.UMM_AL_QURA)
        assertEquals(90, Duration.between(normal.prayers.getValue(Prayer.MAGHRIB), normal.prayers.getValue(Prayer.ISHA)).toMinutes())
    }
    @Test fun `both engines fail closed for polar day and night`() {
        for (profile in listOf(Profile.MWL, Profile.JAFARI)) for (d in listOf(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
            assertIs<CalculationResult.Unavailable>(calculator.calculate(d, GeoPoint(78.22, 15.65), ZoneId.of("Arctic/Longyearbyen"), CalculationSettings(profile, polar = PolarMode.DISABLED)))
        }
    }
    @Test fun `invalid coordinates and corrections are rejected`() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, 181.0) }
        assertFailsWith<IllegalArgumentException> { CalculationSettings(corrections = mapOf(Prayer.FAJR to 121)) }
        assertIs<CalculationResult.Unavailable>(calculator.calculate(date, point, zone,
            CalculationSettings(corrections = mapOf(Prayer.FAJR to 120))))
    }
    @Test fun `unknown records never become missed and excuses pause a verified streak`() {
        assertEquals(DayResult.UNCONFIRMED, dayResult(emptyMap()))
        val records = Prayer.entries.flatMap { p -> listOf(record(date, p, PrayerStatus.ON_TIME),
            record(date.minusDays(1), p, PrayerStatus.EXCUSED), record(date.minusDays(2), p, PrayerStatus.ON_TIME)) }
        assertEquals(VerifiedStreak(2, true), dailyStreak(date, date.minusDays(3), records))
        val corrected = records.filterNot { it.key == PrayerKey(date, Prayer.FAJR) } + record(date, Prayer.FAJR, PrayerStatus.MADE_UP)
        assertEquals(VerifiedStreak(0, false), dailyStreak(date, date.minusDays(3), corrected))
    }
    @Test fun `compiler suppresses completed excused paused and stale targets with stable IDs`() {
        val s = schedule()
        val now = s.prayers.getValue(Prayer.FAJR).minusSeconds(3600)
        val rules = Prayer.entries.map { AlertRule(it, EventKind.AT_TIME) }.toSet()
        val first = compileSchedule(listOf(s), rules, emptyMap(), emptyList(), now, 1)
        val changed = compileSchedule(listOf(s), rules, emptyMap(), emptyList(), now, 2)
        assertEquals(first.map { it.id }, changed.map { it.id })
        assertTrue(changed.all { it.generation == 2L })
        for (status in listOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED)) {
            val r = record(date, Prayer.FAJR, status)
            assertEquals(4, compileSchedule(listOf(s), rules, mapOf(r.key to r), emptyList(), now, 1).size)
        }
        assertTrue(compileSchedule(listOf(s), rules, emptyMap(), listOf(PauseInterval(date, date)), now, 1).isEmpty())
        assertTrue(compileSchedule(listOf(s), rules, emptyMap(), emptyList(), now.plus(Duration.ofDays(2)), 1).isEmpty())
    }
    @Test fun `notification target keeps original service date after midnight`() {
        val s = schedule()
        val rule = AlertRule(Prayer.ISHA, EventKind.AFTER, Duration.ofHours(6))
        val event = compileSchedule(listOf(s), setOf(rule), emptyMap(), emptyList(), Instant.EPOCH, 1).single()
        assertEquals(PrayerKey(date, Prayer.ISHA), event.target)
        assertEquals(date.plusDays(1), event.at.atZone(zone).toLocalDate())
    }
    @Test fun `next prayer includes previous service date Isha after civil midnight`() {
        val previous = schedule().let { s -> s.copy(prayers = s.prayers +
            (Prayer.ISHA to date.plusDays(1).atTime(0, 30).atZone(zone).toInstant())) }
        val current = schedule(date.plusDays(1))
        val now = date.plusDays(1).atTime(0, 15).atZone(zone).toInstant()
        val next = assertNotNull(nextPrayer(listOf(previous, current), now))
        assertEquals(PrayerKey(date, Prayer.ISHA), next.key)
        assertEquals(previous.prayers.getValue(Prayer.ISHA), next.at)
        assertEquals(PrayerKey(date.plusDays(1), Prayer.FAJR),
            nextPrayer(listOf(previous, current), next.at)?.key)
        assertNull(nextPrayer(listOf(previous), next.at))
    }

}
