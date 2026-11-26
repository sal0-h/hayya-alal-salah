// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.hayya.core.GeoPoint
import java.text.Normalizer
import java.util.Locale

data class OfflineCity(val name: String, val country: String, val point: GeoPoint, val zone: String)
class OfflineCities(private val context: Context) {
    // Stream the small index on demand: do not retain every city's aliases in Today memory.
    suspend fun search(query: String): List<OfflineCity> = withContext(Dispatchers.IO) {
        val needle = normalized(query.trim())
        if (needle.length < 2) return@withContext emptyList()
        context.assets.open("geography/cities.tsv").reader(Charsets.UTF_8).buffered(65536).useLines { lines ->
            lines.mapNotNull { line ->
                coroutineContext.ensureActive()
                if (!line.substringAfterLast('\t').contains(needle)) null
                else {
                    val f = line.split('\t')
                    if (f.size != 9) null else OfflineCity(f[1], f[5], GeoPoint(f[3].toDouble(), f[4].toDouble()), f[6])
                }
            }.take(30).toList()
        }
    }
    private val marks = Regex("\\p{M}+|ـ")
    private fun normalized(value: String) = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")
}
