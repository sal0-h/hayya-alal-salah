// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.*
import org.hayya.salah.alerts.*
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.format.FormatStyle
import kotlin.math.abs

@Composable internal fun FajrScreen(config: AppPreferences, history: HistorySnapshot?, update: PreferencesUpdate,
    clock: Clock = Clock.systemUTC()) {
    val context = LocalContext.current
    val prefs = config.fajr
    val place = config.place ?: return
    var showAccess by rememberSaveable { mutableStateOf(false) }
    var editOffset by rememberSaveable { mutableStateOf(false) }
    var editClock by rememberSaveable { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.fajr_alarm))
    ToggleRow(stringResource(R.string.enable_fajr), prefs.enabled) { enabled ->
        update { it.copy(fajr = it.fajr.copy(enabled = enabled)) }
    }
    if (prefs.enabled || showAccess) AlertReadiness(true) { ready ->
        if (prefs.enabled) NextFajrAlarm(place, prefs, history, ready, clock)
    }
    SelectionField(stringResource(R.string.alarm_timing), prefs.mode, FajrMode.entries, {
        stringResource(if (it == FajrMode.RELATIVE) R.string.fajr_relative else R.string.fajr_fixed)
    }) { mode -> update { it.copy(fajr = it.fajr.copy(mode = mode)) } }

    if (prefs.mode == FajrMode.RELATIVE) {
        val minutes = prefs.offsetMinutes
        val offset = if (minutes == 0) stringResource(R.string.at_fajr)
            else stringResource(if (minutes < 0) R.string.minutes_before else R.string.minutes_after, numberLabel(abs(minutes)))
        OutlinedButton(onClick = { editOffset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("fajr-offset")) {
            Text(offset)
        }
        if (editOffset) FajrOffsetDialog(minutes, close = { editOffset = false }) { value ->
            update { it.copy(fajr = it.fajr.copy(offsetMinutes = value)) }
            editOffset = false
        }
    } else {
        OutlinedButton(onClick = { editClock = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("fajr-fixed-clock")) {
            Text(wallTimeLabel(prefs.fixedTime))
        }
        Text(stringResource(R.string.fajr_location_zone, place.name), style = MaterialTheme.typography.bodySmall)
        if (editClock) FajrClockPicker(prefs.fixedTime, close = { editClock = false }) { time ->
            update { it.copy(fajr = it.fajr.copy(fixedTime = time)) }
        }
    }
    SoundField(prefs.sound, history) { sound -> update { it.copy(fajr = it.fajr.copy(sound = sound)) } }
    SelectionField(stringResource(R.string.challenge), prefs.difficulty, ChallengeDifficulty.entries, {
        stringResource(when (it) { ChallengeDifficulty.OFF -> R.string.alert_off; ChallengeDifficulty.EASY -> R.string.challenge_easy
            ChallengeDifficulty.MEDIUM -> R.string.challenge_medium; ChallengeDifficulty.HARD -> R.string.challenge_hard })
    }) { difficulty -> update { it.copy(fajr = it.fajr.copy(difficulty = difficulty)) } }
    OutlinedButton(onClick = {
        if (!AlertNotifications.ready(context, true)) showAccess = true
        else {
            val now = clock.instant()
            val entry = AlarmEntry("test-fajr", PrayerKey(now.atZone(place.zone).toLocalDate(), Prayer.FAJR), EventKind.FAJR_WAKE_UP,
                now, place.zone.id, prefs.sound, prefs.difficulty)
            AlarmPlayback.start(context, entry)
            AlertNotifications.ringing(context, entry).send()
        }
    }) { Text(stringResource(R.string.alarm_test)) }
}

@Composable private fun NextFajrAlarm(place: ManualSetup, prefs: FajrPreferences, history: HistorySnapshot?, ready: Boolean, clock: Clock) {
    val now = foregroundInstant(clock)
    val localDate = now.atZone(place.zone).toLocalDate()
    val dates = remember(localDate, place.zone) { alarmDates(now, place.zone) }
    val relative = prefs.mode == FajrMode.RELATIVE
    val calculated by key(place, dates, relative) {
        produceState<Map<java.time.LocalDate, Instant>?>(if (relative) null else emptyMap()) {
            if (relative) value = withContext(Dispatchers.Default) { fajrByDate(calculateAlarmDays(place, dates)) }
        }
    }
    val fajr = calculated
    val next = if (fajr != null && history != null)
        compileFajrAlarms(dates, place.zone, prefs.timing, fajr, history.records, history.pauses, now).firstOrNull() else null
    Column(Modifier.testTag("next-fajr-alarm").semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(if (ready) R.string.next_alarm else R.string.configured_alarm_time), style = MaterialTheme.typography.labelLarge)
        if (next != null) {
            val at = next.at
            Text(stringResource(R.string.dated_clock_time, dateLabel(at.atZone(place.zone).toLocalDate(), FormatStyle.FULL),
                timeLabel(at, place.zone)), style = MaterialTheme.typography.titleMedium)
        } else Text(stringResource(when {
            history == null -> R.string.storage_error
            fajr == null -> R.string.loading
            relative && dates.drop(1).none(fajr::containsKey) -> R.string.fajr_times_unavailable
            else -> R.string.no_upcoming_fajr_alarm
        }), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun FajrOffsetDialog(current: Int, close: () -> Unit, apply: (Int) -> Unit) {
    var direction by rememberSaveable { mutableIntStateOf(current.compareTo(0)) }
    val initial = numberLabel(abs(current).takeIf { it > 0 } ?: 15)
    var minutes by rememberSaveable { mutableStateOf(initial) }
    val value = normalizeNumber(minutes).toIntOrNull()
    val invalid = direction != 0 && value !in 1..180
    AlertDialog(onDismissRequest = close,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        title = { Text(stringResource(R.string.fajr_relative)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            SelectionField(stringResource(R.string.alarm_timing), direction, listOf(-1, 0, 1), {
                stringResource(when (it) { -1 -> R.string.before_fajr; 1 -> R.string.after_fajr; else -> R.string.at_fajr })
            }) { direction = it }
            if (direction != 0) MinuteInput(minutes, { minutes = it },
                stringResource(R.string.minutes_range, numberLabel(1), numberLabel(180)), invalid)
        } },
        confirmButton = { TextButton(enabled = !invalid, onClick = { apply(direction * (value ?: 0)) }) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun FajrClockPicker(time: LocalTime, close: () -> Unit, apply: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val use24 = uses24HourClock()
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val commit by rememberUpdatedState(apply)
    val dismiss by rememberUpdatedState(close)
    DisposableEffect(context, use24, dark) {
        val theme = if (dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
        val dialog = TimePickerDialog(context, theme, { _, hour, minute -> commit(LocalTime.of(hour, minute)) }, time.hour, time.minute, use24)
        dialog.setOnDismissListener { dismiss() }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    }
}
