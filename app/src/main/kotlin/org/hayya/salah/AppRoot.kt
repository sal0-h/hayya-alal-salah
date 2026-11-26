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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
                endPause = { pause, day -> perform { app.repository.endPause(pause, day) } },
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
    endPause: (PauseInterval, LocalDate) -> Unit = { _, _ -> },
    initialRoute: String = "today", clock: java.time.Clock = java.time.Clock.systemUTC(),
    scrollState: androidx.compose.foundation.ScrollState? = null) {
    // Reset must forget retained tab state as well as persisted data.
    val screens = key(config.place != null) { rememberSaveableStateHolder() }
    val timetable = config.place?.let { rememberTimetableCache(it) }
    var route by rememberSaveable { mutableStateOf(initialRoute) }
    var locationParent by rememberSaveable { mutableStateOf("settings") }
    var pauseParent by rememberSaveable { mutableStateOf("settings") }
    var historyDay by rememberSaveable { mutableLongStateOf(clock.instant().atZone(config.place?.zone ?: ZoneId.systemDefault()).toLocalDate().toEpochDay()) }
    var setupDraft by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { PreferencesCodec.encode(AppPreferences(place = it)) },
        restore = { PreferencesCodec.decode(it).place }
    )) { mutableStateOf<ManualSetup?>(null) }
    var choosingMethod by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(config.place) {
        if (config.place != null) {
            setupDraft = null
            choosingMethod = false
        }
    }
    val roots = listOf("today", "history", "settings")
    val labels = listOf(R.string.today, R.string.history, R.string.settings)
    LaunchedEffect(requested, config.place) {
        if (requested != null && config.place != null) { route = "history"; historyDay = requested.date.toEpochDay(); onRecord(requested); consumeRequest() }
    }
    fun goBack() {
        if (choosingMethod) choosingMethod = false
        else route = when {
            route == "location" -> locationParent
            route == "pause" -> pauseParent
            route in roots || route == "qibla" -> "today"
            else -> "settings"
        }
    }
    BackHandler(route != "today" || choosingMethod, onBack = ::goBack)
    Scaffold(modifier = Modifier.semantics { testTagsAsResourceId = true }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        if (config.place != null && route in roots) Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    roots.forEachIndexed { index, target ->
                        val selected = route == target
                        val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(Modifier.weight(1f).testTag("nav-$target").selectable(selected,role = Role.Tab,onClick = { route = target })
                            .heightIn(min = 48.dp).padding(horizontal = 2.dp,vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(labels[index]), color = accent, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            androidx.compose.foundation.Canvas(Modifier.size(24.dp,4.dp).clearAndSetSemantics { }) {
                                if (selected) drawLine(accent, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width,0f),strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                }
            }
        }
    }) { padding ->
        val destination: @Composable () -> Unit = {
            val destinationScroll = scrollState ?: rememberScrollState()
            LaunchedEffect(choosingMethod) { if (choosingMethod) destinationScroll.scrollTo(0) }
            val scrolling = if (route == "today" && config.place != null) Modifier else Modifier.verticalScroll(destinationScroll)
            Column(Modifier.padding(padding).fillMaxSize().testTag("screen-$route").semantics { testTagsAsResourceId = true }.then(scrolling).padding(horizontal = 20.dp, vertical = if (route == "today") 4.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (route == "today") 6.dp else 12.dp)) {
                if (route !in roots && config.place != null) TextButton(onClick = ::goBack, modifier = Modifier.heightIn(min = 48.dp).testTag("screen-back")) { Text(stringResource(R.string.back)) }
                if (config.place != null && history == null) OutlinedButton(onClick = retryHistory) { Text(stringResource(R.string.retry)) }
                val place = config.place
                if (place == null) {
                    SectionTitle(stringResource(R.string.app_name))
                    val draft = setupDraft
                    if (!choosingMethod || draft == null) {
                        SelectionField(stringResource(R.string.language), config.language, listOf("system", "en", "ru", "az", "ar"), { languageName(it) }, onLanguage)
                        SectionTitle(stringResource(R.string.choose_location))
                        ManualLocationScreen(draft, firstRun = true) {
                            setupDraft = it
                            choosingMethod = true
                            route = "today"
                        }
                    } else {
                        TextButton(onClick = { choosingMethod = false }, modifier = Modifier.testTag("setup-back")) { Text(stringResource(R.string.back)) }
                        MethodPicker(draft.zone, draft.profile) { setupDraft = draft.copy(calculation = CalculationSettings(it)) }
                        Button(onClick = { savePlace(draft) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("finish-setup")) {
                            Text(stringResource(R.string.confirm_setup))
                        }
                    }
                } else when (route) {
                    "today" -> TodayScreen(config, history, onRecord, update, { route = "qibla" },
                        { locationParent = "today"; route = "location" }, { pauseParent = "today"; route = "pause" }, clock, checkNotNull(timetable), scrollState)
                    "history" -> HistoryScreen(config, history, LocalDate.ofEpochDay(historyDay), { historyDay = it.toEpochDay() }, onRecord, clock, checkNotNull(timetable))
                    "settings" -> {
                        SectionTitle(stringResource(R.string.settings))
                        listOf("location" to R.string.location, "calculation" to R.string.calculation,
                            "alerts" to R.string.prayer_alerts, "fajr" to R.string.fajr_alarm, "focus" to R.string.prayer_focus,
                            "pause" to R.string.pause, "appearance" to R.string.appearance,
                            "privacy" to R.string.privacy_data, "credits" to R.string.credits).forEach { (target, label) ->
                            OutlinedButton(onClick = {
                                if (target == "location") locationParent = "settings"
                                if (target == "pause") pauseParent = "settings"
                                route = target
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings-$target")) { Text(stringResource(label)) }
                        }
                    }
                    "location" -> {
                        SectionTitle(stringResource(R.string.location))
                        ManualLocationScreen(place, save = savePlace)
                    }
                    "calculation" -> CalculationScreen(place, update)
                    "alerts" -> AlertsScreen(config, history, update)
                    "fajr" -> FajrScreen(config, history, update, clock)
                    "focus" -> FocusScreen(config, update)
                    "pause" -> PauseScreen(history, place.zone, addPause, removePause, endPause, clock)
                    "appearance" -> AppearanceScreen(config, update, onLanguage)
                    "privacy" -> PrivacyScreen(deleteRecords, reset)
                    "credits" -> CreditsScreen()
                    "qibla" -> QiblaScreen(place)
                }
            }
        }
        // Forms read current preferences on entry; only the three tabs retain UI state.
        if (config.place != null && route in roots) screens.SaveableStateProvider(route, destination)
        else key(route) { destination() }
    }
}
