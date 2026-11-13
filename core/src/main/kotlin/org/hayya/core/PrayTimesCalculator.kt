// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import io.github.persiancalendar.praytimes.AsrMethod
import io.github.persiancalendar.praytimes.CalculationMethod
import io.github.persiancalendar.praytimes.Coordinates
import io.github.persiancalendar.praytimes.HighLatitudesMethod
import io.github.persiancalendar.praytimes.PrayTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.floor

class PrayTimesCalculator : PrayerCalculator {
    override fun calculate(date: LocalDate, point: GeoPoint, zone: ZoneId, settings: CalculationSettings): CalculationResult {
        require(settings.profile.engine == "praytimes")
        if (date.year !in 1900..2100) return CalculationResult.Unavailable(CalculationResult.Reason.UNSUPPORTED_DATE)
        val method = when (settings.profile) {
            Profile.FRANCE -> CalculationMethod.France
            Profile.RUSSIA -> CalculationMethod.Russia
            Profile.TEHRAN -> CalculationMethod.Tehran
            Profile.JAFARI -> CalculationMethod.Jafari
            else -> error("Wrong engine")
        }
        val asr = if (settings.asr == AsrConvention.HANAFI) AsrMethod.Hanafi else AsrMethod.Standard
        val high = when (settings.highLatitude) {
            HighLatitude.MIDDLE_OF_NIGHT -> HighLatitudesMethod.NightMiddle
            HighLatitude.SEVENTH_OF_NIGHT -> HighLatitudesMethod.OneSeventh
            HighLatitude.TWILIGHT_ANGLE -> HighLatitudesMethod.AngleBased
        }
        fun compute(d: LocalDate) = PrayTimes(method, d.year, d.monthValue, d.dayOfMonth, 0.0,
            Coordinates(point.latitude, point.longitude, 0.0), asr, high).unwrappedHours
        fun toInstant(d: LocalDate, hour: Double): Instant =
            d.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(floor(hour * 60 + 0.5).toLong() * 60)
        var anchor = date
        var times = compute(anchor)
        if (!times.dhuhr.isFinite()) return CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
        val delta = ChronoUnit.DAYS.between(toInstant(anchor, times.dhuhr).atZone(zone).toLocalDate(), date)
        if (delta != 0L) { anchor = date.plusDays(delta); times = compute(anchor) }
        val raw = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha, times.midnight)
        if (raw.any { !it.isFinite() }) return CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
        val prayers = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
            .mapIndexed { i, hour -> Prayer.entries[i] to toInstant(anchor, hour)
                .plusSeconds(60L * (settings.corrections[Prayer.entries[i]] ?: 0)) }.toMap()
        return try {
            CalculationResult.Available(DailySchedule(date, zone, prayers, toInstant(anchor, times.sunrise),
                Provenance("praytimes:1d1b4b5", settings.profile, point, point, settings),
                toInstant(anchor, times.midnight)))
        } catch (_: IllegalArgumentException) {
            CalculationResult.Unavailable(CalculationResult.Reason.INVALID_ORDER_OR_DATE)
        }
    }
}

class LocalPrayerCalculator : PrayerCalculator {
    private val adhan = AdhanCalculator()
    private val prayTimes = PrayTimesCalculator()
    override fun calculate(date: LocalDate, point: GeoPoint, zone: ZoneId, settings: CalculationSettings) =
        when (settings.profile.engine) {
            "adhan" -> adhan.calculate(date, point, zone, settings)
            else -> prayTimes.calculate(date, point, zone, settings)
        }
}
