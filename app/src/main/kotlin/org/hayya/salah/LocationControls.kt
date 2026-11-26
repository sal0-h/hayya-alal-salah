// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import org.hayya.core.*
import org.hayya.salah.location.*
import java.time.Instant
import java.time.ZoneId

@Composable internal fun LocationControls(select: (String, GeoPoint, String, Instant?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var query by rememberSaveable { mutableStateOf("") }
    val cities by produceState<List<OfflineCity>>(emptyList(), query) {
        delay(250); value = OfflineCities(context).search(query)
    }
    var busy by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    var request by remember { mutableIntStateOf(0) }
    val coordinateLabel = stringResource(R.string.current_coordinates)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { request++ }
    LaunchedEffect(request) {
        var consumed = false
        if (request > 0) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (consumed) awaitCancellation()
            consumed = true
            busy = true; unavailable = false
            try {
                val location = ForegroundLocation.get(context)
                if (location == null) unavailable = true
                else {
                    val point = GeoPoint(location.latitude, location.longitude)
                    select(coordinateLabel, point, ZoneSuggestion.at(point), Instant.ofEpochMilli(location.time))
                }
            } finally { busy = false }
            // Each explicit click gets a single bounded request; resume does not repeat it.
            awaitCancellation()
        }
    }
    OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.search_city)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    cities.forEach { city -> TextButton(onClick = { select(city.name, city.point, city.zone, null); query = "" }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("${city.name} · ${city.country} · ${city.zone}") } }
    OutlinedButton(onClick = {
        if (ForegroundLocation.allowed(context)) request++
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }, enabled = !busy) { Text(stringResource(R.string.use_device_location)) }
    if (busy) CircularProgressIndicator()
    if (unavailable) Text(stringResource(R.string.location_unavailable), color = MaterialTheme.colorScheme.error)
    Text(stringResource(R.string.zone_suggestion_note))
}

/** Foreground-only automatic refresh; coordinate and confirmed zone change atomically. */
@Composable internal fun AutomaticLocationRefresh(config: AppPreferences, save: (ManualSetup) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val place = config.place
    var pending by remember { mutableStateOf<ManualSetup?>(null) }
    var zone by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val name = stringResource(R.string.current_coordinates)
    LaunchedEffect(place, owner) {
        if (place?.automatic == true) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (ForegroundLocation.allowed(context) && (place.refreshedAt == null || place.refreshedAt < Instant.now().minusSeconds(900))) {
                val location = ForegroundLocation.get(context)
                if (location != null && Instant.ofEpochMilli(location.time) > (place.refreshedAt ?: Instant.EPOCH)) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    val suggested = ZoneSuggestion.at(point)
                    val candidate = place.copy(name = name, point = point, refreshedAt = Instant.ofEpochMilli(location.time))
                    if (suggested == place.zone.id) save(candidate)
                    else { pending = candidate; zone = suggested }
                }
            }
            awaitCancellation()
        }
    }
    pending?.let { candidate -> AlertDialog(onDismissRequest = { pending = null },
        title = { Text(stringResource(R.string.confirm_location_zone)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${candidate.point.latitude}, ${candidate.point.longitude}")
            Text(stringResource(R.string.zone_suggestion_note))
            OutlinedTextField(zone, { zone = it }, label = { Text(stringResource(R.string.timezone)) }, singleLine = true)
            if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
        } }, confirmButton = { TextButton(onClick = {
            val selected = runCatching { candidate.copy(zone = ZoneId.of(zone.trim())) }.getOrNull()
            invalid = selected == null
            if (selected != null) { save(selected); pending = null }
        }) { Text(stringResource(R.string.confirm_setup)) } }, dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } }) }
}
