// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

class AdhanCalculator : PrayerCalculator {
    override fun calculate(date: LocalDate, point: GeoPoint, zone: ZoneId, settings: CalculationSettings): CalculationResult {
        require(settings.profile.engine == "adhan")
        if (date.year !in 1900..2100) return CalculationResult.Unavailable(CalculationResult.Reason.UNSUPPORTED_DATE)
        return try {
            val method = when (settings.profile) {
                Profile.MWL -> CalculationMethod.MUSLIM_WORLD_LEAGUE
                Profile.ISNA -> CalculationMethod.NORTH_AMERICA
                Profile.EGYPTIAN -> CalculationMethod.EGYPTIAN
                Profile.KARACHI -> CalculationMethod.KARACHI
                Profile.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
                Profile.DUBAI -> CalculationMethod.DUBAI
                Profile.QATAR -> CalculationMethod.QATAR
                Profile.KUWAIT -> CalculationMethod.KUWAIT
                Profile.MOONSIGHTING -> CalculationMethod.MOON_SIGHTING_COMMITTEE
                Profile.SINGAPORE -> CalculationMethod.SINGAPORE
                Profile.TURKEY -> CalculationMethod.TURKEY
                else -> error("Wrong engine")
            }
            val base = method.parameters
            val ramadan = settings.profile == Profile.UMM_AL_QURA &&
                HijrahDate.from(date).get(ChronoField.MONTH_OF_YEAR) == 9
            val params = base.copy(
                madhab = if (settings.asr == AsrConvention.HANAFI) Madhab.HANAFI else Madhab.SHAFI,
                highLatitudeRule = when (settings.highLatitude) {
                    HighLatitude.MIDDLE_OF_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
                    HighLatitude.SEVENTH_OF_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
                    HighLatitude.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
                },
                fajrAngle = settings.overrides.fajrAngle ?: base.fajrAngle,
                ishaAngle = settings.overrides.ishaAngle ?: base.ishaAngle,
                ishaInterval = settings.overrides.ishaInterval
                    ?: if (settings.overrides.ishaAngle != null) 0 else if (ramadan) 120 else base.ishaInterval,
            )
            // 0.0.7 throws IllegalStateException when a required solar event is absent.
            // Catch only at the upstream constructor boundary, not around application logic.
            fun compute(d: LocalDate): PrayerTimes? = try {
                PrayerTimes(Coordinates(point.latitude, point.longitude),
                    DateComponents(d.year, d.monthValue, d.dayOfMonth), params)
            } catch (_: IllegalStateException) { null }
            var times = compute(date)
                ?: return CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
            fun instant(millis: Long) = Instant.ofEpochMilli(millis)
            // Solar date and civil date can differ across the international date line.
            val delta = ChronoUnit.DAYS.between(instant(times.dhuhr.toEpochMilliseconds()).atZone(zone).toLocalDate(), date)
            if (delta != 0L) times = compute(date.plusDays(delta))
                ?: return CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
            val prayers = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
                .mapIndexed { i, t -> Prayer.entries[i] to instant(t.toEpochMilliseconds())
                    .plusSeconds(60L * (settings.corrections[Prayer.entries[i]] ?: 0)) }.toMap()
            try {
                CalculationResult.Available(DailySchedule(date, zone, prayers,
                    instant(times.sunrise.toEpochMilliseconds()),
                    Provenance("adhan2:0.0.7", settings.profile, point, point, settings)))
            } catch (_: IllegalArgumentException) {
                CalculationResult.Unavailable(CalculationResult.Reason.INVALID_ORDER_OR_DATE)
            }
        } catch (_: IllegalArgumentException) {
            CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
        } catch (_: NullPointerException) {
            // Upstream signals absent polar solar events through nullable internals.
            CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)
        } catch (_: java.time.DateTimeException) {
            CalculationResult.Unavailable(CalculationResult.Reason.UNSUPPORTED_DATE)
        }
    }
}
