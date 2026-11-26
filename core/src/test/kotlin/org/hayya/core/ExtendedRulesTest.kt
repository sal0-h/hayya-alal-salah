// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.*
import kotlin.test.*

class ExtendedRulesTest {
    private val calculator = LocalPrayerCalculator()
    private val zone = ZoneId.of("Europe/Oslo")
    private val date = LocalDate.of(2026, 6, 21)
    @Test fun `polar estimation requires confirmation and preserves actual coordinates in both hemispheres`() {
        for ((point, day, z) in listOf(Triple(GeoPoint(78.22, 15.65), date, zone),
            Triple(GeoPoint(-77.85, 166.67), LocalDate.of(2026, 12, 21), ZoneId.of("Antarctica/McMurdo")))) {
            for (profile in listOf(Profile.MWL, Profile.JAFARI, Profile.TEHRAN)) {
                assertIs<CalculationResult.NeedsPolarConfirmation>(calculator.calculate(day, point, z, CalculationSettings(profile)))
                val s = assertIs<CalculationResult.Available>(calculator.calculate(day, point, z,
                    CalculationSettings(profile, polar = PolarMode.NEAREST))).schedule
                assertEquals(point, s.provenance.actual)
                assertEquals(point.longitude, s.provenance.effective.longitude)
                assertEquals(CalculationQuality.ESTIMATED_REFERENCE, s.provenance.quality)
                assertTrue(kotlin.math.abs(s.provenance.effective.latitude) < kotlin.math.abs(point.latitude))
                assertEquals(kotlin.math.sign(point.latitude), kotlin.math.sign(s.provenance.effective.latitude))
                assertIs<CalculationResult.Unavailable>(calculator.calculate(day, point, z, CalculationSettings(profile, polar = PolarMode.DISABLED)))
            }
        }
    }
    @Test fun `manual reference and advanced controls do not change engine routing`() {
        val point = GeoPoint(78.22, 15.65)
        val s = assertIs<CalculationResult.Available>(calculator.calculate(date, point, zone,
            CalculationSettings(Profile.JAFARI, polar = PolarMode.REFERENCE, referenceLatitude = 48.5))).schedule
        assertEquals(48.5, s.provenance.effective.latitude)
        for (profile in listOf(Profile.MWL, Profile.JAFARI)) {
            val custom = assertIs<CalculationResult.Available>(calculator.calculate(date, GeoPoint(40.4, 49.8), ZoneId.of("Asia/Baku"),
                CalculationSettings(profile, overrides = ParameterOverrides(fajrAngle = 15.0, ishaInterval = 100)))).schedule
            assertEquals(100, Duration.between(custom.prayers.getValue(Prayer.MAGHRIB), custom.prayers.getValue(Prayer.ISHA)).toMinutes())
        }
        assertFailsWith<IllegalArgumentException> { ParameterOverrides(ishaAngle = 14.0, ishaInterval = 90) }
        assertFailsWith<IllegalArgumentException> { ParameterOverrides(fajrAngle = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { CalculationSettings(Profile.MWL, overrides = ParameterOverrides(maghribAngle = 4.0)) }
    }
    private val normal by lazy { assertIs<CalculationResult.Available>(calculator.calculate(date, GeoPoint(40.4, 49.8),
        ZoneId.of("Asia/Baku"), CalculationSettings())).schedule }
    @Test fun `pause preserves completed records and unknown is excluded from recorded percentages`() {
        val key = PrayerKey(date, Prayer.FAJR)
        val r = PrayerRecord(key, PrayerStatus.ON_TIME, Instant.EPOCH, Instant.EPOCH, zone)
        val records = mapOf(key to r)
        val pause = listOf(PauseInterval(date, date))
        assertEquals(PrayerStatus.ON_TIME, effectiveStatus(key, records, pause))
        val paused = statistics(date, date, records, pause)
        assertEquals(1, paused.onTime); assertEquals(4, paused.excused); assertEquals(0, paused.unrecorded)
        val unpaused = statistics(date, date, records, emptyList())
        assertEquals(20, unpaused.coveragePercent); assertEquals(100, unpaused.onTimePercent)
        assertEquals(4, unpaused.unrecorded); assertEquals(0, unpaused.missed)
        assertEquals(VerifiedStreak(1, false), verifiedStreak(date, date, records, pause))
        assertEquals(VerifiedStreak(1, true), verifiedStreak(date, date.minusDays(1), records, pause, Prayer.FAJR))
    }
    @Test fun `focus expires at sunrise or next prayer and emergency bypass fabricates no record`() {
        val s = normal
        val now = s.prayers.getValue(Prayer.FAJR).plusSeconds(60)
        val w = assertNotNull(focusWindow(listOf(s), setOf(Prayer.FAJR), emptyMap(), emptyList(), now))
        assertEquals(s.sunrise, w.expiresAt)
        assertNull(focusWindow(listOf(s), setOf(Prayer.FAJR), emptyMap(), emptyList(), s.sunrise))
        assertNull(focusWindow(listOf(s), setOf(Prayer.FAJR), emptyMap(), emptyList(), now, FocusBypass(w.target, w.expiresAt)))
        val missed = PrayerRecord(w.target, PrayerStatus.MISSED, now, now, zone)
        assertNotNull(focusWindow(listOf(s), setOf(Prayer.FAJR), mapOf(w.target to missed), emptyList(), now))
        assertNull(focusWindow(listOf(s), setOf(Prayer.FAJR), emptyMap(), listOf(PauseInterval(date,date)), now))
    }
    @Test fun `Hijri display adjustment is independent of prayer settings and qibla uses actual point`() {
        val h = ummAlQuraDisplay(date, 1)
        assertEquals(ummAlQuraDisplay(date.plusDays(1), 0), h)
        val bearing = qiblaBearing(GeoPoint(40.4093, 49.8671))
        assertTrue(bearing in 200.0..220.0)
        for (difficulty in ChallengeDifficulty.entries) {
            val questions = arithmeticQuestions(difficulty, kotlin.random.Random(42))
            assertEquals(difficulty.questions, questions.size)
            assertTrue(questions.all { it.answer in 1..1000 })
        }
    }
}
