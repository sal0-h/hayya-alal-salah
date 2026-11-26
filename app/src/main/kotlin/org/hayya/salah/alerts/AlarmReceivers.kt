// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.PrayerStatus
import org.hayya.salah.appContainer
import org.json.JSONObject
import java.time.ZoneId

private fun BroadcastReceiver.background(context: Context, work: suspend () -> Unit) {
    val result = goAsync()
    val wake = context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hayya:delivery").apply { acquire(25000) }
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try { withTimeout(20000) { work() } }
        catch (_: Exception) { /* No partial playback when snapshot/storage access fails. */ }
        finally { if (wake.isHeld) wake.release(); result.finish() }
    }
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = background(context) {
        val id = intent.getStringExtra("id") ?: return@background
        val token = intent.getStringExtra("token") ?: return@background
        val entry = AlarmCoordinator.deliver(context, id, token) ?: return@background
        if (AlertNotifications.ready(context, entry.isFajr)) {
            AlertNotifications.show(context, entry)
            if (entry.sound != null) AlarmPlayback.start(context, entry)
        }
        AlarmCoordinator.reconcile(context)
    }
}
class AlarmSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_USER_UNLOCKED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED", "org.hayya.salah.MAINTAIN")) return
        background(context) {
            AlarmCoordinator.reconcile(context)
            if (AlarmCoordinator.unlocked(context)) {
                context.appContainer.startIntegrations()
                org.hayya.salah.focus.PrayerFocusService.onTimeChanged()
            }
        }
    }
}
class RecordActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = background(context) {
        val entry = runCatching { AlarmEntry.fromJson(JSONObject(intent.getStringExtra("event") ?: "")) }.getOrNull() ?: return@background
        if (!AlarmCoordinator.unlocked(context) || context.getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked) return@background
        // The action label explicitly states ON_TIME. It retains its original date and zone.
        val repository = context.appContainer.repository
        repository.record(entry.target, PrayerStatus.ON_TIME, ZoneId.of(entry.zone))
        AlarmCoordinator.reconcile(context)
    }
}
