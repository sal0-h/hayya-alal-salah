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


internal val LocalAppearance = staticCompositionLocalOf { AppearancePreferences() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HayyaTheme(appearance: AppearancePreferences = AppearancePreferences(), content: @Composable () -> Unit) {
    val scheme = if (appearance.theme == ThemePreference.DARK || (appearance.theme == ThemePreference.SYSTEM && isSystemInDarkTheme())) darkColorScheme(
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
    val dark = appearance.theme == ThemePreference.DARK || (appearance.theme == ThemePreference.SYSTEM && isSystemInDarkTheme())
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val activity = view.context as? android.app.Activity
        activity?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val accent = when (appearance.palette) {
        Palette.TEAL -> scheme.primary
        Palette.INK -> if (dark) Color(0xFFA6C8FF) else Color(0xFF204D86)
        Palette.PLUM -> if (dark) Color(0xFFE5AADC) else Color(0xFF743768)
    }
    val colors = scheme.copy(primary = accent, secondary = accent,
        secondaryContainer = androidx.compose.ui.graphics.lerp(scheme.background, accent, .14f),
        onSecondaryContainer = scheme.onSurface)
    CompositionLocalProvider(LocalAppearance provides appearance,
        LocalRippleConfiguration provides if (appearance.reducedMotion) null else LocalRippleConfiguration.current) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}

@Composable internal fun NextPrayerMarker(name: String, time: String) {
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

@Composable internal fun prayerName(prayer: Prayer) = stringResource(when (prayer) {
    Prayer.FAJR -> R.string.fajr; Prayer.DHUHR -> R.string.dhuhr; Prayer.ASR -> R.string.asr
    Prayer.MAGHRIB -> R.string.maghrib; Prayer.ISHA -> R.string.isha
})
@Composable internal fun profileName(profile: Profile) = stringResource(when (profile) {
    Profile.MWL -> R.string.profile_mwl; Profile.ISNA -> R.string.profile_isna; Profile.EGYPTIAN -> R.string.profile_egyptian
    Profile.KARACHI -> R.string.profile_karachi; Profile.UMM_AL_QURA -> R.string.profile_umm_al_qura
    Profile.DUBAI -> R.string.profile_dubai; Profile.QATAR -> R.string.profile_qatar; Profile.KUWAIT -> R.string.profile_kuwait
    Profile.MOONSIGHTING -> R.string.profile_moonsighting; Profile.SINGAPORE -> R.string.profile_singapore
    Profile.TURKEY -> R.string.profile_turkey; Profile.FRANCE -> R.string.profile_france; Profile.RUSSIA -> R.string.profile_russia
    Profile.TEHRAN -> R.string.profile_tehran; Profile.JAFARI -> R.string.profile_jafari
})
@Composable internal fun timeLabel(instant: Instant, zone: ZoneId): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val preference = LocalAppearance.current
    val use24 = when (preference.time) { TimePreference.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
        TimePreference.TWELVE -> false; TimePreference.TWENTY_FOUR -> true }
    val skeleton = if (use24) "Hm" else "hm"
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return instant.atZone(zone).format(DateTimeFormatter.ofPattern(pattern, locale)
        .withDecimalStyle(decimalStyle(locale, preference.numerals)))
}

internal fun decimalStyle(locale: java.util.Locale, numerals: NumeralPreference) = when (numerals) {
    NumeralPreference.SYSTEM -> java.time.format.DecimalStyle.of(locale)
    NumeralPreference.LATIN -> java.time.format.DecimalStyle.STANDARD
    NumeralPreference.ARABIC_INDIC -> java.time.format.DecimalStyle.STANDARD.withZeroDigit('٠')
}
internal fun normalizeNumber(text: String): String = buildString {
    text.trim().forEach { c -> append(when { Character.digit(c, 10) >= 0 -> '0' + Character.digit(c, 10)
        c == '−' -> '-'; c == '٫' || c == ',' -> '.'; else -> c }) }
}

@Composable internal fun foregroundInstant(clock: java.time.Clock): java.time.Instant {
    var now by remember { mutableStateOf(clock.instant()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, clock) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) { now = clock.instant(); delay(30_000) }
    } }
    return now
}
