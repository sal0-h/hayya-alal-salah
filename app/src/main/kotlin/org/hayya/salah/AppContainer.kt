// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import androidx.core.content.edit
import org.hayya.core.Prayer
import java.io.IOException
import org.hayya.salah.alerts.*
import org.hayya.salah.data.PrayerDatabase

class SalahApplication : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        if (AlarmCoordinator.unlocked(this)) container.startIntegrations()
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (AlarmCoordinator.unlocked(this)) container.refreshIntegrations()
    }
}
class AppContainer(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SetupStore(context)
    val database: PrayerDatabase by lazy { Room.databaseBuilder(context, PrayerDatabase::class.java, "prayer-history.db").build() }
    val repository: PrayerRepository by lazy { PrayerRepository(database) }
    private var integrationJob: Job? = null
    fun startIntegrations() {
        if (integrationJob?.isActive == true) return
        integrationJob = scope.launch {
            combine(settings.preferences, repository.changes) { _, _ -> Unit }.collect {
                try { AlarmCoordinator.reconcile(context); org.hayya.salah.widgets.WidgetUpdater.update(context) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep persisted events if private storage is unavailable. */ }
            }
        }
    }
    suspend fun removeImportedAudio(audio: org.hayya.salah.data.AudioEntity) {
        settings.update { prefs -> prefs.copy(alerts = prefs.alerts.mapValues { (_,alert) ->
            if (alert.sound == audio.id) alert.copy(sound = "builtin_adhan") else alert },
            fajr = if (prefs.fajr.sound == audio.id) prefs.fajr.copy(sound = "builtin_fajr") else prefs.fajr) }
        withContext(Dispatchers.IO) {
            val file = context.filesDir.resolve("audio").resolve(audio.fileName)
            if (file.exists() && !file.delete()) throw IOException("Private audio deletion incomplete")
        }
        repository.deleteAudio(audio.id)
    }
    /** Keep setup available for retry if deleting a private audio file fails. */
    suspend fun resetLocalData() {
        settings.update { it.copy(alerts = Prayer.entries.associateWith { PrayerAlert() },
            fajr = it.fajr.copy(enabled = false),focus = it.focus.copy(enabled = false,bypass = null)) }
        org.hayya.salah.focus.PrayerFocusService.releaseOverlay()
        AlarmCoordinator.cancelAll(context)
        repository.reset()
        withContext(Dispatchers.IO) {
            val audio = context.filesDir.resolve("audio")
            if (audio.exists() && !audio.deleteRecursively()) throw IOException("Private audio deletion incomplete")
        }
        settings.reset()
        DeliveryLocale.clear(context)
        context.getSharedPreferences("locale-bootstrap",0).edit { clear() }
        AlarmCoordinator.cancelAll(context)
        org.hayya.salah.widgets.WidgetUpdater.update(context)
    }
    fun refreshIntegrations() {
        startIntegrations()
        scope.launch { runCatching { AlarmCoordinator.reconcile(context); org.hayya.salah.widgets.WidgetUpdater.update(context) } }
    }
}
val Context.appContainer get() = (applicationContext as SalahApplication).container
