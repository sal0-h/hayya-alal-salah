// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import org.hayya.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AlertMode { OFF, NOTIFICATION, ADHAN }
data class PrayerAlert(val mode: AlertMode = AlertMode.OFF, val beforeMinutes: Int? = null,
    val afterMinutes: Int? = null, val sound: String = "builtin_adhan") {
    init { require(listOfNotNull(beforeMinutes, afterMinutes).all { it in 1..120 }) }
}
data class FajrPreferences(val enabled: Boolean = false, val offsetMinutes: Int = 0,
    val sound: String = "builtin_fajr", val difficulty: ChallengeDifficulty = ChallengeDifficulty.OFF) {
    init { require(offsetMinutes in -180..180) }
}
data class FocusPreferences(val enabled: Boolean = false, val packages: Set<String> = emptySet(),
    val prayers: Set<Prayer> = Prayer.entries.toSet(), val bypass: FocusBypass? = null)
enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class Palette { TEAL, INK, PLUM }
enum class TimePreference { SYSTEM, TWELVE, TWENTY_FOUR }
enum class NumeralPreference { SYSTEM, LATIN, ARABIC_INDIC }
data class AppearancePreferences(val theme: ThemePreference = ThemePreference.SYSTEM, val palette: Palette = Palette.TEAL,
    val time: TimePreference = TimePreference.SYSTEM, val numerals: NumeralPreference = NumeralPreference.SYSTEM,
    val reducedMotion: Boolean = false)
data class AppPreferences(val place: ManualSetup? = null,
    val alerts: Map<Prayer, PrayerAlert> = Prayer.entries.associateWith { PrayerAlert() },
    val fajr: FajrPreferences = FajrPreferences(), val focus: FocusPreferences = FocusPreferences(),
    val appearance: AppearancePreferences = AppearancePreferences(), val language: String = "system",
    val hijriAdjustment: Int = 0, val trackingSince: LocalDate? = null) {
    init { require(hijriAdjustment in -2..2); require(language in setOf("system", "en", "ru", "az", "ar")) }
}

data class ManualSetup(val name: String, val point: GeoPoint, val zone: ZoneId,
    val calculation: CalculationSettings, val automatic: Boolean = false, val refreshedAt: Instant? = null) {
    constructor(name: String, point: GeoPoint, zone: ZoneId, profile: Profile) : this(name, point, zone, CalculationSettings(profile))
    val profile get() = calculation.profile
    init { require(name.isNotBlank()); require(zone.id in ZoneId.getAvailableZoneIds()) }
}

