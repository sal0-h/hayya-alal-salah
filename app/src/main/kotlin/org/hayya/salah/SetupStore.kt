// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import org.hayya.core.GeoPoint
import org.hayya.core.Profile
import java.time.ZoneId

private val Context.setupDataStore by preferencesDataStore("setup")
data class ManualSetup(val name: String, val point: GeoPoint, val zone: ZoneId, val profile: Profile)
sealed interface SetupState {
    data object Loading : SetupState
    data class Ready(val setup: ManualSetup?) : SetupState
    data object ReadFailure : SetupState
}
class SetupStore(context: Context) {
    private val store = context.applicationContext.setupDataStore
    private val name = stringPreferencesKey("name")
    private val latitude = stringPreferencesKey("latitude")
    private val longitude = stringPreferencesKey("longitude")
    private val zone = stringPreferencesKey("zone")
    private val profile = stringPreferencesKey("profile")
    val setup = store.data.map { p ->
        // Corrupt or obsolete setup returns to explicit confirmation, never silently substitutes a city.
        runCatching { ManualSetup(p[name]!!, GeoPoint(p[latitude]!!.toDouble(), p[longitude]!!.toDouble()),
            ZoneId.of(p[zone]!!), Profile.valueOf(p[profile]!!)) }.getOrNull()
    }
    fun state(): Flow<SetupState> = setup.map<ManualSetup?, SetupState> { SetupState.Ready(it) }.catch {
        if (it is java.io.IOException) emit(SetupState.ReadFailure) else throw it
    }
    suspend fun save(value: ManualSetup) {
        store.edit { p ->
            p[name] = value.name
            p[latitude] = value.point.latitude.toString()
            p[longitude] = value.point.longitude.toString()
            p[zone] = value.zone.id
            p[profile] = value.profile.name
        }
    }
    suspend fun clear() { store.edit { it.clear() } }
}
