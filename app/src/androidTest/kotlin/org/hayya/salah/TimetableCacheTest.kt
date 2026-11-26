// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class TimetableCacheTest {
    private val day = LocalDate.of(2026, 11, 13)
    private val point = GeoPoint(40.4, 49.9)
    private val zone = ZoneId.of("Asia/Baku")
    private val unavailable = CalculationResult.Unavailable(CalculationResult.Reason.NO_VALID_SOLAR_EVENTS)

    @Test fun returningToADateAndConcurrentRequestsReuseItsResult() = runBlocking {
        val calls = AtomicInteger()
        val cache = TimetableCache(point, zone, CalculationSettings(), PrayerCalculator { _, _, _, _ ->
            calls.incrementAndGet()
            unavailable
        })
        assertNull(cache.peek(day))
        val results = List(8) { async { cache.load(day) } }.awaitAll()
        assertTrue(results.all { it == unavailable })
        assertEquals(unavailable, cache.peek(day))
        assertEquals(unavailable, cache.load(day))
        assertEquals(1, calls.get())
    }

    @Test fun cacheIsBoundedAndRefreshesEvictedDates() = runBlocking {
        val calls = AtomicInteger()
        val cache = TimetableCache(point, zone, CalculationSettings(), PrayerCalculator { _, _, _, _ ->
            calls.incrementAndGet(); unavailable
        }, capacity = 2)
        cache.load(day); cache.load(day.plusDays(1)); cache.load(day.plusDays(2))
        assertNull(cache.peek(day))
        assertNotNull(cache.peek(day.plusDays(1)))
        cache.load(day)
        assertEquals(4, calls.get())
    }

    @Test fun independentCalculationConfigurationsCannotReturnTheOtherResult() = runBlocking {
        val inputs = mutableListOf<Triple<GeoPoint, ZoneId, CalculationSettings>>()
        val calculator = PrayerCalculator { _, location, timezone, settings ->
            inputs += Triple(location, timezone, settings)
            CalculationResult.NeedsPolarConfirmation(location.latitude)
        }
        val first = TimetableCache(point, zone, CalculationSettings(), calculator)
        val otherPoint = GeoPoint(-33.8, 151.2)
        val otherZone = ZoneId.of("Australia/Sydney")
        val otherSettings = CalculationSettings(profile = Profile.JAFARI, asr = AsrConvention.HANAFI)
        val second = TimetableCache(otherPoint, otherZone, otherSettings, calculator)
        assertEquals(CalculationResult.NeedsPolarConfirmation(point.latitude), first.load(day))
        assertEquals(CalculationResult.NeedsPolarConfirmation(otherPoint.latitude), second.load(day))
        assertEquals(2, inputs.size)
        assertEquals(Triple(otherPoint, otherZone, otherSettings), inputs.last())
    }
}
