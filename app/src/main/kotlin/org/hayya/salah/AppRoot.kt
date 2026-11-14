// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
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
    ReportDrawnWhen { prefsResult != null && (config?.place == null || historyResult != null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.save_error)
    val resetError = stringResource(R.string.reset_error)
    val context = LocalContext.current
    var recordTarget by remember { mutableStateOf<Pair<PrayerKey, ZoneId>?>(null) }
    var recordBusy by remember { mutableStateOf(false) }
    fun perform(message: String = errorMessage, action: suspend () -> Unit) { scope.launch {
        try { action() } catch (e: CancellationException) { throw e } catch (_: Exception) { snackbar.showSnackbar(message) }
    } }
    val update: PreferencesUpdate = { transform -> perform { app.settings.update(transform) } }
    HayyaTheme(config?.appearance ?: remember { DeliveryLocale.appearance(context) }) {
        when {
            (prefsResult == null || config?.place != null && historyResult == null) -> Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { LoadingIndicator() } }
            config == null -> Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().padding(24.dp)) {
                Text(stringResource(R.string.storage_error))
                Button(onClick = { retry++ }) { Text(stringResource(R.string.retry)) }
            } }
            else -> AppShell(config, historyResult?.getOrNull(), requested, consumeRequest, snackbar,
                retryHistory = { retry++ }, onRecord = { key -> recordTarget = key to (config.place?.zone ?: ZoneId.systemDefault()) },
                update = update,
                savePlace = { place -> perform { app.settings.save(place) } },
                addPause = { perform { app.repository.addPause(it) } },
                removePause = { perform { app.repository.deletePause(it) } },
                deleteRecords = { perform { app.repository.deleteRecords() } },
                reset = { perform(resetError) {
                    app.resetLocalData()
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
    reset: () -> Unit, onLanguage: (String) -> Unit, retryHistory: () -> Unit = {},
    initialRoute: String = "today", clock: java.time.Clock = java.time.Clock.systemUTC(),
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()) {
    var route by rememberSaveable { mutableStateOf(initialRoute) }
    var historyDay by rememberSaveable { mutableLongStateOf(clock.instant().atZone(config.place?.zone ?: ZoneId.systemDefault()).toLocalDate().toEpochDay()) }
    LaunchedEffect(route) { scrollState.scrollTo(0) }
    val roots = listOf("today", "history", "settings")
    val labels = listOf(R.string.today, R.string.history, R.string.settings)
    LaunchedEffect(requested, config.place) {
        if (requested != null && config.place != null) { route = "history"; historyDay = requested.date.toEpochDay(); onRecord(requested); consumeRequest() }
    }
    BackHandler(route != "today") { route = if (route in roots || route == "qibla") "today" else "settings" }
    Scaffold(modifier = Modifier.semantics { testTagsAsResourceId = true }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        if (config.place != null && route in roots) Surface {
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
                roots.forEachIndexed { index, target ->
                    val selected = route == target
                    val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).testTag("nav-$target").selectable(selected,role = Role.Tab,onClick = { route = target })
                        .heightIn(min = 64.dp).padding(horizontal = 4.dp,vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(labels[index]), color = accent, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        androidx.compose.foundation.Canvas(Modifier.size(24.dp,4.dp).clearAndSetSemantics { }) {
                            if (selected) drawLine(accent, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width,0f),strokeWidth = 2.dp.toPx())
                        }
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().testTag("screen-$route").semantics { testTagsAsResourceId = true }.verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (route !in roots && config.place != null) TextButton(onClick = { route = if (route == "qibla") "today" else "settings" }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back)) }
            if (config.place != null && history == null) OutlinedButton(onClick = retryHistory) { Text(stringResource(R.string.retry)) }
            val place = config.place
            if (place == null) {
                SectionTitle(stringResource(R.string.app_name))
                SelectionField(stringResource(R.string.language), config.language, listOf("system", "en", "ru", "az", "ar"), { languageName(it) }, onLanguage)
                ManualLocationScreen(null, savePlace)
            } else when (route) {
                "today" -> TodayScreen(config, history, onRecord, update, { route = "qibla" }, clock)
                "history" -> HistoryScreen(config, history, LocalDate.ofEpochDay(historyDay), { historyDay = it.toEpochDay() }, onRecord, clock)
                "settings" -> {
                    SectionTitle(stringResource(R.string.settings))
                    listOf("location" to R.string.location, "calculation" to R.string.calculation,
                        "alerts" to R.string.prayer_alerts, "fajr" to R.string.fajr_alarm, "focus" to R.string.prayer_focus,
                        "pause" to R.string.pause, "appearance" to R.string.appearance,
                        "privacy" to R.string.privacy_data, "credits" to R.string.credits).forEach { (target, label) ->
                        OutlinedButton(onClick = { route = target }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings-$target")) { Text(stringResource(label)) }
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
