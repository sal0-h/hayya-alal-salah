// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.UserManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.*
import java.time.Duration
import java.time.Instant

object AlarmCoordinator {
    private val mutex = Mutex()
    private fun manager(context: Context) = context.getSystemService(AlarmManager::class.java)
    fun exactAccess(context: Context) = Build.VERSION.SDK_INT < 31 || manager(context).canScheduleExactAlarms()
    fun unlocked(context: Context) = context.getSystemService(UserManager::class.java).isUserUnlocked
    private fun pending(context: Context, entry: AlarmEntry) = PendingIntent.getBroadcast(context, 0,
        Intent(context, AlarmReceiver::class.java).setAction("org.hayya.salah.DELIVER")
            .setData("hayya://alarm/${entry.id}".toUri()).putExtra("id", entry.id).putExtra("token", entry.token),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun install(context: Context, entry: AlarmEntry) {
        if (entry.claimed || entry.at <= Instant.now()) return
        val alarms = manager(context)
        val operation = pending(context, entry)
        try {
            if (entry.isFajr) {
                if (!exactAccess(context) || !AlertNotifications.ready(context, true)) return
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(entry.at.toEpochMilli(), AlertNotifications.open(context, entry)), operation)
            } else if (entry.kind == EventKind.AT_TIME && exactAccess(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, entry.at.toEpochMilli(), operation)
            } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, entry.at.toEpochMilli(), operation)
        } catch (_: SecurityException) { /* Revoked concurrently; readiness screen reports missing access. */ }
    }
    private fun maintenance(context: Context) = PendingIntent.getBroadcast(context, 0,
        Intent(context, AlarmSystemReceiver::class.java).setAction("org.hayya.salah.MAINTAIN"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun scheduleMaintenance(context: Context) {
        manager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Instant.now().plus(Duration.ofHours(12)).toEpochMilli(), maintenance(context))
    }
    suspend fun reconcile(context: Context) = mutex.withLock {
        val store = AlarmSnapshot(context)
        if (!unlocked(context)) {
            store.read().forEach { install(context, it) }
            if (store.read().any { !it.claimed && it.at > Instant.now() }) scheduleMaintenance(context)
            else manager(context).cancel(maintenance(context))
            return@withLock
        }
        val app = context.appContainer
        val prefs = app.settings.preferences.first()
        DeliveryLocale.save(context, prefs.language)
        val history = app.repository.snapshot()
        val now = Instant.now()
        val place = prefs.place
        val schedules = if (place == null) emptyList() else (-1L..7L).mapNotNull { offset ->
            (LocalPrayerCalculator().calculate(now.atZone(place.zone).toLocalDate().plusDays(offset), place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule
        }
        val rules = buildSet {
            prefs.alerts.forEach { (prayer, alert) -> if (alert.mode != AlertMode.OFF) {
                add(AlertRule(prayer, EventKind.AT_TIME))
                alert.beforeMinutes?.let { add(AlertRule(prayer, EventKind.BEFORE, Duration.ofMinutes(-it.toLong()))) }
                alert.afterMinutes?.let { add(AlertRule(prayer, EventKind.AFTER, Duration.ofMinutes(it.toLong()))) }
            } }
            if (prefs.fajr.enabled && exactAccess(context) && AlertNotifications.ready(context, true))
                add(AlertRule(Prayer.FAJR, EventKind.FAJR_WAKE_UP, Duration.ofMinutes(prefs.fajr.offsetMinutes.toLong())))
        }
        val previous = store.read().associateBy { it.id }
        val desired = compileSchedule(schedules, rules, history.records, history.pauses, now, 0).map { event ->
            val sound = when {
                event.kind == EventKind.FAJR_WAKE_UP -> prefs.fajr.sound
                event.kind == EventKind.AT_TIME && prefs.alerts[event.target.prayer]?.mode == AlertMode.ADHAN -> prefs.alerts[event.target.prayer]?.sound
                else -> null
            }
            val candidate = AlarmEntry(event.id, event.target, event.kind, event.at, place!!.zone.id, sound,
                if (event.kind == EventKind.FAJR_WAKE_UP) prefs.fajr.difficulty else ChallengeDifficulty.OFF)
            previous[event.id]?.takeIf { it.samePayload(candidate) } ?: candidate
        }
        store.replace(desired)
        previous.values.filter { old -> desired.none { it.id == old.id && it.token == old.token } }
            .forEach { manager(context).cancel(pending(context, it)) }
        desired.forEach { install(context, it) }
        val active = AlarmPlayback.active
        if (active != null && (place == null || (active.isFajr && !prefs.fajr.enabled) ||
                (!active.isFajr && prefs.alerts[active.target.prayer]?.mode != AlertMode.ADHAN) || history.pauses.any { active.target.date in it } ||
                history.records[active.target]?.status in setOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED))) {
            AlarmPlayback.stop(context)
        }
        AlertNotifications.cancelSuppressed(context, history)
        if (desired.isNotEmpty()) scheduleMaintenance(context) else manager(context).cancel(maintenance(context))
    }
    suspend fun deliver(context: Context, id: String, token: String): AlarmEntry? = mutex.withLock {
        val entry = AlarmSnapshot(context).claim(id, token, Instant.now()) ?: return@withLock null
        if (unlocked(context)) {
            val app = context.appContainer
            val history = app.repository.snapshot()
            val prefs = app.settings.preferences.first()
            val place = prefs.place ?: return@withLock null
            val rule = prefs.alerts.getValue(entry.target.prayer)
            val offset = when (entry.kind) {
                EventKind.AT_TIME -> if (rule.mode != AlertMode.OFF) 0 else return@withLock null
                EventKind.BEFORE -> if (rule.mode != AlertMode.OFF) -(rule.beforeMinutes ?: return@withLock null) else return@withLock null
                EventKind.AFTER -> if (rule.mode != AlertMode.OFF) rule.afterMinutes ?: return@withLock null else return@withLock null
                EventKind.FAJR_WAKE_UP -> if (prefs.fajr.enabled) prefs.fajr.offsetMinutes else return@withLock null
            }
            val schedule = (LocalPrayerCalculator().calculate(entry.target.date, place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule ?: return@withLock null
            if (place.zone.id != entry.zone || schedule.prayers.getValue(entry.target.prayer).plusSeconds(offset * 60L) != entry.at) return@withLock null
            if (history.pauses.any { entry.target.date in it } || history.records[entry.target]?.status in
                setOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED)) return@withLock null
        }
        entry
    }
    suspend fun cancelAll(context: Context) = mutex.withLock {
        val store = AlarmSnapshot(context)
        store.read().forEach { manager(context).cancel(pending(context, it)) }
        store.replace(emptyList()); manager(context).cancel(maintenance(context))
        AlarmPlayback.stop(context)
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
    }
}
