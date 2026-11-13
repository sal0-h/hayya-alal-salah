// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun effectiveStatus(key: PrayerKey, records: Map<PrayerKey, PrayerRecord>, pauses: List<PauseInterval>): PrayerStatus? =
    records[key]?.status ?: if (pauses.any { key.date in it }) PrayerStatus.EXCUSED else null

fun verifiedStreak(endingOn: LocalDate, firstDate: LocalDate, records: Map<PrayerKey, PrayerRecord>,
    pauses: List<PauseInterval>, prayer: Prayer? = null): VerifiedStreak {
    var date = endingOn
    var count = 0
    while (date >= firstDate) {
        val targets = prayer?.let(::listOf) ?: Prayer.entries
        val statuses = targets.map { effectiveStatus(PrayerKey(date, it), records, pauses) }
        when {
            statuses.all { it == PrayerStatus.EXCUSED } -> Unit
            statuses.any { it == PrayerStatus.MISSED || it == PrayerStatus.MADE_UP } -> return VerifiedStreak(count, false)
            statuses.any { it == null } -> return VerifiedStreak(count, true)
            else -> count++
        }
        date = date.minusDays(1)
    }
    return VerifiedStreak(count, false)
}

data class PrayerStatistics(val onTime: Int, val madeUp: Int, val missed: Int, val excused: Int, val unrecorded: Int) {
    val eligibleRecorded get() = onTime + madeUp + missed
    val eligibleTotal get() = eligibleRecorded + unrecorded
    val coveragePercent: Int? get() = if (eligibleTotal == 0) null else eligibleRecorded * 100 / eligibleTotal
    val onTimePercent: Int? get() = if (eligibleRecorded == 0) null else onTime * 100 / eligibleRecorded
}
fun statistics(start: LocalDate, end: LocalDate, records: Map<PrayerKey, PrayerRecord>, pauses: List<PauseInterval>,
    prayer: Prayer? = null): PrayerStatistics {
    require(end >= start && ChronoUnit.DAYS.between(start, end) <= 36600)
    val counts = mutableMapOf<PrayerStatus?, Int>()
    var date = start
    while (date <= end) {
        for (p in prayer?.let(::listOf) ?: Prayer.entries) {
            val status = effectiveStatus(PrayerKey(date, p), records, pauses)
            counts[status] = (counts[status] ?: 0) + 1
        }
        date = date.plusDays(1)
    }
    return PrayerStatistics(counts[PrayerStatus.ON_TIME] ?: 0, counts[PrayerStatus.MADE_UP] ?: 0,
        counts[PrayerStatus.MISSED] ?: 0, counts[PrayerStatus.EXCUSED] ?: 0, counts[null] ?: 0)
}

data class LocalInsights(val covered: Int, val eligible: Int, val onTime: Int, val recorded: Int,
    val perPrayer: Map<Prayer, PrayerStatistics>)
fun insights(start: LocalDate, end: LocalDate, records: Map<PrayerKey, PrayerRecord>, pauses: List<PauseInterval>): LocalInsights {
    val total = statistics(start, end, records, pauses)
    return LocalInsights(total.eligibleRecorded, total.eligibleTotal, total.onTime, total.eligibleRecorded,
        Prayer.entries.associateWith { statistics(start, end, records, pauses, it) })
}
