// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.ZoneId

@Composable internal fun ManualLocationScreen(initial: ManualSetup?, save: (ManualSetup) -> Unit) {
    var name by rememberSaveable(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var latitude by rememberSaveable(initial) { mutableStateOf(initial?.point?.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable(initial) { mutableStateOf(initial?.point?.longitude?.toString().orEmpty()) }
    var zone by rememberSaveable(initial) { mutableStateOf(initial?.zone?.id ?: ZoneId.systemDefault().id) }
    var invalid by remember { mutableStateOf(false) }
    var showCoordinates by rememberSaveable { mutableStateOf(false) }
    var automatic by rememberSaveable(initial) { mutableStateOf(initial?.automatic ?: false) }
    var refreshedAt by remember(initial) { mutableStateOf(initial?.refreshedAt) }
    LocationControls { selectedName, point, selectedZone, refreshed ->
        name = selectedName
        latitude = point.latitude.toString()
        longitude = point.longitude.toString()
        zone = selectedZone
        refreshedAt = refreshed
        automatic = refreshed != null
        showCoordinates = refreshed != null || selectedZone !in ZoneId.getAvailableZoneIds()
        invalid = false
    }
    if (name.isNotBlank()) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(zone, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick = { showCoordinates = !showCoordinates }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.coordinates_and_zone))
    }
    if (showCoordinates) {
        if (refreshedAt != null) Text(stringResource(R.string.zone_suggestion_note))
        if (zone.trim() !in ZoneId.getAvailableZoneIds())
            Text(stringResource(R.string.zone_not_supported), color = MaterialTheme.colorScheme.error)
        listOf(Triple(R.string.place_name, name, { value: String -> name = value }),
            Triple(R.string.latitude, latitude, { value: String -> latitude = value; refreshedAt = null }),
            Triple(R.string.longitude, longitude, { value: String -> longitude = value; refreshedAt = null }),
            Triple(R.string.timezone, zone, { value: String -> zone = value })).forEach { (label, value, change) ->
            OutlinedTextField(value, change, label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
    ToggleRow(stringResource(R.string.automatic_location), automatic) { automatic = it }
    if (automatic) {
        Text(stringResource(R.string.foreground_location_note))
        if (!org.hayya.salah.location.ForegroundLocation.allowed(LocalContext.current))
            Text(stringResource(R.string.location_permission_needed), color = MaterialTheme.colorScheme.error)
    }
    if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
    Button(onClick = {
        val place = runCatching { ManualSetup(name.trim(), GeoPoint(normalizeNumber(latitude).toDouble(), normalizeNumber(longitude).toDouble()),
            ZoneId.of(zone.trim()), initial?.calculation ?: CalculationSettings(), automatic, refreshedAt) }.getOrNull()
        invalid = place == null
        if (place == null) showCoordinates = true else save(place)
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save-location")) {
        Text(stringResource(if (initial == null) R.string.prayer_time_method else R.string.save_location))
    }
}

@Composable internal fun languageName(language: String): String = when (language) {
    "en" -> "English"; "ru" -> "Русский"; "az" -> "Azərbaycanca"; "ar" -> "العربية"; else -> stringResource(R.string.follow_system)
}
@Composable internal fun AppearanceScreen(config: AppPreferences, update: PreferencesUpdate, onLanguage: (String) -> Unit) {
    val appearance = config.appearance
    SectionTitle(stringResource(R.string.appearance))
    SelectionField(stringResource(R.string.language), config.language, listOf("system", "en", "ru", "az", "ar"), { languageName(it) }, onLanguage)
    PalettePicker(appearance) { selected -> update { it.copy(appearance = it.appearance.copy(palette = selected)) } }
    SelectionField(stringResource(R.string.appearance), appearance.theme, ThemePreference.entries,
        { stringResource(when (it) { ThemePreference.SYSTEM -> R.string.follow_system; ThemePreference.LIGHT -> R.string.light; ThemePreference.DARK -> R.string.dark }) }) { selected -> update { it.copy(appearance = it.appearance.copy(theme = selected)) } }
    SelectionField(stringResource(R.string.time_format), appearance.time, TimePreference.entries,
        { stringResource(when (it) { TimePreference.SYSTEM -> R.string.follow_system; TimePreference.TWELVE -> R.string.hour_12; TimePreference.TWENTY_FOUR -> R.string.hour_24 }) }) { selected -> update { it.copy(appearance = it.appearance.copy(time = selected)) } }
    SelectionField(stringResource(R.string.numerals), appearance.numerals, NumeralPreference.entries,
        { stringResource(when (it) { NumeralPreference.SYSTEM -> R.string.follow_system; NumeralPreference.LATIN -> R.string.latin_digits; NumeralPreference.ARABIC_INDIC -> R.string.arabic_digits }) }) { selected -> update { it.copy(appearance = it.appearance.copy(numerals = selected)) } }
    ToggleRow(stringResource(R.string.reduced_motion), appearance.reducedMotion) { selected -> update { it.copy(appearance = it.appearance.copy(reducedMotion = selected)) } }
    Text(stringResource(R.string.hijri_calendar), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.hijri_umm_al_qura), color = MaterialTheme.colorScheme.onSurfaceVariant)
    SelectionField(stringResource(R.string.hijri_adjustment), config.hijriAdjustment, (-2..2).toList(), { numberLabel(it) }) { selected -> update { it.copy(hijriAdjustment = selected) } }
}
@Composable private fun PalettePicker(appearance: AppearancePreferences, select: (Palette) -> Unit) {
    val dark = appearance.theme == ThemePreference.DARK ||
        appearance.theme == ThemePreference.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.palette), style = MaterialTheme.typography.labelLarge)
        Palette.entries.forEach { palette ->
            val colors = paletteColors(palette, dark)
            val selected = appearance.palette == palette
            Surface(shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("palette-${palette.name.lowercase()}")
                    .selectable(selected, role = Role.RadioButton) { select(palette) }.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(androidx.compose.ui.graphics.Color(colors.accent), MaterialTheme.shapes.extraSmall))
                    Text(stringResource(when (palette) { Palette.TEAL -> R.string.teal; Palette.INK -> R.string.ink; Palette.PLUM -> R.string.plum }),
                        Modifier.weight(1f))
                    if (selected) Text("✓", Modifier.clearAndSetSemantics { }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable internal fun PrivacyScreen(deleteRecords: () -> Unit, reset: () -> Unit) {
    var confirmation by remember { mutableStateOf<Int?>(null) }
    SectionTitle(stringResource(R.string.privacy_data))
    Text(stringResource(R.string.privacy))
    OutlinedButton(onClick = { confirmation = R.string.delete_records }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("privacy-delete")) { Text(stringResource(R.string.delete_records)) }
    OutlinedButton(onClick = { confirmation = R.string.full_reset }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.full_reset)) }
    confirmation?.let { label -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(stringResource(label)) }, text = { Text(stringResource(R.string.delete_confirm)) },
        confirmButton = { TextButton(onClick = { if (label == R.string.delete_records) deleteRecords() else reset(); confirmation = null }) { Text(stringResource(R.string.remove)) } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text(stringResource(R.string.cancel)) } }) }
}
@Composable internal fun CreditsScreen() {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf("NOTICE.txt") }
    val names by produceState<List<String>>(emptyList()) { value = withContext(Dispatchers.IO) { context.assets.list("")?.filter { it.endsWith(".txt") }.orEmpty() } }
    SectionTitle(stringResource(R.string.credits))
    if (names.isNotEmpty()) SelectionField(stringResource(R.string.credits), selected, names, { it }) { selected = it }
    val text by produceState("", selected) { value = withContext(Dispatchers.IO) { context.assets.open(selected).bufferedReader().use { it.readText() } } }
    ReportDrawnWhen { text.isNotEmpty() }
    Text(text, style = MaterialTheme.typography.bodySmall)
}
