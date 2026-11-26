// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
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
    fun refreshIntegrations() {
        startIntegrations()
        scope.launch { runCatching { AlarmCoordinator.reconcile(context); org.hayya.salah.widgets.WidgetUpdater.update(context) } }
    }
}
val Context.appContainer get() = (applicationContext as SalahApplication).container
