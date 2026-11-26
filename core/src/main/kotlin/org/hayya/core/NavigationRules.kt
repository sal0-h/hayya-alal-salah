// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Qibla
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val year: Int, val month: Int, val day: Int)
fun ummAlQuraDisplay(date: LocalDate, adjustment: Int): HijriDate {
    require(adjustment in -2..2)
    val h = HijrahDate.from(date.plusDays(adjustment.toLong()))
    return HijriDate(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
}
fun qiblaBearing(actual: GeoPoint): Double = Qibla(Coordinates(actual.latitude, actual.longitude)).direction
