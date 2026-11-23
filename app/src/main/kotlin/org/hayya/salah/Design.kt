// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.hayya.core.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


internal val LocalAppearance = staticCompositionLocalOf { AppearancePreferences() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HayyaTheme(appearance: AppearancePreferences = AppearancePreferences(), content: @Composable () -> Unit) {
    val dark = appearance.theme == ThemePreference.DARK || (appearance.theme == ThemePreference.SYSTEM && isSystemInDarkTheme())
    val palette = paletteColors(appearance.palette, dark)
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
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
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val activity = view.context.findActivity()
        activity?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val accent = Color(palette.accent)
    val surface = Color(palette.surface)
    val ink = Color(palette.ink)
    val secondary = Color(palette.secondary)
    val wash = androidx.compose.ui.graphics.lerp(surface, accent, if (dark) .18f else .10f)
    val onAccent = if (dark) Color(0xFF112019) else Color.White
    val colors = scheme.copy(background = surface, surface = surface, onBackground = ink, onSurface = ink,
        onSurfaceVariant = secondary, outline = secondary, outlineVariant = Color(palette.line),
        primary = accent, onPrimary = onAccent, secondary = accent, onSecondary = onAccent,
        primaryContainer = wash, onPrimaryContainer = ink, secondaryContainer = wash, onSecondaryContainer = ink,
        surfaceVariant = wash, surfaceContainerLow = wash)
    CompositionLocalProvider(LocalAppearance provides appearance,
        LocalRippleConfiguration provides if (appearance.reducedMotion) null else LocalRippleConfiguration.current) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}

@Composable internal fun NextPrayerMarker(name: String, time: String, remaining: String) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().then(Modifier.testTag("prayer-data-ready")).heightIn(min = 122.dp), contentAlignment = Alignment.Center) {
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
        Column(Modifier.padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(stringResource(R.string.next_prayer), style = MaterialTheme.typography.labelSmall, color = accent, textAlign = TextAlign.Center)
            Text(name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(time, style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr,
                fontFeatureSettings = "tnum"), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(remaining, style = MaterialTheme.typography.bodySmall, color = accent, textAlign = TextAlign.Center)
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

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
