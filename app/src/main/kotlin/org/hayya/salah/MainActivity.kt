// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SetupStore(this)
        setContent { HayyaTheme { HayyaApp(store) } }
    }
}

@Composable internal fun HayyaTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme(
        background = Color(0xFF101D26), surface = Color(0xFF101D26),
        onBackground = Color(0xFFE4EDF2), onSurface = Color(0xFFE4EDF2),
        onSurfaceVariant = Color(0xFFA9B9C5), primary = Color(0xFF76C9CF), outlineVariant = Color(0xFF354A59),
    ) else lightColorScheme(
        background = Color(0xFFF8FAFC), surface = Color(0xFFF8FAFC),
        onBackground = Color(0xFF172D3A), onSurface = Color(0xFF172D3A),
        onSurfaceVariant = Color(0xFF526875), primary = Color(0xFF086D79), outlineVariant = Color(0xFFCFD9DF),
    )
    val arabic = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "ar"
    val family = if (arabic) FontFamily(Font(R.font.plex_arabic_regular), Font(R.font.plex_arabic_medium, FontWeight.Medium))
        else FontFamily(Font(R.font.plex_sans_regular), Font(R.font.plex_sans_medium, FontWeight.Medium))
    val base = Typography()
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

@Composable private fun HayyaApp(store: SetupStore) {
    val scope = rememberCoroutineScope()
    var readAttempt by remember { mutableIntStateOf(0) }
    val stateFlow = remember(store, readAttempt) { store.state() }
    val state by stateFlow.collectAsStateWithLifecycle(initialValue = SetupState.Loading)
    val setup = (state as? SetupState.Ready)?.setup
    var destination by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        if (state == SetupState.Loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (state == SetupState.ReadFailure) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.read_error))
                Button(onClick = { readAttempt++ }) { Text(stringResource(R.string.retry)) }
            }
        } else if (setup == null || editing) {
            SetupScreen(setup, saving, saveError, onCancel = if (setup != null) ({ editing = false }) else null) { value ->
                saving = true; saveError = false
                scope.launch {
                    try { store.save(value); editing = false }
                    catch (_: java.io.IOException) { saveError = true }
                    finally { saving = false }
                }
            }
        } else {
            val selected = setup
            Scaffold(bottomBar = {
                NavigationBar {
                    listOf(R.string.today, R.string.history, R.string.settings).forEachIndexed { i, label ->
                        NavigationBarItem(selected = destination == i, onClick = { destination = i },
                            icon = { Text(listOf("◈", "▦", "☷")[i], Modifier.clearAndSetSemantics { }, fontSize = 24.sp) },
                            label = { Text(stringResource(label)) })
                    }
                }
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    when (destination) {
                        0 -> Today(selected)
                        1 -> { Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineLarge)
                            Text(stringResource(R.string.history_pending), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        2 -> {
                            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
                            Text(selected.name, style = MaterialTheme.typography.titleLarge)
                            Text(selected.zone.id)
                            Text(profileName(selected.profile))
                            OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.change_setup))
                            }
                            Text(stringResource(R.string.internal_scope), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.privacy), style = MaterialTheme.typography.bodyLarge)
                            if (saveError) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { scope.launch {
                                try { store.clear(); saveError = false }
                                catch (_: java.io.IOException) { saveError = true }
                            } }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.delete_setup))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SetupScreen(initial: ManualSetup?, saving: Boolean, saveError: Boolean,
    onCancel: (() -> Unit)?, onSave: (ManualSetup) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(initial?.point?.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(initial?.point?.longitude?.toString().orEmpty()) }
    var zone by rememberSaveable { mutableStateOf(initial?.zone?.id ?: ZoneId.systemDefault().id) }
    var profile by rememberSaveable { mutableStateOf(initial?.profile ?: Profile.MWL) }
    var expanded by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val candidate = runCatching {
        require(name.isNotBlank())
        ManualSetup(name.trim(), GeoPoint(latitude.trim().toDouble(), longitude.trim().toDouble()), ZoneId.of(zone.trim()), profile)
    }.getOrNull()
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.place_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(latitude, { latitude = it }, label = { Text(stringResource(R.string.latitude)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(longitude, { longitude = it }, label = { Text(stringResource(R.string.longitude)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(zone, { zone = it }, label = { Text(stringResource(R.string.timezone)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text(stringResource(R.string.profile_explanation), style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(profileName(profile)) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                Profile.entries.forEach { p -> DropdownMenuItem(text = { Text(profileName(p)) }, onClick = { profile = p; expanded = false }) }
            }
        }
        if (attempted && candidate == null) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = { attempted = true; candidate?.let(onSave) }, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.confirm_setup)) }
        onCancel?.let { TextButton(onClick = it) { Text(stringResource(R.string.cancel)) } }
    }
}

@Composable internal fun Today(setup: ManualSetup, clock: java.time.Clock = java.time.Clock.systemUTC()) {
    var now by remember { mutableStateOf(clock.instant()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Countdown exists only while this destination is visible and the activity is started.
    LaunchedEffect(setup, lifecycle, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = clock.instant(); delay(30_000) }
        }
    }
    val date = now.atZone(setup.zone).toLocalDate()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val schedules by produceState<List<DailySchedule>?>(null, setup, date) {
        value = withContext(Dispatchers.Default) {
            val calculator = LocalPrayerCalculator()
            (-1L..1L).mapNotNull { offset ->
                (calculator.calculate(date.plusDays(offset), setup.point, setup.zone,
                    CalculationSettings(setup.profile)) as? CalculationResult.Available)?.schedule
            }
        }
    }
    val today = schedules?.firstOrNull { it.date == date }
    val next = nextPrayer(schedules.orEmpty(), now)
    Text(setup.name, style = MaterialTheme.typography.headlineSmall)
    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        .withDecimalStyle(java.time.format.DecimalStyle.of(locale))), color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (schedules == null) CircularProgressIndicator()
    else if (today == null) {
        Text(stringResource(R.string.unavailable), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.unavailable_detail))
    } else {
        if (next != null) {
            NextPrayerMarker(prayerName(next.key.prayer), timeLabel(next.at, setup.zone))
            val minutes = Duration.between(now, next.at).toMinutes().coerceAtLeast(0)
            Text(stringResource(R.string.remaining, minutes / 60, minutes % 60), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.alignCenter())
        } else Text(stringResource(R.string.next_unavailable))
        Text(stringResource(R.string.today), style = MaterialTheme.typography.titleMedium)
        val rows = listOf(Prayer.FAJR to today.prayers.getValue(Prayer.FAJR), null to today.sunrise) +
            Prayer.entries.drop(1).map { it to today.prayers.getValue(it) }
        rows.forEach { (prayer, at) ->
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (prayer == null) stringResource(R.string.sunrise) else prayerName(prayer),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(timeLabel(at, setup.zone), style = MaterialTheme.typography.titleMedium.copy(
                        textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
                        fontWeight = if (at == next?.at) FontWeight.Bold else FontWeight.Normal)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Text(profileName(setup.profile), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.internal_scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.alignCenter() = this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)

@Composable private fun NextPrayerMarker(name: String, time: String) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().heightIn(min = 216.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().clearAndSetSemantics { }) {
            // An open eight-sided time frame; the upper split gives the marker a clear direction.
            val inset = 8.dp.toPx(); val cut = 28.dp.toPx(); val w = size.width; val h = size.height
            val path = Path().apply {
                moveTo(w * .43f, inset); lineTo(cut, inset); lineTo(inset, cut)
                lineTo(inset, h - cut); lineTo(cut, h - inset); lineTo(w - cut, h - inset)
                lineTo(w - inset, h - cut); lineTo(w - inset, cut); lineTo(w - cut, inset); lineTo(w * .57f, inset)
            }
            drawPath(path, accent, style = Stroke(1.dp.toPx()))
            val diamond = Path().apply { moveTo(w / 2, 0f); lineTo(w / 2 + 6.dp.toPx(), inset)
                lineTo(w / 2, inset * 2); lineTo(w / 2 - 6.dp.toPx(), inset); close() }
            drawPath(diamond, accent)
        }
        Column(Modifier.padding(horizontal = 32.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.next_prayer), style = MaterialTheme.typography.labelLarge, color = accent, textAlign = TextAlign.Center)
            Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(time, style = MaterialTheme.typography.displayMedium.copy(textDirection = TextDirection.Ltr,
                fontFeatureSettings = "tnum"), fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun prayerName(prayer: Prayer) = stringResource(when (prayer) {
    Prayer.FAJR -> R.string.fajr; Prayer.DHUHR -> R.string.dhuhr; Prayer.ASR -> R.string.asr
    Prayer.MAGHRIB -> R.string.maghrib; Prayer.ISHA -> R.string.isha
})
@Composable private fun profileName(profile: Profile) = stringResource(when (profile) {
    Profile.MWL -> R.string.profile_mwl; Profile.ISNA -> R.string.profile_isna; Profile.EGYPTIAN -> R.string.profile_egyptian
    Profile.KARACHI -> R.string.profile_karachi; Profile.UMM_AL_QURA -> R.string.profile_umm_al_qura
    Profile.DUBAI -> R.string.profile_dubai; Profile.QATAR -> R.string.profile_qatar; Profile.KUWAIT -> R.string.profile_kuwait
    Profile.MOONSIGHTING -> R.string.profile_moonsighting; Profile.SINGAPORE -> R.string.profile_singapore
    Profile.TURKEY -> R.string.profile_turkey; Profile.FRANCE -> R.string.profile_france; Profile.RUSSIA -> R.string.profile_russia
    Profile.TEHRAN -> R.string.profile_tehran; Profile.JAFARI -> R.string.profile_jafari
})
@Composable private fun timeLabel(instant: Instant, zone: ZoneId): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val skeleton = if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm"
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return instant.atZone(zone).format(DateTimeFormatter.ofPattern(pattern, locale)
        .withDecimalStyle(java.time.format.DecimalStyle.of(locale)))
}
