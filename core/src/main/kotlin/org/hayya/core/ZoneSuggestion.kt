// SPDX-License-Identifier: CC0-1.0
// Port of PhotoStructure/darksky tz-lookup traversal, a09cbf7a1874937d81f42ccb1bdad4ac88d0400e.
// Data: ODbL-1.0, OpenStreetMap contributors/timezone-boundary-builder. See docs/GEOGRAPHY.md.
package org.hayya.core

/** Approximate offline suggestion, never authority to silently change a selected zone. */
object ZoneSuggestion {
    private val tree by lazy { requireNotNull(javaClass.getResourceAsStream("/geography/timezone-tree.txt")).bufferedReader().use { it.readLines() } }
    fun at(point: GeoPoint): String {
        if (point.latitude >= 90) return "Etc/GMT"
        val data = tree.first()
        val zones = tree.drop(1)
        var n = -1
        var x = ((180 + point.longitude) * 48) / 360.00000000000006
        var y = ((90 - point.latitude) * 24) / 180.00000000000003
        var u = x.toInt(); var v = y.toInt()
        fun node(index: Int) = data[index].code * 56 + data[index + 1].code - 1995
        var i = node(v * 96 + u * 2)
        while (i + zones.size < 3136) {
            n += i + 1
            x = ((x - u) * 2) % 2; y = ((y - v) * 2) % 2
            u = x.toInt(); v = y.toInt()
            i = node(n * 8 + v * 4 + u * 2 + 2304)
        }
        return zones[i + zones.size - 3136]
    }
}
