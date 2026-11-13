// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import org.hayya.core.*
import java.time.LocalDate
import java.time.ZoneId

private val Context.setupDataStore by preferencesDataStore("setup")
sealed interface SetupState {
    data object Loading : SetupState
    data class Ready(val setup: ManualSetup?) : SetupState
    data object ReadFailure : SetupState
}
class SetupStore(private val context: Context) {
    private val store = context.applicationContext.setupDataStore
    private val configKey = stringPreferencesKey("app_config")
    private fun decode(p: Preferences): AppPreferences {
        val json = p[configKey]
        if (json != null) return try { PreferencesCodec.decode(json) } catch (e: Exception) {
            throw java.io.IOException("Saved settings could not be read", e)
        }
        // Preserve the installed foundation preview's explicit manual setup.
        val legacy = runCatching {
            fun get(key: String) = p[stringPreferencesKey(key)]!!
            ManualSetup(get("name"), GeoPoint(get("latitude").toDouble(), get("longitude").toDouble()), ZoneId.of(get("zone")), Profile.valueOf(get("profile")))
        }.getOrNull()
        return AppPreferences(place = legacy, trackingSince = legacy?.let { LocalDate.now(it.zone) })
    }
    val preferences: Flow<AppPreferences> = store.data.map(::decode)
    val setup: Flow<ManualSetup?> = preferences.map { it.place }
    fun state(): Flow<SetupState> = setup.map<ManualSetup?, SetupState> { SetupState.Ready(it) }.catch {
        if (it is java.io.IOException) emit(SetupState.ReadFailure) else throw it
    }
    suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        store.edit { p ->
            val next = transform(decode(p))
            val encoded = PreferencesCodec.encode(next)
            p.clear(); p[configKey] = encoded
        }
    }
    suspend fun save(value: ManualSetup) = update { previous ->
        val old = previous.place
        val changed = old != null && (old.profile != value.profile || old.zone != value.zone || old.point != value.point)
        val place = if (changed) value.copy(calculation = value.calculation.copy(polar = PolarMode.ASK, referenceLatitude = null)) else value
        previous.copy(place = place, trackingSince = previous.trackingSince ?: LocalDate.now(value.zone))
    }
    suspend fun clear() = update { it.copy(place = null) }
    suspend fun reset() { store.edit { it.clear() } }
}
