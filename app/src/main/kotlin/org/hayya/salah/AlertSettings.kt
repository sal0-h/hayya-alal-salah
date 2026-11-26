// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.hayya.core.*
import org.hayya.salah.alerts.*

@Composable internal fun AlertReadiness(fajr: Boolean, content: @Composable (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { revision++; context.appContainer.refreshIntegrations() }
    val ready = remember(revision, fajr) { AlertNotifications.ready(context, fajr) }
    val exact = remember(revision) { AlarmCoordinator.exactAccess(context) }
    fun open(intent: Intent) { runCatching { context.startActivity(intent) } }
    content(ready && (!fajr || exact))
    if (!ready) {
        Text(stringResource(R.string.alert_not_ready), color = MaterialTheme.colorScheme.error)
        if (Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.notification_access)) }
        OutlinedButton(onClick = { open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(stringResource(R.string.channel_settings)) }
    }
    if (!exact) {
        Text(stringResource(if (fajr) R.string.fajr_exact_required else R.string.exact_not_ready), color = MaterialTheme.colorScheme.error)
        if (Build.VERSION.SDK_INT >= 31) OutlinedButton(onClick = { open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())) }) { Text(stringResource(R.string.exact_access)) }
    }
    if (fajr && Build.VERSION.SDK_INT >= 34 && !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        OutlinedButton(onClick = { open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri())) }) { Text(stringResource(R.string.fullscreen_access)) }
    }
}
@Composable internal fun SoundField(value: String, history: HistorySnapshot?, change: (String) -> Unit) {
    val sounds = listOf("builtin_adhan", "builtin_adhan_2", "builtin_fajr") + history?.audio.orEmpty().map { it.id }
    SelectionField(stringResource(R.string.sound), value, sounds, {
        when (it) { "builtin_adhan" -> stringResource(R.string.builtin_adhan_1); "builtin_adhan_2" -> stringResource(R.string.builtin_adhan_2); "builtin_fajr" -> stringResource(R.string.builtin_fajr)
            else -> history?.audio?.firstOrNull { audio -> audio.id == it }?.name ?: stringResource(R.string.builtin_adhan_1) }
    }, change)
}
@Composable internal fun AlertsScreen(config: AppPreferences, history: HistorySnapshot?, update: PreferencesUpdate) {
    val context = LocalContext.current
    SectionTitle(stringResource(R.string.prayer_alerts))
    var showAccess by rememberSaveable { mutableStateOf(false) }
    if (showAccess || config.alerts.values.any { it.mode != AlertMode.OFF }) AlertReadiness(false)
    Prayer.entries.forEach { prayer ->
        val alert = config.alerts.getValue(prayer)
        Text(prayerName(prayer), style = MaterialTheme.typography.titleLarge)
        SelectionField(stringResource(R.string.prayer_alerts), alert.mode, AlertMode.entries, {
            stringResource(when (it) { AlertMode.OFF -> R.string.alert_off; AlertMode.NOTIFICATION -> R.string.alert_notification; AlertMode.ADHAN -> R.string.alert_adhan })
        }) { mode -> update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(mode = mode))) } }
        if (alert.mode != AlertMode.OFF) {
            if (alert.mode == AlertMode.ADHAN) SoundField(alert.sound, history) { sound ->
                update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(sound = sound))) }
            }
            ExtraReminders(prayer, alert) { before, after ->
                update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(beforeMinutes = before, afterMinutes = after))) }
            }
        }
        HorizontalDivider()
    }
    val testLabel = stringResource(R.string.alert_test)
    OutlinedButton(onClick = {
        AlertNotifications.channels(context)
        if (AlertNotifications.ready(context, false)) context.getSystemService(NotificationManager::class.java).notify("test", 1,
            NotificationCompat.Builder(context, AlertNotifications.PRAYERS).setSmallIcon(R.drawable.ic_notification).setContentTitle(testLabel).setAutoCancel(true).build())
        else showAccess = true
    }) { Text(stringResource(R.string.alert_test)) }
    AudioLibrary(history)
}
@Composable internal fun AudioLibrary(history: HistorySnapshot?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; error = false
            try { AudioImporter.import(context, uri, context.appContainer.repository) } catch (_: Exception) { error = true } finally { busy = false }
        }
    }
    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, enabled = !busy) { Text(stringResource(R.string.import_audio)) }
    if (busy) LoadingIndicator()
    if (error) Text(stringResource(R.string.import_error), color = MaterialTheme.colorScheme.error)
    if (deletionError) Text(stringResource(R.string.audio_delete_error),color = MaterialTheme.colorScheme.error)
    history?.audio.orEmpty().forEach { audio ->
        Text(audio.name)
        TextButton(enabled = !busy,onClick = {
            scope.launch {
                busy = true; deletionError = false
                try { context.appContainer.removeImportedAudio(audio) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { deletionError = true }
                finally { busy = false }
            }
        }) { Text(stringResource(R.string.remove)) }
    }
}
@Composable private fun ExtraReminders(prayer: Prayer, current: PrayerAlert, apply: (Int?, Int?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val before = current.beforeMinutes?.let { stringResource(R.string.minutes_before, numberLabel(it)) }
    val after = current.afterMinutes?.let { stringResource(R.string.minutes_after, numberLabel(it)) }
    val summary = listOfNotNull(before, after).joinToString(" · ").ifEmpty { stringResource(R.string.alert_off) }
    val name = stringResource(R.string.extra_reminders)
    TextButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("edit-reminders-${prayer.name.lowercase()}")) {
        Text("$name · $summary")
    }
    if (editing) ExtraReminderDialog(prayer, current, close = { editing = false }) { b, a ->
        apply(b, a)
        editing = false
    }
}

@Composable private fun ExtraReminderDialog(prayer: Prayer, current: PrayerAlert, close: () -> Unit, apply: (Int?, Int?) -> Unit) {
    var beforeOn by rememberSaveable { mutableStateOf(current.beforeMinutes != null) }
    var afterOn by rememberSaveable { mutableStateOf(current.afterMinutes != null) }
    val beforeDefault = numberLabel(current.beforeMinutes ?: 10)
    val afterDefault = numberLabel(current.afterMinutes ?: 10)
    var before by rememberSaveable { mutableStateOf(beforeDefault) }
    var after by rememberSaveable { mutableStateOf(afterDefault) }
    val b = normalizeNumber(before).toIntOrNull()
    val a = normalizeNumber(after).toIntOrNull()
    val beforeInvalid = beforeOn && b !in 1..120
    val afterInvalid = afterOn && a !in 1..120
    val bounds = stringResource(R.string.minutes_range, numberLabel(1), numberLabel(120))
    AlertDialog(onDismissRequest = close, modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text("${prayerName(prayer)} · ${stringResource(R.string.extra_reminders)}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            ToggleRow(stringResource(R.string.before_reminder), beforeOn) { beforeOn = it }
            if (beforeOn) MinuteInput(before, { before = it }, bounds, beforeInvalid)
            ToggleRow(stringResource(R.string.after_reminder), afterOn) { afterOn = it }
            if (afterOn) MinuteInput(after, { after = it }, bounds, afterInvalid)
            Text(stringResource(R.string.extras_may_delay), style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = !beforeInvalid && !afterInvalid, onClick = {
            apply(b.takeIf { beforeOn }, a.takeIf { afterOn })
        }) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
}

@Composable internal fun MinuteInput(value: String, change: (String) -> Unit, bounds: String, invalid: Boolean) {
    OutlinedTextField(value, change, label = { Text(stringResource(R.string.minutes)) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = invalid, supportingText = if (invalid) ({ Text(bounds) }) else null)
}
