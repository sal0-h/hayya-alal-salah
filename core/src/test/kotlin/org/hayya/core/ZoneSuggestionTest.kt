// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core
import org.junit.Assert.*
import org.junit.Test
class ZoneSuggestionTest {
    @Test fun knownPublicCitiesAndAllWorldEdgesProduceIdentifiers() {
        listOf(Triple(40.4093,49.8671,"Asia/Baku"), Triple(55.7558,37.6173,"Europe/Moscow"),
            Triple(21.4225,39.8262,"Asia/Riyadh"),Triple(40.7128,-74.0060,"America/New_York"),
            Triple(-33.8688,151.2093,"Australia/Sydney"),Triple(27.7172,85.3240,"Asia/Kathmandu"))
            .forEach { (lat, lon, expected) -> assertEquals(expected, ZoneSuggestion.at(GeoPoint(lat,lon))) }
        listOf(-90.0,0.0,90.0).forEach { lat -> listOf(-180.0,0.0,180.0).forEach { lon ->
            assertTrue(ZoneSuggestion.at(GeoPoint(lat,lon)).contains('/'))
        } }
    }
}
