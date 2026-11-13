// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import androidx.core.app.NotificationCompat
import org.hayya.core.*
import org.hayya.salah.*

object AlertNotifications {
    const val PRAYERS = "prayer-times-v1"
    const val FAJR = "fajr-wake-up-v1"
    const val PLAYBACK_ID = 600
    fun channels(context: Context) {
        val strings = DeliveryLocale.context(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(PRAYERS, strings.getString(R.string.prayer_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = strings.getString(R.string.alert_channel_description)
            // Playback is controlled per prayer, without fighting user channel choices.
            setSound(null, null)
        })
        manager.createNotificationChannel(NotificationChannel(FAJR, strings.getString(R.string.fajr_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            description = strings.getString(R.string.fajr_channel_description)
            setSound(null, null); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }
    fun ready(context: Context, fajr: Boolean): Boolean {
        channels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        return (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.areNotificationsEnabled() && manager.getNotificationChannel(if (fajr) FAJR else PRAYERS)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun open(context: Context, entry: AlarmEntry): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).setData("hayya://record/${entry.id}".toUri())
            .putExtra("serviceDate", entry.target.date.toString()).putExtra("prayer", entry.target.prayer.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun ringing(context: Context, entry: AlarmEntry): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, RingingActivity::class.java).setData("hayya://ring/${entry.id}".toUri())
            .putExtra("event", entry.json().toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun onTime(context: Context, entry: AlarmEntry): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, RecordActionReceiver::class.java).setData("hayya://on-time/${entry.id}".toUri())
            .putExtra("event", entry.json().toString()), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun prayerName(context: Context, prayer: Prayer): String = DeliveryLocale.context(context).getString(when (prayer) {
        Prayer.FAJR -> R.string.fajr; Prayer.DHUHR -> R.string.dhuhr; Prayer.ASR -> R.string.asr; Prayer.MAGHRIB -> R.string.maghrib; Prayer.ISHA -> R.string.isha
    })
    fun build(context: Context, entry: AlarmEntry, playing: Boolean = false): Notification {
        val strings = DeliveryLocale.context(context)
        val title = if (entry.isFajr) strings.getString(R.string.fajr_alarm) else prayerName(context, entry.target.prayer)
        val builder = NotificationCompat.Builder(context, if (entry.isFajr) FAJR else PRAYERS)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
            .setContentText(strings.getString(when(entry.kind) { EventKind.BEFORE -> R.string.before_reminder; EventKind.AFTER -> R.string.after_reminder; else -> R.string.prayer_reminder }))
            .setCategory(if (entry.isFajr) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(true)
            .setContentIntent(if (entry.isFajr) ringing(context, entry) else open(context, entry)).setAutoCancel(!entry.isFajr)
        if (entry.isFajr) {
            builder.setOngoing(playing).addAction(0, strings.getString(R.string.open_alarm), ringing(context, entry))
            if (Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent())
                builder.setFullScreenIntent(ringing(context, entry), true)
        } else {
            val action = NotificationCompat.Action.Builder(0, strings.getString(R.string.record_on_time), onTime(context, entry))
            if (Build.VERSION.SDK_INT >= 31) action.setAuthenticationRequired(true)
            builder.addAction(action.build()).addAction(0, strings.getString(R.string.record_prayer), open(context, entry))
            if (playing) builder.addAction(0, strings.getString(R.string.stop_audio), AlarmPlayback.stopPending(context))
        }
        return builder.build()
    }
    fun show(context: Context, entry: AlarmEntry) {
        if (ready(context, entry.isFajr)) context.getSystemService(NotificationManager::class.java).notify(entry.id, 1, build(context, entry))
    }
    fun cancelSuppressed(context: Context, history: HistorySnapshot) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.forEach { status ->
            val tag = status.tag ?: return@forEach
            val parts = tag.split('/')
            val key = runCatching { PrayerKey(java.time.LocalDate.parse(parts[0]), Prayer.valueOf(parts[1])) }.getOrNull() ?: return@forEach
            if (history.pauses.any { key.date in it } || history.records[key]?.status in setOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.EXCUSED)) manager.cancel(tag, status.id)
        }
    }
}
