// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.*

@Composable internal fun HistoryScreen(config: AppPreferences, history: HistorySnapshot?, selectedDate: LocalDate,
    onDate: (LocalDate) -> Unit, onRecord: (PrayerKey) -> Unit, clock: Clock = Clock.systemUTC()) {
    val place = config.place ?: return
    val today = foregroundInstant(clock).atZone(place.zone).toLocalDate()
    SectionTitle(stringResource(R.string.history))
    if (history == null) { Text(stringResource(R.string.storage_error)); return }
    val yesterday = today.minusDays(1)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(today to R.string.today, yesterday to R.string.recent_yesterday).forEach { (date, label) ->
            val selected = date == selectedDate
            Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                Box(Modifier.heightIn(min = 48.dp).selectable(selected, role = Role.Tab) { onDate(date) }.padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(label), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    DateButton(stringResource(R.string.choose_date), selectedDate, modifier = Modifier.testTag("history-date"), max = today, onDate = onDate)
    val calculated by produceState<CalculationResult?>(null, place, selectedDate) {
        value = withContext(Dispatchers.Default) { LocalPrayerCalculator().calculate(selectedDate, place.point, place.zone, place.calculation) }
    }
    ReportDrawnWhen { calculated != null }
    val calculation = (calculated as? CalculationResult.Available)?.schedule
    if (calculated != null && calculation == null) Text(stringResource(R.string.calculation_unavailable))
    DailyPrayerRows(selectedDate, calculation, place.zone, history, onRecord)
    val first = remember(config.trackingSince, today, history.records) {
        minOf(config.trackingSince ?: today, history.records.keys.minOfOrNull { it.date } ?: today)
    }
    val ending = today.minusDays(1)
    val currentStatuses = remember(today, history) {
        Prayer.entries.mapNotNull { p -> effectiveStatus(PrayerKey(today, p), history.records, history.pauses)?.let { p to it } }.toMap()
    }
    val streakEnding = if (dayResult(currentStatuses) == DayResult.UNCONFIRMED) ending else today
    val daily = remember(streakEnding, first, history) { verifiedStreak(streakEnding, first, history.records, history.pauses) }
    val insight = remember(ending, history) { insights(ending.minusDays(29), ending, history.records, history.pauses) }
    SectionTitle(stringResource(R.string.statistics))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.on_time_streak), style = MaterialTheme.typography.labelLarge)
        Text(numberLabel(daily.count), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
        if (daily.interruptedByUnknown) Text(stringResource(R.string.streak_unknown), style = MaterialTheme.typography.bodySmall)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.period_last_30), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.recorded_coverage, numberLabel(insight.covered), numberLabel(insight.eligible)))
        if (insight.eligible > 0) {
            val covered = insight.covered.toFloat() / insight.eligible
            val foreground = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.outlineVariant
            Canvas(Modifier.fillMaxWidth().height(4.dp).clearAndSetSemantics { }) {
                drawRect(track)
                val width = size.width * covered.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(foreground, androidx.compose.ui.geometry.Offset(left, 0f), androidx.compose.ui.geometry.Size(width, size.height))
            }
        }
        if (insight.recorded == 0) Text(stringResource(R.string.no_records), style = MaterialTheme.typography.bodySmall)
        else Text(stringResource(R.string.on_time_recorded, numberLabel(insight.onTime), numberLabel(insight.recorded)))
        Text(stringResource(R.string.coverage_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Prayer.entries.forEach { prayer ->
        val prayerEnding = if (currentStatuses[prayer] == null) ending else today
        val streak = remember(prayerEnding, first, history, prayer) {
            verifiedStreak(prayerEnding, first, history.records, history.pauses, prayer)
        }
        val stats = insight.perPrayer.getValue(prayer)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prayerName(prayer), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.streak_prayer, numberLabel(streak.count)), color = MaterialTheme.colorScheme.primary)
            if (stats.eligibleRecorded > 0) {
                Text(stringResource(R.string.on_time_recorded, numberLabel(stats.onTime), numberLabel(stats.eligibleRecorded)))
            }
            Text(stringResource(R.string.recorded_coverage, numberLabel(stats.eligibleRecorded), numberLabel(stats.eligibleTotal)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
