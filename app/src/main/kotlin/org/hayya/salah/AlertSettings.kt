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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.hayya.core.*
import org.hayya.salah.alerts.*
import java.time.Instant
import java.time.LocalDate

@Composable internal fun AlertReadiness(fajr: Boolean) {
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
    if (!ready) {
        Text(stringResource(R.string.alert_not_ready), color = MaterialTheme.colorScheme.error)
        if (Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.notification_access)) }
        OutlinedButton(onClick = { open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(stringResource(R.string.channel_settings)) }
    }
    if (!exact) {
        Text(stringResource(R.string.exact_not_ready), color = MaterialTheme.colorScheme.error)
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
    AlertReadiness(false)
    Text(stringResource(R.string.alert_delivery_note))
    Text(stringResource(R.string.normal_sound_note))
    Prayer.entries.forEach { prayer ->
        val alert = config.alerts.getValue(prayer)
        Text(prayerName(prayer), style = MaterialTheme.typography.titleLarge)
        SelectionField(stringResource(R.string.prayer_alerts), alert.mode, AlertMode.entries, {
            stringResource(when (it) { AlertMode.OFF -> R.string.alert_off; AlertMode.NOTIFICATION -> R.string.alert_notification; AlertMode.ADHAN -> R.string.alert_adhan })
        }) { mode -> update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(mode = mode))) } }
        if (alert.mode != AlertMode.OFF) {
            SoundField(alert.sound, history) { sound -> update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(sound = sound))) } }
            var before by rememberSaveable(alert.beforeMinutes) { mutableStateOf(alert.beforeMinutes?.toString().orEmpty()) }
            var after by rememberSaveable(alert.afterMinutes) { mutableStateOf(alert.afterMinutes?.toString().orEmpty()) }
            var invalid by remember { mutableStateOf(false) }
            OutlinedTextField(before, { before = it }, label = { Text(stringResource(R.string.before_minutes)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(after, { after = it }, label = { Text(stringResource(R.string.after_minutes)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = {
                val b = normalizeNumber(before).takeIf { it.isNotBlank() }?.toIntOrNull()
                val a = normalizeNumber(after).takeIf { it.isNotBlank() }?.toIntOrNull()
                invalid = (before.isNotBlank() && b !in 1..120) || (after.isNotBlank() && a !in 1..120)
                if (!invalid) update { it.copy(alerts = it.alerts + (prayer to it.alerts.getValue(prayer).copy(beforeMinutes = b, afterMinutes = a))) }
            }) { Text(stringResource(R.string.save)) }
        }
        HorizontalDivider()
    }
    val testLabel = stringResource(R.string.alert_test)
    OutlinedButton(onClick = {
        AlertNotifications.channels(context)
        if (AlertNotifications.ready(context, false)) context.getSystemService(NotificationManager::class.java).notify("test", 1,
            NotificationCompat.Builder(context, AlertNotifications.PRAYERS).setSmallIcon(R.drawable.ic_notification).setContentTitle(testLabel).setAutoCancel(true).build())
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
    Text(stringResource(R.string.import_note))
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
@Composable internal fun FajrScreen(config: AppPreferences, history: HistorySnapshot?, update: PreferencesUpdate) {
    val context = LocalContext.current
    val prefs = config.fajr
    SectionTitle(stringResource(R.string.fajr_alarm))
    AlertReadiness(true)
    Text(stringResource(R.string.normal_sound_note))
    ToggleRow(stringResource(R.string.enable_fajr), prefs.enabled) { enabled -> update { it.copy(fajr = it.fajr.copy(enabled = enabled)) } }
    var offset by rememberSaveable(prefs.offsetMinutes) { mutableStateOf(prefs.offsetMinutes.toString()) }
    var invalid by remember { mutableStateOf(false) }
    OutlinedTextField(offset, { offset = it }, label = { Text(stringResource(R.string.fajr_offset)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = { val value = normalizeNumber(offset).toIntOrNull(); invalid = value !in -180..180
        if (!invalid) update { it.copy(fajr = it.fajr.copy(offsetMinutes = value!!)) }
    }) { Text(stringResource(R.string.save)) }
    SoundField(prefs.sound, history) { sound -> update { it.copy(fajr = it.fajr.copy(sound = sound)) } }
    SelectionField(stringResource(R.string.challenge), prefs.difficulty, ChallengeDifficulty.entries, {
        stringResource(when (it) { ChallengeDifficulty.OFF -> R.string.alert_off; ChallengeDifficulty.EASY -> R.string.challenge_easy; ChallengeDifficulty.MEDIUM -> R.string.challenge_medium; ChallengeDifficulty.HARD -> R.string.challenge_hard })
    }) { difficulty -> update { it.copy(fajr = it.fajr.copy(difficulty = difficulty)) } }
    Text(stringResource(R.string.dismiss_not_record))
    OutlinedButton(onClick = {
        if (AlertNotifications.ready(context, true)) {
            val place = config.place ?: return@OutlinedButton
            val entry = AlarmEntry("test-fajr", PrayerKey(LocalDate.now(place.zone), Prayer.FAJR), EventKind.FAJR_WAKE_UP,
                Instant.now(), place.zone.id, prefs.sound, prefs.difficulty)
            AlarmPlayback.start(context, entry); AlertNotifications.ringing(context, entry).send()
        }
    }) { Text(stringResource(R.string.alarm_test)) }
}
