// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.hayya.core.*
import java.time.*
import java.time.format.*

internal typealias PreferencesUpdate = ((AppPreferences) -> AppPreferences) -> Unit

@Composable internal fun dateLabel(date: LocalDate, style: FormatStyle = FormatStyle.MEDIUM): String {
    val locale = LocalConfiguration.current.locales[0]
    return date.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
        .withDecimalStyle(decimalStyle(locale, LocalAppearance.current.numerals)))
}
@Composable internal fun statusName(status: PrayerStatus?) = stringResource(when (status) {
    PrayerStatus.ON_TIME -> R.string.status_on_time; PrayerStatus.MADE_UP -> R.string.status_made_up
    PrayerStatus.MISSED -> R.string.status_missed; PrayerStatus.EXCUSED -> R.string.status_excused
    null -> R.string.status_unrecorded
})
internal fun statusSymbol(status: PrayerStatus?) = when (status) {
    PrayerStatus.ON_TIME -> "✓"; PrayerStatus.MADE_UP -> "↺"; PrayerStatus.MISSED -> "×"; PrayerStatus.EXCUSED -> "○"; null -> "—"
}
@Composable internal fun <T> SelectionField(label: String, value: T, options: List<T>, text: @Composable (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text(value)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { item -> DropdownMenuItem(text = { Text(text(item)) }, onClick = { expanded = false; onSelect(item) }) }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun DateButton(label: String, date: LocalDate, max: LocalDate? = null, onDate: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("$label: ${dateLabel(date)}") }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L,
            yearRange = 1900..2100, selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = max == null || LocalDate.ofEpochDay(Math.floorDiv(utcTimeMillis, 86_400_000L)) <= max
            })
        DatePickerDialog(onDismissRequest = { show = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }; show = false },
                enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.apply)) }
        }, dismissButton = { TextButton(onClick = { show = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state) }
    }
}
@Composable internal fun RecordDialog(key: PrayerKey, busy: Boolean, onSave: (PrayerStatus?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Column { Text(stringResource(R.string.record_prayer)); Text("${prayerName(key.prayer)} · ${dateLabel(key.date)}", style = MaterialTheme.typography.titleMedium) } },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.record_explanation))
            PrayerStatus.entries.forEach { status -> TextButton(enabled = !busy, onClick = { onSave(status) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("${statusSymbol(status)}  ${statusName(status)}")
            } }
            TextButton(enabled = !busy, onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.clear_record)) }
        } }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
@Composable internal fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onCheckedChange = onChecked, enabled = enabled)
    }
}
@Composable internal fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge) }
