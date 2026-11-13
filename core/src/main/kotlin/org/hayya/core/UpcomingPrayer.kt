// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Instant

data class PrayerOccurrence(val key: PrayerKey, val at: Instant)

/** Include the previous service date: its Isha can still be ahead after midnight. */
fun nextPrayer(schedules: List<DailySchedule>, now: Instant): PrayerOccurrence? =
    schedules.flatMap { schedule ->
        schedule.prayers.map { (prayer, at) -> PrayerOccurrence(PrayerKey(schedule.date, prayer), at) }
    }.filter { it.at > now }.minByOrNull { it.at }
