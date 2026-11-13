// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.*
import kotlin.test.*

/** Broad seasonal invariants supplement, rather than replace, authority comparisons. */
class SeasonalCalculationTest {
    @Test fun `every profile preserves ordered dates through twelve months in both hemispheres`() {
        val calculator = LocalPrayerCalculator()
        val places = listOf(GeoPoint(40.4093,49.8671) to ZoneId.of("Asia/Baku"),
            GeoPoint(-33.8688,151.2093) to ZoneId.of("Australia/Sydney"),
            GeoPoint(27.7172,85.3240) to ZoneId.of("Asia/Kathmandu"))
        for ((point,zone) in places) for (month in 1..12) for (profile in Profile.entries) {
            val date = LocalDate.of(2026,month,15)
            val schedule = assertIs<CalculationResult.Available>(calculator.calculate(date,point,zone,CalculationSettings(profile))).schedule
            val ordered = listOf(schedule.prayers.getValue(Prayer.FAJR),schedule.sunrise) + Prayer.entries.drop(1).map { schedule.prayers.getValue(it) }
            assertTrue(ordered.zipWithNext().all { (a,b) -> a < b }, "$point $date $profile")
            assertEquals(date,schedule.prayers.getValue(Prayer.DHUHR).atZone(zone).toLocalDate())
            assertEquals(point,schedule.provenance.actual)
        }
    }
    @Test fun `seasonal polar proposals stay disclosed for both engines and transition back to actual coordinates`() {
        val calculator = LocalPrayerCalculator()
        for (profile in listOf(Profile.MWL,Profile.JAFARI)) {
            val point = GeoPoint(69.6492,18.9553)
            val zone = ZoneId.of("Europe/Oslo")
            for (month in listOf(6,12)) {
                val date = LocalDate.of(2026,month,21)
                val unconfirmed = calculator.calculate(date,point,zone,CalculationSettings(profile))
                assertIs<CalculationResult.NeedsPolarConfirmation>(unconfirmed)
                val confirmed = assertIs<CalculationResult.Available>(calculator.calculate(date,point,zone,CalculationSettings(profile,polar = PolarMode.NEAREST))).schedule
                assertEquals(CalculationQuality.ESTIMATED_REFERENCE,confirmed.provenance.quality)
            }
            val normal = assertIs<CalculationResult.Available>(calculator.calculate(LocalDate.of(2026,3,21),point,zone,CalculationSettings(profile,polar = PolarMode.NEAREST))).schedule
            assertEquals(point,normal.provenance.effective)
            assertEquals(CalculationQuality.CALCULATED,normal.provenance.quality)
        }
    }
}
