// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.*
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable internal fun HistoryScreen(config: AppPreferences, history: HistorySnapshot?, selectedDate: LocalDate,
    onDate: (LocalDate) -> Unit, onRecord: (PrayerKey) -> Unit) {
    val place = config.place ?: return
    val today = LocalDate.now(place.zone)
    var monthString by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate).toString()) }
    val month = YearMonth.parse(monthString)
    val locale = LocalConfiguration.current.locales[0]
    SectionTitle(stringResource(R.string.history))
    if (history == null) { Text(stringResource(R.string.storage_error)); return }
    DateButton(stringResource(R.string.choose_date), selectedDate, today) { onDate(it); monthString = YearMonth.from(it).toString() }
    val previousLabel = stringResource(R.string.previous_month)
    val nextLabel = stringResource(R.string.next_month)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { monthString = month.minusMonths(1).toString() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("‹", Modifier.semantics { contentDescription = previousLabel }) }
        Text("${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}", Modifier.weight(1f))
        TextButton(enabled = month < YearMonth.from(today), onClick = { monthString = month.plusMonths(1).toString() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("›", Modifier.semantics { contentDescription = nextLabel }) }
    }
    val weekStart = WeekFields.of(locale).firstDayOfWeek
    val offset = Math.floorMod(month.atDay(1).dayOfWeek.value - weekStart.value, 7)
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(Modifier.width(360.dp)) {
            repeat(7) { i -> Text(weekStart.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale), modifier = Modifier.weight(1f)) }
        }
        repeat((offset + month.lengthOfMonth() + 6) / 7) { week ->
            Row(Modifier.width(360.dp)) {
                repeat(7) { day ->
                    val number = week * 7 + day - offset + 1
                    if (number !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f)) else {
                        val d = month.atDay(number)
                        val count = Prayer.entries.count { effectiveStatus(PrayerKey(d, it), history.records, history.pauses) != null }
                        val spoken = "${dateLabel(d)}, $count / 5"
                        TextButton(enabled = d <= today, onClick = { onDate(d) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = spoken },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (d == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(number.toString(), style = MaterialTheme.typography.titleSmall)
                                Text("$count/5", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
    Text(dateLabel(selectedDate), style = MaterialTheme.typography.titleMedium)
    val calculation by produceState<DailySchedule?>(null, place, selectedDate) {
        value = withContext(Dispatchers.Default) { (LocalPrayerCalculator().calculate(selectedDate, place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule }
    }
    DailyPrayerRows(selectedDate, calculation, place.zone, history, onRecord)
    val first = minOf(config.trackingSince ?: today, history.records.keys.minOfOrNull { it.date } ?: today)
    val currentStatuses = Prayer.entries.mapNotNull { p -> effectiveStatus(PrayerKey(today, p), history.records, history.pauses)?.let { p to it } }.toMap()
    val streakEnding = if (dayResult(currentStatuses) == DayResult.UNCONFIRMED) today.minusDays(1) else today
    val ending = today.minusDays(1)
    val daily = verifiedStreak(streakEnding, first, history.records, history.pauses)
    Text(stringResource(R.string.streak_daily, daily.count), style = MaterialTheme.typography.titleMedium)
    if (daily.interruptedByUnknown) Text(stringResource(R.string.streak_unknown), color = MaterialTheme.colorScheme.onSurfaceVariant)
    SectionTitle(stringResource(R.string.statistics))
    Text(stringResource(R.string.coverage_explanation), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.period_last_30), style = MaterialTheme.typography.labelLarge)
    val start = ending.minusDays(29)
    val insight = insights(start, ending, history.records, history.pauses)
    if (insight.recorded == 0) Text(stringResource(R.string.no_records))
    Text(stringResource(R.string.recorded_coverage, insight.covered, insight.eligible))
    Text(stringResource(R.string.on_time_recorded, insight.onTime, insight.recorded))
    Prayer.entries.forEach { prayer ->
        val prayerEnding = if (currentStatuses[prayer] == null) ending else today
        val streak = verifiedStreak(prayerEnding, first, history.records, history.pauses, prayer)
        val stats = insight.perPrayer.getValue(prayer)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prayerName(prayer), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.streak_prayer, streak.count))
            Text(stringResource(R.string.on_time_recorded, stats.onTime, stats.eligibleRecorded))
            Text(stringResource(R.string.recorded_coverage, stats.eligibleRecorded, stats.eligibleTotal), color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable internal fun PauseScreen(history: HistorySnapshot?, zone: java.time.ZoneId, onAdd: (PauseInterval) -> Unit, onRemove: (PauseInterval) -> Unit) {
    var startDay by rememberSaveable { mutableLongStateOf(LocalDate.now(zone).toEpochDay()) }
    var endDay by rememberSaveable { mutableLongStateOf(startDay) }
    var invalid by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.pause))
    Text(stringResource(R.string.pause_explanation))
    DateButton(stringResource(R.string.start_date), LocalDate.ofEpochDay(startDay)) { startDay = it.toEpochDay(); if (endDay < startDay) endDay = startDay }
    DateButton(stringResource(R.string.end_date), LocalDate.ofEpochDay(endDay)) { endDay = it.toEpochDay() }
    if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
    Button(enabled = history != null, onClick = {
        invalid = endDay < startDay
        if (!invalid) onAdd(PauseInterval(LocalDate.ofEpochDay(startDay), LocalDate.ofEpochDay(endDay)))
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.add_pause)) }
    history?.pauses?.forEach { pause ->
        Text("${dateLabel(pause.start)} — ${dateLabel(pause.endInclusive)}")
        TextButton(onClick = { onRemove(pause) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.remove)) }
    }
}
