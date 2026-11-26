// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.hayya.core.*
import java.time.LocalDate
import java.time.ZoneId

/** One bounded, in-memory calculation cache shared by the visible app's destinations. */
internal class TimetableCache(
    private val point: GeoPoint,
    private val zone: ZoneId,
    private val settings: CalculationSettings,
    private val calculator: PrayerCalculator = LocalPrayerCalculator(),
    capacity: Int = 16,
) {
    private val results = LruCache<LocalDate, CalculationResult>(capacity)
    private val calculation = Mutex()
    fun peek(date: LocalDate): CalculationResult? = results.get(date)
    suspend fun load(date: LocalDate): CalculationResult = peek(date) ?: withContext(Dispatchers.Default) {
        calculation.withLock {
            peek(date) ?: calculator.calculate(date, point, zone, settings).also { results.put(date, it) }
        }
    }
}

@Composable internal fun rememberTimetableCache(place: ManualSetup): TimetableCache =
    remember(place.point, place.zone, place.calculation) {
        TimetableCache(place.point, place.zone, place.calculation)
    }
