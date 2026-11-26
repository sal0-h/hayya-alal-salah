// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Prayer { FAJR, DHUHR, ASR, MAGHRIB, ISHA }
enum class PrayerStatus { ON_TIME, MADE_UP, MISSED, EXCUSED }
data class PrayerKey(val date: LocalDate, val prayer: Prayer)
data class PrayerRecord(
    val key: PrayerKey,
    val status: PrayerStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val originatingZone: ZoneId,
) {
    init { require(!updatedAt.isBefore(createdAt)) }
}
// Absence of a record is UNRECORDED. Time passing never creates a record.
data class PauseInterval(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }
    operator fun contains(date: LocalDate) = date >= start && date <= endInclusive
}
enum class DayResult { ON_TIME, EXCUSED, INTERRUPTED, UNCONFIRMED }
fun dayResult(statuses: Map<Prayer, PrayerStatus>): DayResult = when {
    statuses.size == Prayer.entries.size && statuses.values.all { it == PrayerStatus.EXCUSED } -> DayResult.EXCUSED
    statuses.values.any { it == PrayerStatus.MISSED || it == PrayerStatus.MADE_UP } -> DayResult.INTERRUPTED
    Prayer.entries.any { it !in statuses } -> DayResult.UNCONFIRMED
    else -> DayResult.ON_TIME
}
data class VerifiedStreak(val count: Int, val interruptedByUnknown: Boolean)
/** Caller supplies the last finished service date, never an unfinished current day. */
fun dailyStreak(endingOn: LocalDate, firstTrackedDate: LocalDate, records: Collection<PrayerRecord>): VerifiedStreak {
    val indexed = records.associateBy { it.key }
    var date = endingOn
    var count = 0
    while (date >= firstTrackedDate) {
        val statuses = Prayer.entries.mapNotNull { p -> indexed[PrayerKey(date, p)]?.let { p to it.status } }.toMap()
        when (dayResult(statuses)) {
            DayResult.ON_TIME -> count++
            DayResult.EXCUSED -> Unit
            DayResult.INTERRUPTED -> return VerifiedStreak(count, false)
            DayResult.UNCONFIRMED -> return VerifiedStreak(count, true)
        }
        date = date.minusDays(1)
    }
    return VerifiedStreak(count, false)
}
