// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.hayya.core.GeoPoint
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale

data class OfflineCity(val name: String, val country: String, val region: String, val point: GeoPoint, val zone: String)

class OfflineCities(private val context: Context) {
    // Read on demand; the Today process does not hold a gazetteer or open it at startup.
    suspend fun search(query: String, locale: Locale = context.resources.configuration.locales[0]): List<OfflineCity> =
        withContext(Dispatchers.IO) {
            val parts = query.split(',').map { normalized(it.trim()) }.filter { it.isNotEmpty() }
            val needle = parts.firstOrNull()?.takeIf { it.length >= 2 } ?: return@withContext emptyList()
            val qualifiers = parts.drop(1)
            val countries = HashMap<String, List<String>>()
            val buckets = List(4) { mutableListOf<OfflineCity>() }
            val supportedZones = ZoneId.getAvailableZoneIds()
            context.assets.open("geography/cities.tsv").reader(Charsets.UTF_8).buffered(65536).useLines { lines ->
                for (line in lines) {
                    coroutineContext.ensureActive()
                    val keyOffset = line.lastIndexOf('\t') + 1
                    if (line.indexOf(needle, keyOffset) < 0) continue
                    val fields = line.split('\t', limit = 9)
                    if (fields.size != 9 || fields[5] !in supportedZones) continue
                    if (qualifiers.isNotEmpty()) {
                        val countryKeys = countries.getOrPut(fields[4]) {
                            val country = Locale.Builder().setRegion(fields[4]).build()
                            listOf(fields[4], country.getDisplayCountry(locale), country.getDisplayCountry(Locale.ENGLISH)).map(::normalized)
                        }
                        val regionKeys = fields[7].split(',')
                        if (qualifiers.any { it !in countryKeys && it !in regionKeys }) continue
                    }
                    val aliases = fields[8].split(',')
                    val rank = when {
                        needle == aliases.firstOrNull() -> 0
                        needle in aliases -> 1
                        aliases.any { it.startsWith(needle) } -> 2
                        else -> 3
                    }
                    val bucket = buckets[rank]
                    if (bucket.size < LIMIT) bucket += OfflineCity(fields[1], fields[4], fields[6],
                        GeoPoint(fields[2].toDouble(), fields[3].toDouble()), fields[5])
                    if (buckets.first().size == LIMIT) break
                }
            }
            buckets.flatten().take(LIMIT)
        }

    private companion object {
        const val LIMIT = 30
        val marks = Regex("\\p{M}+|ـ")
        fun normalized(value: String) =
            Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")
    }
}
