// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hayya.core.*
import java.time.*
import java.util.Locale

/** Full shared shell with synthetic public fixtures. Absent from QA/release APKs. */
class ScreenshotActivity : ComponentActivity() {
    companion object {
        var language = "en"
        var dark = false
        var route = "today"
        var recordDialog = false
    }
    private var scrolling: ScrollState? = null
    override fun attachBaseContext(base: Context) {
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(language)))
            setLayoutDirection(Locale.forLanguageTag(language))
            // Font scale comes from the actual emulator system setting.
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }
    fun bottom() { lifecycleScope.launch { scrolling?.let { it.scrollTo(it.maxValue) } } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        if (route == "focus-overlay") {
            setContentView(org.hayya.salah.focus.focusOverlayContent(this,language,
                AppearancePreferences(theme = if (dark) ThemePreference.DARK else ThemePreference.LIGHT),Prayer.FAJR,{},{}))
            androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
            window.decorView.post { reportFullyDrawn() }; return
        }
        setContent {
            ReportDrawnWhen { true }
            if (route in listOf("ringing","ringing-keyboard")) {
                HayyaTheme(AppearancePreferences(theme = if (dark) ThemePreference.DARK else ThemePreference.LIGHT)) {
                    org.hayya.salah.alerts.RingingContent(org.hayya.salah.alerts.AlarmEntry("visual",
                        PrayerKey(LocalDate.of(2026,11,13),Prayer.FAJR),EventKind.FAJR_WAKE_UP,Instant.EPOCH,"UTC",null,ChallengeDifficulty.HARD,"visual-fixture"),{},{})
                }
                return@setContent
            }
            val fixed = remember { Clock.fixed(Instant.parse("2026-11-13T08:00:00Z"), ZoneOffset.UTC) }
            val today = LocalDate.of(2026,11,13)
            val place = remember { ManualSetup("Baku · Bakı", GeoPoint(40.4093,49.8671), ZoneId.of("Asia/Baku"), Profile.MWL) }
            val polarPlace = remember { ManualSetup("Longyearbyen", GeoPoint(78.22,15.65), ZoneId.of("Europe/Oslo"), Profile.MWL) }
            var config by remember { mutableStateOf(AppPreferences(if (route == "onboarding") null else if (route == "polar") polarPlace else place,
                appearance = AppearancePreferences(theme = if (dark) ThemePreference.DARK else ThemePreference.LIGHT), trackingSince = today.minusDays(30))) }
            var history by remember { mutableStateOf(HistorySnapshot(PrayerStatus.entries.mapIndexed { index, status ->
                val key = PrayerKey(today.minusDays(1), Prayer.entries[index]); key to PrayerRecord(key, status, fixed.instant(), fixed.instant(), place.zone)
            }.toMap(), listOf(PauseInterval(today.minusDays(3),today.minusDays(2))), emptyList())) }
            var target by remember { mutableStateOf(if (recordDialog) PrayerKey(today,Prayer.FAJR) else null) }
            val scroll = rememberScrollState(); scrolling = scroll
            HayyaTheme(config.appearance) {
                AppShell(config, history, onRecord = { target = it }, update = { transform -> config = transform(config) },
                    savePlace = { config = config.copy(place = it) }, addPause = { history = history.copy(pauses = history.pauses + it) },
                    removePause = { history = history.copy(pauses = history.pauses - it) }, deleteRecords = { history = history.copy(records = emptyMap()) },
                    reset = { config = AppPreferences() }, onLanguage = {}, initialRoute = route, clock = fixed, scrollState = scroll)
                target?.let { key -> RecordDialog(key, false, onSave = { status ->
                    history = history.copy(records = if (status == null) history.records - key else history.records +
                        (key to PrayerRecord(key,status,fixed.instant(),fixed.instant(),place.zone))); target = null
                }, onDismiss = { target = null }) }
            }
        }
    }
}

/** Stable native underlay while capture activities/configurations change; never a launcher. */
class CaptureHostActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) { super.attachBaseContext(DeliveryLocale.context(base)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
    }
}
