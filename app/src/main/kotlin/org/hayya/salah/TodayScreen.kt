// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.hayya.core.*
import java.time.*
import java.time.format.FormatStyle

private val firstTimetableDay = LocalDate.of(1900, 1, 1).toEpochDay()
private val lastTimetablePage = (LocalDate.of(2100, 12, 31).toEpochDay() - firstTimetableDay).toInt()
private fun timetablePage(day: Long) = (day - firstTimetableDay).coerceIn(0L, lastTimetablePage.toLong()).toInt()
private fun timetableDay(page: Int) = LocalDate.ofEpochDay(firstTimetableDay + page)

@Composable internal fun TodayScreen(config: AppPreferences, history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit,
    update: PreferencesUpdate, onQibla: () -> Unit, onLocation: () -> Unit, onPause: () -> Unit, clock: Clock = Clock.systemUTC(),
    timetable: TimetableCache, scrollState: ScrollState? = null) {
    val place = config.place ?: return
    val now = foregroundInstant(clock)
    val today = now.atZone(place.zone).toLocalDate()
    val todayPage = timetablePage(today.toEpochDay())
    val pager = rememberPagerState(initialPage = todayPage) { lastTimetablePage + 1 }
    var lastTodayPage by rememberSaveable { mutableIntStateOf(todayPage) }
    LaunchedEffect(todayPage) {
        val followedToday = pager.settledPage == lastTodayPage && !pager.isScrollInProgress
        if (followedToday && lastTodayPage != todayPage) pager.scrollToPage(todayPage)
        lastTodayPage = todayPage
    }

    val scope = rememberCoroutineScope()
    val reducedMotion = config.appearance.reducedMotion
    fun selectPage(page: Int) {
        if (page !in 0..lastTimetablePage) return
        scope.launch {
            if (reducedMotion || kotlin.math.abs(page - pager.currentPage) > 1) pager.scrollToPage(page)
            else pager.animateScrollToPage(page, animationSpec = tween(240))
        }
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showLegend by rememberSaveable { mutableStateOf(false) }
    // Prefetch calculations without composing an entire offscreen timetable.
    LaunchedEffect(timetable, pager.settledPage) {
        val date = timetableDay(pager.settledPage)
        for (offset in listOf(0L, -1L, 1L, 2L)) timetable.load(date.plusDays(offset))
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onLocation, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("today-location"),
            shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onQibla, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.qibla)) }
    }
    val selectedDate = if (pager.settledPage == todayPage && today.year !in 1900..2100) today else timetableDay(pager.settledPage)
    val previousLabel = stringResource(R.string.previous_day)
    val nextLabel = stringResource(R.string.next_day)
    val hijri = remember(selectedDate, config.hijriAdjustment) {
        runCatching { ummAlQuraDisplay(selectedDate, config.hijriAdjustment) }.getOrNull()
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { selectPage(pager.settledPage - 1) }, enabled = pager.settledPage > 0,
            modifier = Modifier.size(48.dp).testTag("today-previous")) {
            Icon(painterResource(R.drawable.ic_chevron_previous), previousLabel)
        }
        TextButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            .testTag("today-date").semantics {
                customActions = buildList {
                    if (pager.settledPage > 0) add(CustomAccessibilityAction(previousLabel) { selectPage(pager.settledPage - 1); true })
                    if (pager.settledPage < lastTimetablePage) add(CustomAccessibilityAction(nextLabel) { selectPage(pager.settledPage + 1); true })
                }
            }, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateLabel(selectedDate, FormatStyle.LONG), style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (hijri != null) Text(hijriLabel(hijri), style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = { selectPage(pager.settledPage + 1) }, enabled = pager.settledPage < lastTimetablePage,
            modifier = Modifier.size(48.dp).testTag("today-next")) {
            Icon(painterResource(R.drawable.ic_chevron_next), nextLabel)
        }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f).testTag("today-pager"),
        pageSpacing = 12.dp,
        verticalAlignment = Alignment.Top,
        flingBehavior = if (reducedMotion) PagerDefaults.flingBehavior(pager,
            snapAnimationSpec = snap(), snapPositionalThreshold = .25f)
        else PagerDefaults.flingBehavior(pager, snapPositionalThreshold = .25f)) { page ->
        // Keep the actual date visible even if the user's device date is outside the calculation range.
        val date = if (page == todayPage && (today.year !in 1900..2100)) today else timetableDay(page)
        val active = page == pager.currentPage
        val accessibility = if (active) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() }
        val dayScroll = if (active && scrollState != null) scrollState else rememberScrollState()
        Column(Modifier.fillMaxSize().then(accessibility).verticalScroll(dayScroll).testTag("today-page-scroll"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TodayPage(config, place, date, today, now, history, onRecord, update, onPause, timetable,
                reportDrawn = active, onLegend = { showLegend = true }, onToday = { selectPage(todayPage) })
        }
    }
    }
    if (showDatePicker) PrayerDatePicker(timetableDay(pager.settledPage), onDismiss = { showDatePicker = false }) { picked ->
        selectPage(timetablePage(picked.toEpochDay()))
        showDatePicker = false
    }
    if (showLegend) StatusLegend { showLegend = false }
}

