// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
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
    var disclose by remember { mutableStateOf(false) }
    val apps by produceState<List<Pair<String, String>>>(emptyList()) { value = withContext(Dispatchers.IO) { EssentialApps.selectable(context) } }
    SectionTitle(stringResource(R.string.prayer_focus))
    Text(stringResource(R.string.focus_block_explanation))
    Text(stringResource(R.string.focus_disclosure))
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
    apps.forEach { (name, label) -> ToggleRow(label, name in config.focus.packages) { selected ->
        update { it.copy(focus = it.focus.copy(packages = if (selected) it.focus.packages + name else it.focus.packages - name)) }
    } }
    if (disclose) AlertDialog(onDismissRequest = { disclose = false }, title = { Text(stringResource(R.string.prayer_focus)) }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(stringResource(R.string.focus_disclosure)) } },
        confirmButton = { TextButton(onClick = {
            update { it.copy(focus = it.focus.copy(enabled = true)) }; disclose = false
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) { Text(stringResource(R.string.focus_permission)) } }, dismissButton = { TextButton(onClick = { disclose = false }) { Text(stringResource(R.string.cancel)) } })
}
