// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Duration
import java.time.Instant

data class NightBoundaries(val sunset: Instant, val dawn: Instant, val sunrise: Instant)
enum class NightTime { OFF, MIDPOINT, LAST_THIRD }

/** PrayTimes' France and Russia profiles use its sunset-to-sunrise default. */
fun midpointEndsAtSunrise(profile: Profile): Boolean = profile == Profile.FRANCE || profile == Profile.RUSSIA

/** The evening of the selected service date through the following morning, measured on the instant timeline. */
fun nightTime(evening: DailySchedule, morning: DailySchedule, display: NightTime): Instant? {
    if (display == NightTime.OFF || morning.date != evening.date.plusDays(1) ||
        evening.zone != morning.zone || evening.provenance.profile != morning.provenance.profile ||
        evening.provenance.effective != morning.provenance.effective) return null
    val start = evening.nightBoundaries?.sunset ?: return null
    val end = morning.nightBoundaries?.let {
        if (display == NightTime.MIDPOINT && midpointEndsAtSunrise(evening.provenance.profile)) it.sunrise else it.dawn
    } ?: return null
    val duration = Duration.between(start, end)
    if (duration <= Duration.ZERO || duration >= Duration.ofDays(1)) return null
    return when (display) {
        NightTime.MIDPOINT -> start.plus(duration.dividedBy(2))
        NightTime.LAST_THIRD -> end.minus(duration.dividedBy(3))
        NightTime.OFF -> null
    }
}
