// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import org.hayya.salah.data.PrayerDatabase

class SalahApplication : Application() { val container by lazy { AppContainer(this) } }
class AppContainer(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SetupStore(context)
    val database: PrayerDatabase by lazy { Room.databaseBuilder(context, PrayerDatabase::class.java, "prayer-history.db").build() }
    val repository: PrayerRepository by lazy { PrayerRepository(database) }
}
val Context.appContainer get() = (applicationContext as SalahApplication).container
