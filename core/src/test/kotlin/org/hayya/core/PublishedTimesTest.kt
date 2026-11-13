// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.nio.file.*
import java.time.*
import kotlin.math.abs
import kotlin.test.*

class PublishedTimesTest {
    @Test fun `local adapters stay within two minutes of frozen published seasonal method fixtures`() {
        val data = checkNotNull(javaClass.getResourceAsStream("/published-times.tsv")).bufferedReader().use { it.readLines() }.drop(1)
        val deltas = mutableListOf("date\tcity\tprofile\tevent\tminutes_local_minus_published")
        var largest = 0L
        for (line in data) {
            val fields = line.split('\t')
            val date = LocalDate.parse(fields[0]); val zone = ZoneId.of(fields[4])
            val schedule = assertIs<CalculationResult.Available>(LocalPrayerCalculator().calculate(date,
                GeoPoint(fields[2].toDouble(),fields[3].toDouble()),zone,
                CalculationSettings(Profile.valueOf(fields[5]),AsrConvention.valueOf(fields[6])))).schedule
            val actual = listOf(schedule.prayers.getValue(Prayer.FAJR),schedule.sunrise) + Prayer.entries.drop(1).map { schedule.prayers.getValue(it) }
            for ((index,label) in listOf("Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha").withIndex()) {
                val expected = date.atTime(LocalTime.parse(fields[index+7])).atZone(zone).toInstant()
                val delta = Duration.between(expected,actual[index]).toMinutes()
                largest = maxOf(largest,abs(delta))
                deltas += "${fields[0]}\t${fields[1]}\t${fields[5]}\t$label\t$delta"
            }
        }
        val output = Path.of("build/reports/calculations/published-deltas.tsv")
        Files.createDirectories(output.parent); Files.write(output,deltas)
        assertEquals(6,data.size)
        assertTrue(largest <= 2,"Published comparison exceeded two minutes; inspect $output (largest=$largest)")
    }
}
