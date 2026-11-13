// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}
enum class Profile(val engine: String) {
    MWL("adhan"), ISNA("adhan"), EGYPTIAN("adhan"), KARACHI("adhan"),
    UMM_AL_QURA("adhan"), DUBAI("adhan"), QATAR("adhan"), KUWAIT("adhan"),
    MOONSIGHTING("adhan"), SINGAPORE("adhan"), TURKEY("adhan"),
    FRANCE("praytimes"), RUSSIA("praytimes"), TEHRAN("praytimes"), JAFARI("praytimes")
}
enum class AsrConvention { STANDARD, HANAFI }
enum class HighLatitude { MIDDLE_OF_NIGHT, SEVENTH_OF_NIGHT, TWILIGHT_ANGLE }
enum class PolarMode { ASK, DISABLED, NEAREST, REFERENCE }
enum class CalculationQuality { CALCULATED, ESTIMATED_REFERENCE }
data class ParameterOverrides(
    val fajrAngle: Double? = null,
    val ishaAngle: Double? = null,
    val ishaInterval: Int? = null,
    val maghribAngle: Double? = null,
) {
    init {
        require(listOfNotNull(fajrAngle, ishaAngle, maghribAngle).all { it.isFinite() && it in 0.1..30.0 })
        require(ishaInterval == null || ishaInterval in 1..300)
        require(ishaAngle == null || ishaInterval == null)
    }
}
data class CalculationSettings(
    val profile: Profile = Profile.MWL,
    val asr: AsrConvention = AsrConvention.STANDARD,
    val highLatitude: HighLatitude = HighLatitude.MIDDLE_OF_NIGHT,
    val corrections: Map<Prayer, Int> = emptyMap(),
    val overrides: ParameterOverrides = ParameterOverrides(),
    val polar: PolarMode = PolarMode.ASK,
    val referenceLatitude: Double? = null,
) {
    init {
        require(corrections.values.all { it in -120..120 })
        require(overrides.maghribAngle == null || profile.engine == "praytimes")
        require(referenceLatitude == null || (referenceLatitude.isFinite() && referenceLatitude in -89.0..89.0))
        require(polar != PolarMode.REFERENCE || referenceLatitude != null)
    }
}
data class Provenance(
    val engineVersion: String,
    val profile: Profile,
    val actual: GeoPoint,
    val effective: GeoPoint,
    val settings: CalculationSettings,
    val quality: CalculationQuality = CalculationQuality.CALCULATED,
)
data class DailySchedule(
    val date: LocalDate,
    val zone: ZoneId,
    val prayers: Map<Prayer, Instant>,
    val sunrise: Instant,
    val provenance: Provenance,
    val midnight: Instant? = null,
) {
    init {
        require(prayers.keys == Prayer.entries.toSet())
        val ordered = listOf(prayers.getValue(Prayer.FAJR), sunrise, prayers.getValue(Prayer.DHUHR),
            prayers.getValue(Prayer.ASR), prayers.getValue(Prayer.MAGHRIB), prayers.getValue(Prayer.ISHA))
        require(ordered.zipWithNext().all { (a, b) -> a < b })
        require(prayers.getValue(Prayer.DHUHR).atZone(zone).toLocalDate() == date)
        require(java.time.Duration.between(ordered.first(), ordered.last()).toHours() < 24)
        require(midnight == null || midnight > prayers.getValue(Prayer.MAGHRIB))
    }
}
sealed interface CalculationResult {
    data class Available(val schedule: DailySchedule) : CalculationResult
    data class Unavailable(val reason: Reason) : CalculationResult
    data class NeedsPolarConfirmation(val referenceLatitude: Double) : CalculationResult
    enum class Reason { NO_VALID_SOLAR_EVENTS, INVALID_ORDER_OR_DATE, UNSUPPORTED_DATE }
}
fun interface PrayerCalculator {
    fun calculate(date: LocalDate, point: GeoPoint, zone: ZoneId, settings: CalculationSettings): CalculationResult
}
