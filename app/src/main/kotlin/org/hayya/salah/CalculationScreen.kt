// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.hayya.core.*

@Composable internal fun CalculationScreen(place: ManualSetup, update: PreferencesUpdate) {
    var profile by rememberSaveable(place) { mutableStateOf(place.profile) }
    var asr by rememberSaveable(place) { mutableStateOf(place.calculation.asr) }
    var high by rememberSaveable(place) { mutableStateOf(place.calculation.highLatitude) }
    var polar by rememberSaveable(place) { mutableStateOf(place.calculation.polar) }
    var reference by rememberSaveable(place) { mutableStateOf(place.calculation.referenceLatitude?.toString().orEmpty()) }
    var fajr by rememberSaveable(place) { mutableStateOf(place.calculation.overrides.fajrAngle?.toString().orEmpty()) }
    var isha by rememberSaveable(place) { mutableStateOf(place.calculation.overrides.ishaAngle?.toString().orEmpty()) }
    var interval by rememberSaveable(place) { mutableStateOf(place.calculation.overrides.ishaInterval?.toString().orEmpty()) }
    var maghrib by rememberSaveable(place) { mutableStateOf(place.calculation.overrides.maghribAngle?.toString().orEmpty()) }
    val offsets = remember(place) { mutableStateMapOf<Prayer, String>().apply { Prayer.entries.forEach { put(it, (place.calculation.corrections[it] ?: 0).toString()) } } }
    var invalid by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.calculation))
    SelectionField(stringResource(R.string.calculation), profile, Profile.entries, { profileName(it) }) {
        profile = it; fajr = ""; isha = ""; interval = ""; maghrib = ""; polar = PolarMode.ASK; reference = ""
    }
    SelectionField(stringResource(R.string.asr_convention), asr, AsrConvention.entries,
        { stringResource(if (it == AsrConvention.STANDARD) R.string.asr_standard else R.string.asr_hanafi) }) { asr = it }
    SelectionField(stringResource(R.string.high_latitude), high, HighLatitude.entries, { stringResource(when (it) {
        HighLatitude.MIDDLE_OF_NIGHT -> R.string.high_middle; HighLatitude.SEVENTH_OF_NIGHT -> R.string.high_seventh; HighLatitude.TWILIGHT_ANGLE -> R.string.high_angle
    }) }) { high = it }
    SelectionField(stringResource(R.string.polar_policy), polar, PolarMode.entries.filter { it != PolarMode.NEAREST || (place.calculation.polar == PolarMode.NEAREST && profile == place.profile) }, { stringResource(when (it) {
        PolarMode.ASK -> R.string.polar_ask; PolarMode.DISABLED -> R.string.polar_disabled; PolarMode.NEAREST -> R.string.polar_nearest; PolarMode.REFERENCE -> R.string.polar_reference
    }) }) { polar = it }
    if (polar == PolarMode.REFERENCE) Text(stringResource(R.string.polar_consent, reference.ifBlank { "…" }))
    if (polar == PolarMode.REFERENCE) OutlinedTextField(reference, { reference = it }, label = { Text(stringResource(R.string.reference_latitude)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    SectionTitle(stringResource(R.string.minute_corrections))
    Prayer.entries.forEach { prayer -> OutlinedTextField(offsets.getValue(prayer), { offsets[prayer] = it }, label = { Text(prayerName(prayer)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
    SectionTitle(stringResource(R.string.advanced_parameters))
    Text(stringResource(R.string.advanced_explanation))
    listOf(Triple(R.string.fajr_angle, fajr, { s: String -> fajr = s }), Triple(R.string.isha_angle, isha, { s: String -> isha = s }),
        Triple(R.string.isha_interval, interval, { s: String -> interval = s })).forEach { (label, value, change) ->
        OutlinedTextField(value, change, label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    if (profile.engine == "praytimes") OutlinedTextField(maghrib, { maghrib = it }, label = { Text(stringResource(R.string.maghrib_angle)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    if (invalid) Text(stringResource(R.string.invalid_parameters), color = MaterialTheme.colorScheme.error)
    Button(onClick = {
        val settings = runCatching {
            fun number(s: String): Double? = if (s.isBlank()) null else normalizeNumber(s).toDouble()
            CalculationSettings(profile, asr, high, offsets.mapValues { normalizeNumber(it.value).toInt() },
                ParameterOverrides(number(fajr), number(isha), if (interval.isBlank()) null else normalizeNumber(interval).toInt(), if (profile.engine == "praytimes") number(maghrib) else null),
                polar, if (polar == PolarMode.REFERENCE) number(reference) else null)
        }.getOrNull()
        invalid = settings == null
        settings?.let { new -> update { it.copy(place = place.copy(calculation = new)) } }
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.save)) }
    TextButton(onClick = { update { it.copy(place = place.copy(calculation = CalculationSettings(profile))) } }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.reset_profile))
    }
}
