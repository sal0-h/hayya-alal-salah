// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
    var profile by rememberSaveable(initial) { mutableStateOf(initial?.profile ?: Profile.MWL) }
    var invalid by remember { mutableStateOf(false) }
    var automatic by rememberSaveable(initial) { mutableStateOf(initial?.automatic ?: false) }
    var refreshedAt by remember(initial) { mutableStateOf(initial?.refreshedAt) }
    Text(stringResource(R.string.setup_intro))
    LocationControls { selectedName, point, selectedZone, refreshed ->
        name = selectedName; latitude = point.latitude.toString(); longitude = point.longitude.toString(); zone = selectedZone
        refreshedAt = refreshed; automatic = refreshed != null
    }
    ToggleRow(stringResource(R.string.automatic_location), automatic) { automatic = it }
    Text(stringResource(R.string.foreground_location_note))
    if (automatic && !org.hayya.salah.location.ForegroundLocation.allowed(LocalContext.current)) Text(stringResource(R.string.location_permission_needed), color = MaterialTheme.colorScheme.error)
    if (zone !in ZoneId.getAvailableZoneIds()) Text(stringResource(R.string.zone_not_supported), color = MaterialTheme.colorScheme.error)
    refreshedAt?.let { Text(stringResource(R.string.location_observed_at, it.toString())) }
    listOf(Triple(R.string.place_name, name, { value: String -> name = value }),
        Triple(R.string.latitude, latitude, { value: String -> latitude = value }),
        Triple(R.string.longitude, longitude, { value: String -> longitude = value }),
        Triple(R.string.timezone, zone, { value: String -> zone = value })).forEach { (label, value, change) ->
        OutlinedTextField(value, change, label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    Text(stringResource(R.string.profile_explanation))
    SelectionField(stringResource(R.string.calculation), profile, Profile.entries, { profileName(it) }) { profile = it }
    if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
    Button(onClick = {
        val place = runCatching { ManualSetup(name.trim(), GeoPoint(normalizeNumber(latitude).toDouble(), normalizeNumber(longitude).toDouble()),
            ZoneId.of(zone.trim()), if (profile == initial?.profile) initial.calculation else CalculationSettings(profile), automatic, refreshedAt) }.getOrNull()
        invalid = place == null
        place?.let(save)
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.confirm_setup)) }
}

@Composable internal fun languageName(language: String): String = when (language) {
    "en" -> "English"; "ru" -> "Русский"; "az" -> "Azərbaycanca"; "ar" -> "العربية"; else -> stringResource(R.string.follow_system)
}
@Composable internal fun AppearanceScreen(config: AppPreferences, update: PreferencesUpdate, onLanguage: (String) -> Unit) {
    val appearance = config.appearance
    SectionTitle(stringResource(R.string.appearance))
    SelectionField(stringResource(R.string.language), config.language, listOf("system", "en", "ru", "az", "ar"), { languageName(it) }, onLanguage)
    SelectionField(stringResource(R.string.appearance), appearance.theme, ThemePreference.entries,
        { stringResource(when (it) { ThemePreference.SYSTEM -> R.string.follow_system; ThemePreference.LIGHT -> R.string.light; ThemePreference.DARK -> R.string.dark }) }) { selected -> update { it.copy(appearance = it.appearance.copy(theme = selected)) } }
    SelectionField(stringResource(R.string.palette), appearance.palette, Palette.entries,
        { stringResource(when (it) { Palette.TEAL -> R.string.teal; Palette.INK -> R.string.ink; Palette.PLUM -> R.string.plum }) }) { selected -> update { it.copy(appearance = it.appearance.copy(palette = selected)) } }
    SelectionField(stringResource(R.string.time_format), appearance.time, TimePreference.entries,
        { stringResource(when (it) { TimePreference.SYSTEM -> R.string.follow_system; TimePreference.TWELVE -> R.string.hour_12; TimePreference.TWENTY_FOUR -> R.string.hour_24 }) }) { selected -> update { it.copy(appearance = it.appearance.copy(time = selected)) } }
    SelectionField(stringResource(R.string.numerals), appearance.numerals, NumeralPreference.entries,
        { stringResource(when (it) { NumeralPreference.SYSTEM -> R.string.follow_system; NumeralPreference.LATIN -> R.string.latin_digits; NumeralPreference.ARABIC_INDIC -> R.string.arabic_digits }) }) { selected -> update { it.copy(appearance = it.appearance.copy(numerals = selected)) } }
    ToggleRow(stringResource(R.string.reduced_motion), appearance.reducedMotion) { selected -> update { it.copy(appearance = it.appearance.copy(reducedMotion = selected)) } }
    SelectionField(stringResource(R.string.hijri_adjustment), config.hijriAdjustment, (-2..2).toList(), { it.toString() }) { selected -> update { it.copy(hijriAdjustment = selected) } }
}
@Composable internal fun PrivacyScreen(deleteRecords: () -> Unit, reset: () -> Unit) {
    var confirmation by remember { mutableStateOf<Int?>(null) }
    SectionTitle(stringResource(R.string.privacy_data))
    Text(stringResource(R.string.privacy))
    OutlinedButton(onClick = { confirmation = R.string.delete_records }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.delete_records)) }
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
    Text(text, style = MaterialTheme.typography.bodySmall)
}
