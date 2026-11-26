// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed interface FajrTiming {
    data class Relative(val minutes: Int) : FajrTiming {
        init { require(minutes in -180..180) }
    }
    data class Fixed(val time: LocalTime) : FajrTiming {
        init { require(time.second == 0 && time.nano == 0) }
    }
}

/** Include yesterday for positive offsets and the week ahead for upcoming local dates. */
fun alarmDates(now: Instant, zone: ZoneId): List<LocalDate> {
    val today = now.atZone(zone).toLocalDate()
    return (-1L..7L).map(today::plusDays)
}

fun fajrAlarmAt(date: LocalDate, zone: ZoneId, timing: FajrTiming, fajr: Instant?): Instant? = when (timing) {
    is FajrTiming.Relative -> fajr?.plusSeconds(timing.minutes * 60L)
    // atZone moves a gap forward and picks the earlier offset of an overlap. Skip a
    // wholly missing local date so it cannot produce a second alarm on the next date.
    is FajrTiming.Fixed -> date.atTime(timing.time).atZone(zone).takeIf { it.toLocalDate() == date }?.toInstant()
}

fun compileFajrAlarms(dates: List<LocalDate>, zone: ZoneId, timing: FajrTiming, fajrByDate: Map<LocalDate, Instant>,
    records: Map<PrayerKey, PrayerRecord>, pauses: List<PauseInterval>, now: Instant): List<ScheduledEvent> =
    dates.mapNotNull { date ->
        val key = PrayerKey(date, Prayer.FAJR)
        val at = fajrAlarmAt(date, zone, timing, fajrByDate[date]) ?: return@mapNotNull null
        if (at <= now || reminderSuppressed(key, at, zone, records, pauses)) null
        else ScheduledEvent("$date/FAJR/FAJR_WAKE_UP", key, EventKind.FAJR_WAKE_UP, at, 0)
    }.sortedBy { it.at }
