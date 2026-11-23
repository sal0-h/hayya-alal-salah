// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.*
import java.time.format.FormatStyle

@Composable internal fun TodayScreen(config: AppPreferences, history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit,
    update: PreferencesUpdate, onQibla: () -> Unit, clock: Clock = Clock.systemUTC(),
    onStatus: ((PrayerKey, PrayerStatus?, PrayerStatus?) -> Unit)? = null) {
    val place = config.place ?: return
    val now = foregroundInstant(clock)
    var showLegend by remember { mutableStateOf(false) }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(place.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onQibla, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.qibla)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(dateLabel(date, FormatStyle.LONG), style = MaterialTheme.typography.bodyMedium)
        val hijri = remember(date, config.hijriAdjustment) {
            runCatching { ummAlQuraDisplay(date, config.hijriAdjustment) }.getOrNull()
        }
        if (hijri != null) Text(hijriLabel(hijri), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (results == null) Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
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
        val minutes = (Duration.between(now, next.at).seconds.coerceAtLeast(0) + 59) / 60
        NextPrayerMarker(prayerName(next.key.prayer), timeLabel(next.at, place.zone),
            stringResource(R.string.remaining, numberLabel(minutes / 60), numberLabel(minutes % 60)))
    }
    if (schedule?.provenance?.quality == CalculationQuality.ESTIMATED_REFERENCE) {
        Text(stringResource(R.string.estimated_boundaries, "%.2f".format(java.util.Locale.ROOT, schedule.provenance.effective.latitude)),
            color = MaterialTheme.colorScheme.primary)
    }
    if (history?.pauses?.any { date in it } == true) Text(stringResource(R.string.paused_today), style = MaterialTheme.typography.titleMedium)
    if (history == null) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.today)) }
            val help = stringResource(R.string.status_legend)
            IconButton(onClick = { showLegend = true },
                modifier = Modifier.testTag("status-legend").semantics { contentDescription = help }) {
                Text("?", Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium)
            }
        }
        DailyPrayerRows(date, schedule, place.zone, history, onRecord, next?.at, now, onStatus)
    }
    if (showLegend) StatusLegend { showLegend = false }
}

private val prayerRows = listOf(Prayer.FAJR, null, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

@Composable internal fun DailyPrayerRows(date: LocalDate, schedule: DailySchedule?, zone: ZoneId,
    history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit, nextAt: Instant? = null, now: Instant? = null,
    onStatus: ((PrayerKey, PrayerStatus?, PrayerStatus?) -> Unit)? = null) {
    prayerRows.forEach { prayer ->
        val key = prayer?.let { PrayerKey(date, it) }
        val label = prayer?.let { prayerName(it) } ?: stringResource(R.string.sunrise)
        val at = if (prayer == null) schedule?.sunrise else schedule?.prayers?.get(prayer)
        val explicit = key?.let { history?.records?.get(it)?.status }
        val paused = key != null && explicit == null && history?.pauses?.any { date in it } == true
        val status = if (paused) PrayerStatus.EXCUSED else explicit
        val state = when {
            history == null -> stringResource(R.string.not_available)
            paused -> stringResource(R.string.pause)
            else -> statusName(status)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (key == null) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            } else {
                val edit = stringResource(R.string.edit_prayer_status, label)
                TextButton(enabled = history != null, onClick = { onRecord(key) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("record-open-${key.prayer.name.lowercase()}")
                        .semantics { contentDescription = "$label, $state"; onClick(label = edit, action = null) },
                    contentPadding = PaddingValues(end = 2.dp)) {
                    Text(label, Modifier.fillMaxWidth().clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(at?.let { timeLabel(it, zone) } ?: "—", style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr,
                fontFeatureSettings = "tnum"), fontWeight = if (at != null && at == nextAt) FontWeight.Bold else FontWeight.Normal,
                color = if (at != null && at == nextAt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (key != null) {
                val canCycle = onStatus != null && history != null && !paused && at != null && now != null && at <= now
                val next = nextExplicitStatus(explicit)
                val action = if (canCycle) stringResource(R.string.set_prayer_status, statusName(next))
                    else stringResource(R.string.edit_prayer_status, label)
                IconButton(enabled = history != null, onClick = {
                    if (canCycle) onStatus(key, explicit, next) else onRecord(key)
                }, modifier = Modifier.size(48.dp).testTag("record-status-${key.prayer.name.lowercase()}").semantics {
                    contentDescription = label
                    stateDescription = state
                    onClick(label = action, action = null)
                }) {
                    Text(statusSymbol(status), Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium,
                        color = if (explicit != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal fun nextExplicitStatus(status: PrayerStatus?): PrayerStatus? = when (status) {
    null -> PrayerStatus.ON_TIME
    PrayerStatus.ON_TIME -> PrayerStatus.MADE_UP
    PrayerStatus.MADE_UP -> PrayerStatus.MISSED
    PrayerStatus.MISSED -> PrayerStatus.EXCUSED
    PrayerStatus.EXCUSED -> null
}

@Composable private fun StatusLegend(close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.status_legend)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(null) + PrayerStatus.entries).forEach { status ->
                Text("${statusSymbol(status)}  ${statusName(status)}")
            }
            Text(stringResource(R.string.status_legend_instructions))
        } }, confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.close)) } })
}

/** Same content for public synthetic screenshots, without reading private storage. */
@Composable internal fun Today(setup: ManualSetup, clock: Clock = Clock.systemUTC()) = TodayScreen(
    AppPreferences(place = setup), HistorySnapshot(emptyMap(), emptyList(), emptyList()), {}, {}, {}, clock)
