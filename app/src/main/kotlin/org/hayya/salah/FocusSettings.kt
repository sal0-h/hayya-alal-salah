// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Intent
import androidx.activity.compose.ReportDrawnWhen
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.core.Prayer
import org.hayya.salah.focus.EssentialApps

@Composable internal fun FocusScreen(config: AppPreferences, update: PreferencesUpdate) {
    val context = LocalContext.current
    var editingApps by remember { mutableStateOf(false) }
    var disclose by remember { mutableStateOf(false) }
    val apps by produceState<List<Pair<String, String>>?>(null) { value = withContext(Dispatchers.IO) { EssentialApps.selectable(context) } }
    SectionTitle(stringResource(R.string.prayer_focus))
    ReportDrawnWhen { apps != null }
    val connected by org.hayya.salah.focus.PrayerFocusService.connected.collectAsStateWithLifecycle()
    if (config.focus.enabled && !connected) Text(stringResource(R.string.focus_service_inactive), color = MaterialTheme.colorScheme.error)
    ToggleRow(stringResource(R.string.enable_focus), config.focus.enabled) { enabled ->
        if (enabled) disclose = true else update { it.copy(focus = it.focus.copy(enabled = false)) }
    }
    OutlinedButton(onClick = { disclose = true }, Modifier.heightIn(min = 48.dp).testTag("focus-permission")) { Text(stringResource(R.string.focus_permission)) }
    Text(stringResource(R.string.focus_select_prayers), style = MaterialTheme.typography.titleLarge)
    Prayer.entries.forEach { prayer -> ToggleRow(prayerName(prayer), prayer in config.focus.prayers) { selected ->
        update { it.copy(focus = it.focus.copy(prayers = if (selected) it.focus.prayers + prayer else it.focus.prayers - prayer)) }
    } }
    Text(stringResource(R.string.focus_select_apps), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.focus_app_count, numberLabel(config.focus.packages.size)))
    OutlinedButton(onClick = { editingApps = true }, enabled = apps != null,
        modifier = Modifier.heightIn(min = 48.dp).testTag("focus-choose-apps")) { Text(stringResource(R.string.choose_focus_apps)) }
    if (editingApps) FocusAppPicker(apps.orEmpty(), config.focus.packages, apply = { selected ->
        update { it.copy(focus = it.focus.copy(packages = selected)) }
        editingApps = false
    }, cancel = { editingApps = false })
    if (disclose) AlertDialog(onDismissRequest = { disclose = false }, title = { Text(stringResource(R.string.prayer_focus)) }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(stringResource(R.string.focus_disclosure)) } },
        confirmButton = { TextButton(onClick = {
            update { it.copy(focus = it.focus.copy(enabled = true)) }; disclose = false
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) { Text(stringResource(R.string.focus_permission)) } }, dismissButton = { TextButton(onClick = { disclose = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun FocusAppPicker(apps: List<Pair<String, String>>, initial: Set<String>,
    apply: (Set<String>) -> Unit, cancel: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    val matches = remember(query, apps) { apps.filter { (id, label) ->
        label.contains(query.trim(), ignoreCase = true) || id.contains(query.trim(), ignoreCase = true)
    } }
    val ids = remember(matches) { matches.mapTo(mutableSetOf()) { it.first } }
    val reviewOrder = remember(matches, initial) {
        if (query.isBlank()) matches.sortedByDescending { it.first in initial } else matches
    }
    AlertDialog(onDismissRequest = cancel,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text(stringResource(R.string.choose_focus_apps)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.focus_app_search)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("focus-app-search"))
                Text(stringResource(R.string.focus_app_count, numberLabel(selected.size)),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                TextButton(onClick = { selected = selected + ids }, enabled = ids.any { it !in selected },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("focus-select-all")) {
                    Text(stringResource(R.string.focus_select_results))
                }
                TextButton(onClick = { selected = selected - ids }, enabled = ids.any { it in selected },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.focus_clear_results)) }
                if (matches.isEmpty()) Text(stringResource(R.string.focus_no_apps))
                else LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 280.dp)) {
                    items(reviewOrder, key = { it.first }) { (id, label) ->
                        ToggleRow(label, id in selected) { checked -> selected = if (checked) selected + id else selected - id }
                    }
                }
            }
        }, confirmButton = {
            TextButton(onClick = { apply(selected) }, modifier = Modifier.testTag("focus-apply-apps")) {
                Text(stringResource(R.string.apply))
            }
        }, dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.cancel)) } })
}