@Composable private fun TodayPage(config: AppPreferences, place: ManualSetup, date: LocalDate, today: LocalDate, now: Instant,
    history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit, update: PreferencesUpdate, onPause: () -> Unit,
    cache: TimetableCache, reportDrawn: Boolean, onLegend: () -> Unit, onToday: () -> Unit) {
    val live = date == today
    val showNight = config.appearance.showMidnight || config.appearance.showLastThird
    val results by key(cache, date, live, showNight) {
        val offsets = if (live) -1L..1L else if (showNight) 0L..1L else 0L..0L
        val days = offsets.map(date::plusDays)
        val cached = days.mapNotNull { day -> cache.peek(day)?.let { day to it } }.toMap()
            .takeIf { it.size == days.size }.orEmpty()
        produceState(cached) {
            if (cached.size == days.size) return@produceState
            // Publish a complete service-date window: yesterday's Isha can follow midnight.
            value = days.associateWith { day -> cache.load(day) }
        }
    }
    val result = results[date]
    if (reportDrawn) ReportDrawnWhen { result != null }
    val schedule = (result as? CalculationResult.Available)?.schedule
    val next = if (live) nextPrayer(
        results.values.mapNotNull { (it as? CalculationResult.Available)?.schedule }, now) else null
    if (!live) TextButton(onClick = onToday, modifier = Modifier.testTag("today-return")) {
        Text(stringResource(R.string.return_today))
    }
    if (result == null) Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
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
    if (history?.pauses?.any { date in it } == true) TextButton(onClick = onPause, contentPadding = PaddingValues(end = 8.dp)) {
        Text(stringResource(R.string.paused_today))
    }
    if (history == null) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SectionTitle(if (live) stringResource(R.string.today) else dateLabel(date, FormatStyle.FULL))
            }
            val help = stringResource(R.string.status_legend)
            IconButton(onClick = onLegend,
                modifier = Modifier.testTag("status-legend").semantics { contentDescription = help }) {
                Text("?", Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium)
            }
        }
        DailyPrayerRows(date, schedule, place.zone, history, onRecord, next?.at, editable = date <= today)
        if (showNight && result != null && date.plusDays(1) in results) {
            val morning = (results[date.plusDays(1)] as? CalculationResult.Available)?.schedule
            val estimated = schedule?.provenance?.quality == CalculationQuality.ESTIMATED_REFERENCE
            fun at(event: NightTime) = if (schedule != null && morning != null) nightTime(schedule, morning, event) else null
            if (config.appearance.showMidnight) NightTimeRow(R.string.night_midpoint, at(NightTime.MIDPOINT), date, place.zone,
                estimated, Modifier.testTag("today-night-midnight"))
            if (config.appearance.showLastThird) NightTimeRow(R.string.last_third, at(NightTime.LAST_THIRD), date, place.zone,
                estimated, Modifier.testTag("today-night-last-third"))
        }
    }
}

@Composable private fun NightTimeRow(label: Int, at: Instant?, evening: LocalDate, zone: ZoneId, estimated: Boolean, modifier: Modifier) {
    val rounded = at?.plusSeconds(30)
    val time = rounded?.let { timeLabel(it, zone) }
    val localDate = rounded?.atZone(zone)?.toLocalDate()?.takeUnless { it == evening }
    TimetableRow(
        time = if (time != null && estimated) "≈ $time" else time ?: "—",
        next = false, modifier = modifier, timeDate = localDate,
        timeDescription = when {
            time == null -> stringResource(R.string.not_available)
            estimated -> stringResource(R.string.estimated_clock_time, time)
            else -> null
        }
    ) { textModifier ->
        Text(stringResource(label), textModifier, style = MaterialTheme.typography.titleMedium)
    }
}

