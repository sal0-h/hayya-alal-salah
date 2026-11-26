// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import org.hayya.core.CalculationResult
import org.hayya.core.DailySchedule
import org.hayya.core.LocalPrayerCalculator
import org.hayya.core.Prayer
import org.hayya.salah.ManualSetup
import java.time.LocalDate

internal fun calculateAlarmDays(place: ManualSetup, dates: List<LocalDate>): List<DailySchedule> {
    val calculator = LocalPrayerCalculator()
    return dates.mapNotNull { date ->
        (calculator.calculate(date, place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule
    }
}

internal fun fajrByDate(schedules: List<DailySchedule>) =
    schedules.associate { it.date to it.prayers.getValue(Prayer.FAJR) }
