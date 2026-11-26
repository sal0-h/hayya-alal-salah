// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Duration
import java.time.Instant

enum class EventKind { AT_TIME, BEFORE, AFTER, FAJR_WAKE_UP }
data class AlertRule(val prayer: Prayer, val kind: EventKind, val offset: Duration = Duration.ZERO) {
    init {
        require(offset.abs() <= Duration.ofHours(6))
        require(kind != EventKind.FAJR_WAKE_UP || prayer == Prayer.FAJR)
        require(kind != EventKind.AT_TIME || offset.isZero)
        require(kind != EventKind.BEFORE || offset.isNegative)
        require(kind != EventKind.AFTER || (!offset.isNegative && !offset.isZero))
    }
}
data class ScheduledEvent(val id: String, val target: PrayerKey, val kind: EventKind, val at: Instant, val generation: Long)
/** Pure desired-state calculation; Android reconciliation is deliberately separate. */
fun compileSchedule(
    schedules: List<DailySchedule>, rules: Set<AlertRule>, records: Map<PrayerKey, PrayerRecord>,
    pauses: List<PauseInterval>, now: Instant, generation: Long,
): List<ScheduledEvent> {
    require(rules.map { it.prayer to it.kind }.distinct().size == rules.size)
    return schedules.flatMap { schedule ->
        if (pauses.any { schedule.date in it }) emptyList() else rules.mapNotNull { rule ->
            val key = PrayerKey(schedule.date, rule.prayer)
            if (records[key]?.status in setOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED)) null
            else {
                val at = schedule.prayers.getValue(rule.prayer).plus(rule.offset)
                // Never replay stale reminders after a process restart.
                if (at <= now) null else ScheduledEvent("${key.date}/${key.prayer}/${rule.kind}", key, rule.kind, at, generation)
            }
        }
    }.sortedBy { it.at }
}
