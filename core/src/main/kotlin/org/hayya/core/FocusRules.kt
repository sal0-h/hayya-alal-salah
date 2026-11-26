// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Instant

data class FocusWindow(val target: PrayerKey, val startsAt: Instant, val expiresAt: Instant)
data class FocusBypass(val target: PrayerKey, val expiresAt: Instant)
fun focusWindow(schedules: List<DailySchedule>, selected: Set<Prayer>, records: Map<PrayerKey, PrayerRecord>,
    pauses: List<PauseInterval>, now: Instant, bypass: FocusBypass? = null): FocusWindow? {
    val occurrences = schedules.flatMap { s -> s.prayers.map { (p, at) -> PrayerOccurrence(PrayerKey(s.date, p), at) } }.sortedBy { it.at }
    val current = occurrences.lastOrNull { it.at <= now } ?: return null
    if (current.key.prayer !in selected) return null
    val schedule = schedules.first { it.date == current.key.date }
    val expiry = if (current.key.prayer == Prayer.FAJR) schedule.sunrise
        else occurrences.firstOrNull { it.at > current.at }?.at ?: return null
    if (now >= expiry || pauses.any { current.key.date in it }) return null
    if (effectiveStatus(current.key, records, pauses) in setOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED)) return null
    if (bypass?.target == current.key && bypass.expiresAt > now) return null
    // MISSED is not completion or a routine skip; the window still expires normally.
    return FocusWindow(current.key, current.at, expiry)
}

enum class ChallengeDifficulty(val questions: Int) { OFF(0), EASY(1), MEDIUM(3), HARD(5) }
data class ArithmeticQuestion(val left: Int, val right: Int, val multiply: Boolean) {
    val answer get() = if (multiply) left * right else left + right
}
fun arithmeticQuestions(difficulty: ChallengeDifficulty, random: kotlin.random.Random = kotlin.random.Random.Default): List<ArithmeticQuestion> =
    List(difficulty.questions) {
        when (difficulty) {
            ChallengeDifficulty.OFF, ChallengeDifficulty.EASY -> ArithmeticQuestion(random.nextInt(1, 11), random.nextInt(1, 11), false)
            ChallengeDifficulty.MEDIUM -> ArithmeticQuestion(random.nextInt(2, 13), random.nextInt(2, 13), true)
            ChallengeDifficulty.HARD -> ArithmeticQuestion(random.nextInt(11, 31), random.nextInt(2, 13), true)
        }
    }
