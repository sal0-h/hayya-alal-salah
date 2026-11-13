// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.sign

/** The caller must persist NEAREST/REFERENCE only after explicit informed consent. */
class LocalPrayerCalculator : PrayerCalculator {
    private val adhan = AdhanCalculator()
    private val prayTimes = PrayTimesCalculator()
    override fun calculate(date: LocalDate, point: GeoPoint, zone: ZoneId, settings: CalculationSettings): CalculationResult {
        val engine = if (settings.profile.engine == "adhan") adhan else prayTimes
        val ordinary = engine.calculate(date, point, zone, settings)
        if (ordinary !is CalculationResult.Unavailable || ordinary.reason == CalculationResult.Reason.UNSUPPORTED_DATE ||
            settings.polar == PolarMode.DISABLED) return ordinary
        // Bad user corrections must be corrected, not silently hidden by a different latitude.
        if (ordinary.reason == CalculationResult.Reason.INVALID_ORDER_OR_DATE && settings.corrections.isNotEmpty()) return ordinary
        fun estimated(latitude: Double): DailySchedule? {
            val effective = GeoPoint(latitude, point.longitude)
            val result = engine.calculate(date, effective, zone, settings) as? CalculationResult.Available ?: return null
            if (engine.calculate(date.plusDays(1), effective, zone, settings) !is CalculationResult.Available) return null
            return result.schedule.copy(provenance = result.schedule.provenance.copy(actual = point,
                effective = effective, quality = CalculationQuality.ESTIMATED_REFERENCE))
        }
        if (settings.polar == PolarMode.REFERENCE) {
            val result = estimated(checkNotNull(settings.referenceLatitude)) ?: return ordinary
            return CalculationResult.Available(result)
        }
        // Adapted from Adhan JS AqrabBalad (MIT): same longitude, move 0.5° toward the
        // equator until today/tomorrow are valid. Validate complete schedules for both engines.
        val direction = sign(point.latitude)
        var latitude = (abs(point.latitude) - 0.5).coerceAtLeast(0.0)
        repeat(180) {
            val schedule = estimated(latitude * direction)
            if (schedule != null) return if (settings.polar == PolarMode.ASK)
                CalculationResult.NeedsPolarConfirmation(schedule.provenance.effective.latitude)
                else CalculationResult.Available(schedule)
            if (latitude == 0.0) return ordinary
            latitude = (latitude - 0.5).coerceAtLeast(0.0)
        }
        return ordinary
    }
}
