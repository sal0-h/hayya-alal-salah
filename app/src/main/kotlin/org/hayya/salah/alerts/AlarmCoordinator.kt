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
        val trigger = entry.at.toEpochMilli()
        try {
            if (entry.isFajr) {
                if (!exactAccess(context) || !AlertNotifications.ready(context, true)) return
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, AlertNotifications.open(context, entry)), operation)
            } else if (entry.kind == EventKind.AT_TIME) {
                if (exactAccess(context)) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
            } else {
                // Optional reminders can wait for a maintenance window without using the idle quota.
                alarms.set(AlarmManager.RTC_WAKEUP, trigger, operation)
            }
        } catch (_: SecurityException) { /* Revoked concurrently; readiness screen reports missing access. */ }
    }
    private fun maintenance(context: Context) = PendingIntent.getBroadcast(context, 0,
        Intent(context, AlarmSystemReceiver::class.java).setAction("org.hayya.salah.MAINTAIN"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun scheduleMaintenance(context: Context) {
        manager(context).set(AlarmManager.RTC_WAKEUP, Instant.now().plus(Duration.ofHours(12)).toEpochMilli(), maintenance(context))
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
        DeliveryLocale.save(context, prefs)
        val history = app.repository.snapshot()
        val now = Instant.now()
        val place = prefs.place
        val rules = buildSet {
            prefs.alerts.forEach { (prayer, alert) -> if (alert.mode != AlertMode.OFF) {
                add(AlertRule(prayer, EventKind.AT_TIME))
                alert.beforeMinutes?.let { add(AlertRule(prayer, EventKind.BEFORE, Duration.ofMinutes(-it.toLong()))) }
                alert.afterMinutes?.let { add(AlertRule(prayer, EventKind.AFTER, Duration.ofMinutes(it.toLong()))) }
            } }
        }
        val scheduleFajr = prefs.fajr.enabled && exactAccess(context) && AlertNotifications.ready(context, true)
        val dates = place?.let { alarmDates(now, it.zone) }.orEmpty()
        val schedules = if (place == null || (rules.isEmpty() && !(scheduleFajr && prefs.fajr.mode == FajrMode.RELATIVE)))
            emptyList() else calculateAlarmDays(place, dates)
        val fajrEvents = if (scheduleFajr && place != null)
            compileFajrAlarms(dates, place.zone, prefs.fajr.timing, fajrByDate(schedules), history.records, history.pauses, now)
            else emptyList()
        val previous = store.read().associateBy { it.id }
        val events = compileSchedule(schedules, rules, history.records, history.pauses, now, 0) + fajrEvents
        val desired = events.map { event ->
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
                (!active.isFajr && prefs.alerts[active.target.prayer]?.mode != AlertMode.ADHAN) || reminderSuppressed(active.target, now, place.zone, history.records, history.pauses))) {
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
            if (place.zone.id != entry.zone) return@withLock null
            val expected = expectedTime(entry, place, prefs) ?: return@withLock null
            val now = Instant.now()
            if (expected != entry.at ||
                reminderSuppressed(entry.target, entry.at, place.zone, history.records, history.pauses) ||
                reminderSuppressed(entry.target, now, place.zone, history.records, history.pauses)) return@withLock null
        }
        entry
    }
    private fun expectedTime(entry: AlarmEntry, place: ManualSetup, prefs: AppPreferences): Instant? {
        if (entry.isFajr) {
            if (!prefs.fajr.enabled) return null
            val timing = prefs.fajr.timing
            val fajr = if (timing is FajrTiming.Relative)
                calculateAlarmDays(place, listOf(entry.target.date)).singleOrNull()?.prayers?.get(Prayer.FAJR) else null
            return fajrAlarmAt(entry.target.date, place.zone, timing, fajr)
        }
        val rule = prefs.alerts.getValue(entry.target.prayer)
        if (rule.mode == AlertMode.OFF) return null
        val minutes = when (entry.kind) {
            EventKind.AT_TIME -> 0
            EventKind.BEFORE -> -(rule.beforeMinutes ?: return null)
            EventKind.AFTER -> rule.afterMinutes ?: return null
            EventKind.FAJR_WAKE_UP -> return null
        }
        val schedule = calculateAlarmDays(place, listOf(entry.target.date)).singleOrNull() ?: return null
        return schedule.prayers.getValue(entry.target.prayer).plusSeconds(minutes * 60L)
    }

    suspend fun cancelAll(context: Context) = mutex.withLock {
        val store = AlarmSnapshot(context)
        store.read().forEach { manager(context).cancel(pending(context, it)) }
        store.replace(emptyList()); manager(context).cancel(maintenance(context))
        AlarmPlayback.stop(context)
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
    }
}