private val prayerRows = listOf(Prayer.FAJR, null, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

@Composable internal fun DailyPrayerRows(date: LocalDate, schedule: DailySchedule?, zone: ZoneId,
    history: HistorySnapshot?, onRecord: (PrayerKey) -> Unit, nextAt: Instant? = null, editable: Boolean = true) = Column {
    prayerRows.forEach { prayer ->
        val key = prayer?.let { PrayerKey(date, it) }
        val label = prayer?.let { prayerName(it) } ?: stringResource(R.string.sunrise)
        val at = if (prayer == null) schedule?.sunrise else schedule?.prayers?.get(prayer)
        val explicit = key?.let { history?.records?.get(it)?.status }
        val paused = key != null && explicit == null && history?.pauses?.any { date in it } == true
        val state = when {
            history == null -> stringResource(R.string.not_available)
            paused -> stringResource(R.string.paused_today)
            else -> statusName(explicit)
        }
        val isNext = at != null && at == nextAt
        TimetableRow(time = at?.let { timeLabel(it, zone) } ?: "—", next = isNext, status = {
            if (key != null && editable) {
                val action = stringResource(R.string.edit_prayer_status, label)
                IconButton(enabled = history != null, onClick = { onRecord(key) },
                    modifier = Modifier.size(48.dp).testTag("record-status-${key.prayer.name.lowercase()}").semantics {
                        contentDescription = label
                        stateDescription = state
                        onClick(label = action, action = null)
                    }) {
                    Text(if (paused) "Ⅱ" else statusSymbol(explicit), Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (explicit != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else EmptyTimetableStatus()
        }) { textModifier ->
            if (key == null || !editable) {
                Text(label, textModifier, style = MaterialTheme.typography.titleMedium)
            } else {
                val edit = stringResource(R.string.edit_prayer_status, label)
                TextButton(enabled = history != null, onClick = { onRecord(key) },
                    modifier = textModifier.heightIn(min = 48.dp).testTag("record-open-${key.prayer.name.lowercase()}")
                        .semantics { contentDescription = "$label, $state"; onClick(label = edit, action = null) },
                    contentPadding = PaddingValues(end = 2.dp)) {
                    Text(label, Modifier.fillMaxWidth().clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable private fun TimetableRow(time: String, next: Boolean, modifier: Modifier = Modifier, timeDate: LocalDate? = null,
    timeDescription: String? = null, status: @Composable () -> Unit = { EmptyTimetableStatus() },
    label: @Composable (Modifier) -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        label(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(time, modifier = if (timeDescription == null) Modifier else Modifier.clearAndSetSemantics {
                contentDescription = timeDescription
            }, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
                fontWeight = if (next) FontWeight.Bold else FontWeight.Normal,
                color = if (next) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (timeDate != null) Text(dateLabel(timeDate, FormatStyle.SHORT), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun EmptyTimetableStatus() {
    // The optional rows and Sunrise can use the record-control space at large text sizes.
    if (LocalConfiguration.current.fontScale < 1.5f) Spacer(Modifier.width(48.dp))
}

@Composable private fun StatusLegend(close: () -> Unit) {
    AlertDialog(onDismissRequest = close,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text(stringResource(R.string.status_legend)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(null) + PrayerStatus.entries).forEach { status ->
                Text("${statusSymbol(status)}  ${statusName(status)}")
            }
            Text(stringResource(R.string.status_legend_instructions))
        } }, confirmButton = {
            TextButton(onClick = close, modifier = Modifier.testTag("status-legend-close")) { Text(stringResource(R.string.close)) }
        })
}

/** Same content for public synthetic screenshots, without reading private storage. */
@Composable internal fun Today(setup: ManualSetup, clock: Clock = Clock.systemUTC()) = TodayScreen(
    AppPreferences(place = setup), HistorySnapshot(emptyMap(), emptyList(), emptyList()), {}, {}, {}, {}, {}, clock, rememberTimetableCache(setup))
