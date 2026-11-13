// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.annotation.SuppressLint
import android.content.Context
import org.hayya.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Only future delivery targets/configuration. Never coordinates, records, or pause dates. */
data class AlarmEntry(val id: String, val target: PrayerKey, val kind: EventKind, val at: Instant,
    val zone: String, val sound: String?, val difficulty: ChallengeDifficulty,
    val token: String = UUID.randomUUID().toString(), val claimed: Boolean = false) {
    fun samePayload(other: AlarmEntry) = copy(token = "", claimed = false) == other.copy(token = "", claimed = false)
    val isFajr get() = kind == EventKind.FAJR_WAKE_UP
    fun json() = JSONObject().apply {
        put("id", id); put("date", target.date.toString()); put("prayer", target.prayer.name)
        put("kind", kind.name); put("at", at.toEpochMilli()); put("zone", zone); put("sound", sound)
        put("difficulty", difficulty.name); put("token", token); put("claimed", claimed)
    }
    companion object {
        fun fromJson(j: JSONObject) = AlarmEntry(j.getString("id"), PrayerKey(LocalDate.parse(j.getString("date")), Prayer.valueOf(j.getString("prayer"))),
            EventKind.valueOf(j.getString("kind")), Instant.ofEpochMilli(j.getLong("at")), j.getString("zone"),
            if (j.isNull("sound")) null else j.getString("sound"), ChallengeDifficulty.valueOf(j.getString("difficulty")), j.getString("token"), j.optBoolean("claimed"))
    }
}
class AlarmSnapshot(context: Context, name: String = "upcoming-alarms") {
    private val prefs = context.createDeviceProtectedStorageContext().getSharedPreferences(name, Context.MODE_PRIVATE)
    @Synchronized fun read(): List<AlarmEntry> {
        return try {
            val array = JSONArray(prefs.getString("events", "[]"))
            (0 until array.length()).map { AlarmEntry.fromJson(array.getJSONObject(it)) }
        } catch (_: org.json.JSONException) { replace(emptyList()); emptyList() }
        catch (_: IllegalArgumentException) { replace(emptyList()); emptyList() }
        catch (_: java.time.DateTimeException) { replace(emptyList()); emptyList() }
        // This technical cache can be rebuilt. Room history is never cleared on errors.
    }
    // Delivery must persist a claim before any audible side effect, including process death.
    @SuppressLint("ApplySharedPref", "UseKtx")
    @Synchronized fun replace(entries: List<AlarmEntry>) {
        check(prefs.edit().putString("events", JSONArray(entries.map { it.json() }).toString()).commit())
    }
    @Synchronized fun claim(id: String, token: String, now: Instant): AlarmEntry? {
        val entries = read()
        val entry = entries.firstOrNull { it.id == id && it.token == token && !it.claimed } ?: return null
        val tolerance = if (entry.isFajr) 600 else if (entry.kind == EventKind.AT_TIME) 300 else 120
        if (now < entry.at.minusSeconds(2) || now > entry.at.plusSeconds(tolerance.toLong())) return null
        replace(entries.map { if (it.id == id) it.copy(claimed = true) else it })
        return entry
    }
}
