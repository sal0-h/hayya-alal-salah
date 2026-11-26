// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.*
import java.time.format.FormatStyle

@Composable internal fun TodayScreen(config: AppPreferences, history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit,
    update: PreferencesUpdate, onQibla: () -> Unit, clock: Clock = Clock.systemUTC()) {
    val place = config.place ?: return
    var now by remember { mutableStateOf(clock.instant()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, clock) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) { now = clock.instant(); delay(30_000) }
    } }
    val date = now.atZone(place.zone).toLocalDate()
    val results by produceState<Map<LocalDate, CalculationResult>?>(null, place, date) {
        value = withContext(Dispatchers.Default) { val calc = LocalPrayerCalculator()
            (-1L..1L).associate { d -> date.plusDays(d).let { it to calc.calculate(it, place.point, place.zone, place.calculation) } } }
    }
    ReportDrawnWhen { results != null }
    val result = results?.get(date)
    val schedule = (result as? CalculationResult.Available)?.schedule
    val schedules = results?.values?.mapNotNull { (it as? CalculationResult.Available)?.schedule }.orEmpty()
    val next = nextPrayer(schedules, now)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(place.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onQibla, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.qibla)) }
    }
    Text(dateLabel(date, FormatStyle.FULL), color = MaterialTheme.colorScheme.onSurfaceVariant)
    val hijri = runCatching { ummAlQuraDisplay(date, config.hijriAdjustment) }.getOrNull()
    if (hijri != null) Text(stringResource(R.string.hijri_convention, numeralLabel("${hijri.day}/${hijri.month}/${hijri.year}")),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (results == null) LoadingIndicator()
    if (result is CalculationResult.NeedsPolarConfirmation) {
        Text(stringResource(R.string.polar_consent, "%.2f".format(java.util.Locale.ROOT, result.referenceLatitude)))
        Button(onClick = { update { it.copy(place = place.copy(calculation = place.calculation.copy(polar = PolarMode.NEAREST))) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.use_estimate))
        }
        OutlinedButton(onClick = { update { it.copy(place = place.copy(calculation = place.calculation.copy(polar = PolarMode.DISABLED))) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.polar_disabled))
        }
    } else if (result is CalculationResult.Unavailable) Text(stringResource(R.string.calculation_unavailable))
    if (next != null) {
        NextPrayerMarker(prayerName(next.key.prayer), timeLabel(next.at, place.zone))
        val minutes = Duration.between(now, next.at).toMinutes().coerceAtLeast(0)
        Text(stringResource(R.string.remaining, numberLabel(minutes / 60), numberLabel(minutes % 60)), color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
    }
    if (schedule?.provenance?.quality == CalculationQuality.ESTIMATED_REFERENCE) {
        Text(stringResource(R.string.estimated_boundaries, "%.2f".format(java.util.Locale.ROOT, schedule.provenance.effective.latitude)),
            color = MaterialTheme.colorScheme.primary)
    }
    if (history?.pauses?.any { date in it } == true) Text(stringResource(R.string.paused_today), style = MaterialTheme.typography.titleMedium)
    if (history == null) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
    SectionTitle(stringResource(R.string.today))
    DailyPrayerRows(date, schedule, place.zone, history, onRecord, next?.at)
    Text(profileName(place.profile), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable internal fun DailyPrayerRows(date: LocalDate, schedule: DailySchedule?, zone: ZoneId,
    history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit, nextAt: Instant? = null) {
    val rows = listOf<Prayer?>(Prayer.FAJR, null, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    rows.forEach { prayer ->
        val label = if (prayer == null) stringResource(R.string.sunrise) else prayerName(prayer)
        val at = if (prayer == null) schedule?.sunrise else schedule?.prayers?.get(prayer)
        val status = if (prayer != null && history != null) effectiveStatus(PrayerKey(date, prayer), history.records, history.pauses) else null
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(at?.let { timeLabel(it, zone) } ?: "—", style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
                    fontWeight = if (at != null && at == nextAt) FontWeight.Bold else FontWeight.Normal)
            }
            if (prayer != null) {
                val description = "$label, ${if (history == null) stringResource(R.string.not_available) else statusName(status)}"
                TextButton(enabled = history != null, onClick = { onRecord(PrayerKey(date, prayer)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("record-open-${prayer.name.lowercase()}").semantics { contentDescription = description }) {
                    Text(if (history == null) stringResource(R.string.not_available) else "${statusSymbol(status)}  ${statusName(status)}")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Same content for public synthetic screenshots, without reading private storage. */
@Composable internal fun Today(setup: ManualSetup, clock: Clock = Clock.systemUTC()) = TodayScreen(
    AppPreferences(place = setup), HistorySnapshot(emptyMap(), emptyList(), emptyList()), {}, {}, {}, clock)
