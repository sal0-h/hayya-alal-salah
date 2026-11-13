// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.hayya.core.*
import java.time.LocalDate
import java.time.ZoneId

@Composable internal fun AppRoot(app: AppContainer, requested: PrayerKey?, consumeRequest: () -> Unit) {
    var retry by remember { mutableIntStateOf(0) }
    val prefFlow = remember(app, retry) { app.settings.preferences.map { Result.success(it) }.catch { emit(Result.failure(it)) } }
    val historyFlow = remember(app, retry) { app.repository.changes.map { Result.success(it) }.catch { emit(Result.failure(it)) } }
    val prefsResult by prefFlow.collectAsStateWithLifecycle(initialValue = null)
    val historyResult by historyFlow.collectAsStateWithLifecycle(initialValue = null)
    val config = prefsResult?.getOrNull()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.save_error)
    val context = LocalContext.current
    var recordTarget by remember { mutableStateOf<Pair<PrayerKey, ZoneId>?>(null) }
    var recordBusy by remember { mutableStateOf(false) }
    fun perform(action: suspend () -> Unit) { scope.launch {
        try { action() } catch (e: CancellationException) { throw e } catch (_: Exception) { snackbar.showSnackbar(errorMessage) }
    } }
    val update: PreferencesUpdate = { transform -> perform { app.settings.update(transform) } }
    HayyaTheme(config?.appearance ?: AppearancePreferences()) {
        when {
            prefsResult == null -> Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            config == null -> Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().padding(24.dp)) {
                Text(stringResource(R.string.storage_error))
                Button(onClick = { retry++ }) { Text(stringResource(R.string.retry)) }
            } }
            else -> AppShell(config, historyResult?.getOrNull(), requested, consumeRequest, snackbar,
                onRecord = { key -> recordTarget = key to (config.place?.zone ?: ZoneId.systemDefault()) },
                update = update,
                savePlace = { place -> perform { app.settings.save(place) } },
                addPause = { perform { app.repository.addPause(it) } },
                removePause = { perform { app.repository.deletePause(it) } },
                deleteRecords = { perform { app.repository.deleteRecords() } },
                reset = { perform {
                    app.settings.reset()
                    DeliveryLocale.save(context, "system")
                    org.hayya.salah.focus.PrayerFocusService.releaseOverlay()
                    org.hayya.salah.alerts.AlarmCoordinator.cancelAll(context)
                    org.hayya.salah.widgets.WidgetUpdater.update(context)
                    app.repository.reset()
                    context.filesDir.resolve("audio").deleteRecursively()
                    context.getSharedPreferences("locale-bootstrap", 0).edit { clear() }
                    if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getEmptyLocaleList()
                    else (context as? Activity)?.recreate()
                } },
                onLanguage = { language -> perform {
                    app.settings.update { it.copy(language = language) }
                    DeliveryLocale.save(context, language)
                    context.getSharedPreferences("locale-bootstrap", 0).edit { putString("language", language) }
                    if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales =
                        if (language == "system") LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language)
                    else (context as? Activity)?.recreate()
                } })
        }
        if (config != null) AutomaticLocationRefresh(config) { place -> perform { app.settings.save(place) } }
        recordTarget?.let { (key, zone) -> RecordDialog(key, recordBusy, onSave = { status ->
            recordBusy = true
            perform { try { app.repository.record(key, status, zone); recordTarget = null } finally { recordBusy = false } }
        }, onDismiss = { recordTarget = null }) }
    }
}

@Composable internal fun AppShell(config: AppPreferences, history: HistorySnapshot?, requested: PrayerKey? = null,
    consumeRequest: () -> Unit = {}, snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onRecord: (PrayerKey) -> Unit, update: PreferencesUpdate, savePlace: (ManualSetup) -> Unit,
    addPause: (PauseInterval) -> Unit, removePause: (PauseInterval) -> Unit, deleteRecords: () -> Unit,
    reset: () -> Unit, onLanguage: (String) -> Unit) {
    var route by rememberSaveable { mutableStateOf("today") }
    var historyDay by rememberSaveable { mutableLongStateOf(LocalDate.now(config.place?.zone ?: ZoneId.systemDefault()).toEpochDay()) }
    val roots = listOf("today", "history", "settings")
    val labels = listOf(R.string.today, R.string.history, R.string.settings)
    LaunchedEffect(requested, config.place) {
        if (requested != null && config.place != null) { route = "history"; historyDay = requested.date.toEpochDay(); onRecord(requested); consumeRequest() }
    }
    BackHandler(route !in roots) { route = if (route == "qibla") "today" else "settings" }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        if (config.place != null && route in roots) NavigationBar {
            roots.forEachIndexed { index, target -> NavigationBarItem(selected = route == target, onClick = { route = target },
                icon = { Text(listOf("◈", "▦", "☷")[index], Modifier.clearAndSetSemantics { }) },
                label = { Text(stringResource(labels[index])) }) }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (route !in roots && config.place != null) TextButton(onClick = { route = if (route == "qibla") "today" else "settings" }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back)) }
            val place = config.place
            if (place == null) {
                SectionTitle(stringResource(R.string.app_name))
                SelectionField(stringResource(R.string.language), config.language, listOf("system", "en", "ru", "az", "ar"), { languageName(it) }, onLanguage)
                ManualLocationScreen(null, savePlace)
            } else when (route) {
                "today" -> TodayScreen(config, history, onRecord, update, { route = "qibla" })
                "history" -> HistoryScreen(config, history, LocalDate.ofEpochDay(historyDay), { historyDay = it.toEpochDay() }, onRecord)
                "settings" -> {
                    SectionTitle(stringResource(R.string.settings))
                    listOf("location" to R.string.location, "calculation" to R.string.calculation,
                        "alerts" to R.string.prayer_alerts, "fajr" to R.string.fajr_alarm, "focus" to R.string.prayer_focus,
                        "pause" to R.string.pause, "appearance" to R.string.appearance,
                        "privacy" to R.string.privacy_data, "credits" to R.string.credits).forEach { (target, label) ->
                        OutlinedButton(onClick = { route = target }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(label)) }
                    }
                }
                "location" -> ManualLocationScreen(place, savePlace)
                "calculation" -> CalculationScreen(place, update)
                "alerts" -> AlertsScreen(config, history, update)
                "fajr" -> FajrScreen(config, history, update)
                "focus" -> FocusScreen(config, update)
                "pause" -> PauseScreen(history, place.zone, addPause, removePause)
                "appearance" -> AppearanceScreen(config, update, onLanguage)
                "privacy" -> PrivacyScreen(deleteRecords, reset)
                "credits" -> CreditsScreen()
                "qibla" -> QiblaScreen(place)
            }
        }
    }
}