/** Versioned, atomic preferences. Missing new fields migrate from established defaults. */
internal object PreferencesCodec {
    private fun JSONObject.optionalDouble(key: String) = if (isNull(key)) null else getDouble(key)
    private fun JSONObject.optionalInt(key: String) = if (isNull(key)) null else getInt(key)
    private fun JSONObject.optionalString(key: String) = if (isNull(key)) null else getString(key)
    private inline fun <reified T : Enum<T>> JSONObject.enum(key: String, default: T): T =
        if (has(key)) enumValueOf(getString(key)) else default
    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
    fun decode(text: String): AppPreferences {
        val root = JSONObject(text)
        require(root.optInt("version", 1) == 1)
        val place = root.optJSONObject("place")?.let { p ->
            val corrections = p.optJSONObject("corrections") ?: JSONObject()
            val overrides = p.optJSONObject("overrides") ?: JSONObject()
            ManualSetup(p.getString("name"), GeoPoint(p.getDouble("latitude"), p.getDouble("longitude")), ZoneId.of(p.getString("zone")),
                CalculationSettings(p.enum("profile", Profile.MWL), p.enum("asr", AsrConvention.STANDARD),
                    p.enum("highLatitude", HighLatitude.MIDDLE_OF_NIGHT),
                    Prayer.entries.filter { corrections.has(it.name) }.associateWith { corrections.getInt(it.name) },
                    ParameterOverrides(overrides.optionalDouble("fajr"), overrides.optionalDouble("isha"),
                        overrides.optionalInt("interval"), overrides.optionalDouble("maghrib")),
                    p.enum("polar", PolarMode.ASK), p.optionalDouble("referenceLatitude")),
                p.optBoolean("automatic", false), p.optionalString("refreshedAt")?.let(Instant::parse))
        }
        val alerts = root.optJSONObject("alerts") ?: JSONObject()
        val fajr = root.optJSONObject("fajr") ?: JSONObject()
        val focus = root.optJSONObject("focus") ?: JSONObject()
        val appearance = root.optJSONObject("appearance") ?: JSONObject()
        return AppPreferences(place,
            Prayer.entries.associateWith { prayer -> (alerts.optJSONObject(prayer.name) ?: JSONObject()).let {
                PrayerAlert(it.enum("mode", AlertMode.OFF), it.optionalInt("before"), it.optionalInt("after"), it.optString("sound", "builtin_adhan")) } },
            FajrPreferences(fajr.optBoolean("enabled", false), fajr.optInt("offset", 0), fajr.optString("sound", "builtin_fajr"), fajr.enum("difficulty", ChallengeDifficulty.OFF)),
            FocusPreferences(focus.optBoolean("enabled", false), focus.optJSONArray("packages")?.strings()?.toSet() ?: emptySet(),
                focus.optJSONArray("prayers")?.strings()?.map(Prayer::valueOf)?.toSet() ?: Prayer.entries.toSet(),
                focus.optJSONObject("bypass")?.let { FocusBypass(PrayerKey(LocalDate.parse(it.getString("date")), Prayer.valueOf(it.getString("prayer"))), Instant.parse(it.getString("expires"))) }),
            AppearancePreferences(appearance.enum("theme", ThemePreference.SYSTEM), appearance.enum("palette", Palette.TEAL),
                appearance.enum("time", TimePreference.SYSTEM), appearance.enum("numerals", NumeralPreference.SYSTEM), appearance.optBoolean("reducedMotion", false)),
            root.optString("language", "system"), root.optInt("hijriAdjustment", 0), root.optionalString("trackingSince")?.let(LocalDate::parse))
    }
    fun encode(config: AppPreferences): String = JSONObject().apply {
        put("version", 1)
        config.place?.let { p -> put("place", JSONObject().apply {
            put("name", p.name); put("latitude", p.point.latitude); put("longitude", p.point.longitude); put("zone", p.zone.id)
            put("profile", p.profile.name); put("asr", p.calculation.asr.name); put("highLatitude", p.calculation.highLatitude.name)
            put("polar", p.calculation.polar.name); put("referenceLatitude", p.calculation.referenceLatitude)
            put("automatic", p.automatic); put("refreshedAt", p.refreshedAt?.toString())
            put("corrections", JSONObject().apply { p.calculation.corrections.forEach { (prayer, offset) -> put(prayer.name, offset) } })
            put("overrides", JSONObject().apply { val o = p.calculation.overrides; put("fajr", o.fajrAngle); put("isha", o.ishaAngle); put("interval", o.ishaInterval); put("maghrib", o.maghribAngle) })
        }) }
        put("alerts", JSONObject().apply { config.alerts.forEach { (prayer, alert) -> put(prayer.name, JSONObject().apply {
            put("mode", alert.mode.name); put("before", alert.beforeMinutes); put("after", alert.afterMinutes); put("sound", alert.sound)
        }) } })
        put("fajr", JSONObject().apply { put("enabled", config.fajr.enabled); put("offset", config.fajr.offsetMinutes); put("sound", config.fajr.sound); put("difficulty", config.fajr.difficulty.name) })
        put("focus", JSONObject().apply { put("enabled", config.focus.enabled); put("packages", JSONArray(config.focus.packages.toList())); put("prayers", JSONArray(config.focus.prayers.map { it.name }))
            config.focus.bypass?.let { put("bypass", JSONObject().apply { put("date", it.target.date.toString()); put("prayer", it.target.prayer.name); put("expires", it.expiresAt.toString()) }) }
        })
        put("appearance", JSONObject().apply { put("theme", config.appearance.theme.name); put("palette", config.appearance.palette.name); put("time", config.appearance.time.name); put("numerals", config.appearance.numerals.name); put("reducedMotion", config.appearance.reducedMotion) })
        put("language", config.language); put("hijriAdjustment", config.hijriAdjustment); put("trackingSince", config.trackingSince?.toString())
    }.toString()
}
