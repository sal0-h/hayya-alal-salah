// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.widgets

import android.app.*
import android.appwidget.*
import android.content.*
import android.os.*
import android.widget.RemoteViews
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hayya.core.*
import org.hayya.salah.*
import org.hayya.salah.R
import org.hayya.salah.alerts.AlarmCoordinator
import org.hayya.salah.alerts.AlertNotifications
import java.time.*
import java.time.format.DateTimeFormatter

private fun BroadcastReceiver.updateAsync(context: Context) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try { withTimeout(15000) { WidgetUpdater.update(context) } }
        catch (_: Exception) { /* Preserve the prior widget if storage is temporarily unavailable. */ }
        finally { pending.finish() }
    }
}
class NextPrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAsync(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateAsync(context)
    override fun onDisabled(context: Context) = updateAsync(context)
}
class DailyPrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAsync(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateAsync(context)
    override fun onDisabled(context: Context) = updateAsync(context)
}
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "org.hayya.salah.WIDGET_BOUNDARY" || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_USER_UNLOCKED ||
            intent.action == Intent.ACTION_LOCALE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) updateAsync(context)
    }
}
object WidgetUpdater {
    private val mutex = Mutex()
    suspend fun update(baseContext: Context) = mutex.withLock {
        val context = DeliveryLocale.context(baseContext)
        val manager = AppWidgetManager.getInstance(context)
        val compact = manager.getAppWidgetIds(ComponentName(context, NextPrayerWidget::class.java))
        val daily = manager.getAppWidgetIds(ComponentName(context, DailyPrayerWidget::class.java))
        val alarm = context.getSystemService(AlarmManager::class.java)
        val refresh = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetRefreshReceiver::class.java).setAction("org.hayya.salah.WIDGET_BOUNDARY"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (compact.isEmpty() && daily.isEmpty()) { alarm.cancel(refresh); return@withLock }
        val unlocked = AlarmCoordinator.unlocked(context)
        val prefs = if (unlocked) context.appContainer.settings.preferences.first() else AppPreferences()
        val place = prefs.place
        val now = Instant.now()
        val today = place?.let { now.atZone(it.zone).toLocalDate() }
        val schedules = if (place == null || today == null) emptyList() else (-1L..2L).mapNotNull { day ->
            (LocalPrayerCalculator().calculate(today.plusDays(day), place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule
        }
        val locale = context.resources.configuration.locales[0]
        val use24 = when (prefs.appearance.time) { TimePreference.TWELVE -> false; TimePreference.TWENTY_FOUR -> true; else -> android.text.format.DateFormat.is24HourFormat(context) }
        val formatter = DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a", locale).withDecimalStyle(decimalStyle(locale, prefs.appearance.numerals))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val dark = prefs.appearance.theme == ThemePreference.DARK || prefs.appearance.theme == ThemePreference.SYSTEM &&
            context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val accent = when (prefs.appearance.palette) {
            Palette.TEAL -> if (dark) 0xff76c9cf.toInt() else 0xff086d79.toInt()
            Palette.INK -> if (dark) 0xff9bbfff.toInt() else 0xff2c4f8f.toInt()
            Palette.PLUM -> if (dark) 0xffdab0d4.toInt() else 0xff76456f.toInt()
        }
        fun base(layout: Int, provenance: Provenance? = null) = RemoteViews(context.packageName, layout).apply {
            setOnClickPendingIntent(R.id.widget_root, open)
            setInt(R.id.widget_root, "setBackgroundColor", if (dark) 0xff101d26.toInt() else 0xfff8fafc.toInt())
            setTextColor(R.id.widget_place, if (dark) 0xffa9b9c5.toInt() else 0xff526875.toInt())
            setTextColor(R.id.widget_title, if (dark) 0xffe4edf2.toInt() else 0xff172d3a.toInt())
            val label = place?.let { location ->
                if (provenance?.quality == CalculationQuality.ESTIMATED_REFERENCE)
                    context.getString(R.string.widget_estimated,"%.2f".format(java.util.Locale.ROOT,provenance.effective.latitude)) + " · " + location.name
                else location.name
            } ?: context.getString(if (unlocked) R.string.widget_setup else R.string.widget_unlock)
            setTextViewText(R.id.widget_place,label)
        }
        val next = nextPrayer(schedules, now)
        compact.forEach { id ->
            val provenance = schedules.firstOrNull { it.date == next?.key?.date }?.provenance
            val views = base(R.layout.widget_next,provenance)
            val constrainedLargeText = context.resources.configuration.fontScale >= 1.5f && manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,160) < 210
            val estimated = provenance?.quality == CalculationQuality.ESTIMATED_REFERENCE
            views.setViewVisibility(R.id.widget_place,if (next != null && constrainedLargeText && !estimated) android.view.View.GONE else android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_title, next?.let { AlertNotifications.prayerName(context, it.key.prayer) } ?: context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_time, next?.at?.atZone(place!!.zone)?.format(formatter).orEmpty())
            views.setTextColor(R.id.widget_time, accent)
            if (next != null) {
                views.setViewVisibility(R.id.widget_countdown,if (constrainedLargeText && estimated) android.view.View.GONE else android.view.View.VISIBLE)
                views.setChronometerCountDown(R.id.widget_countdown, true)
                views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + Duration.between(now, next.at).toMillis(), null, true)
                views.setTextColor(R.id.widget_countdown, if (dark) 0xffe4edf2.toInt() else 0xff172d3a.toInt())
            } else views.setViewVisibility(R.id.widget_countdown, android.view.View.GONE)
            manager.updateAppWidget(id, views)
        }
        val schedule = schedules.firstOrNull { it.date == today }
        val rows = schedule?.let { day ->
            listOf(AlertNotifications.prayerName(context,Prayer.FAJR) to day.prayers.getValue(Prayer.FAJR),
                context.getString(R.string.sunrise) to day.sunrise) + Prayer.entries.drop(1).map { AlertNotifications.prayerName(context,it) to day.prayers.getValue(it) }
        }.orEmpty().map { (name,at) -> "$name    \u2068${at.atZone(place!!.zone).format(formatter)}\u2069" }
        daily.forEach { id ->
            val views = base(R.layout.widget_daily,schedule?.provenance)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.today))
            views.setViewVisibility(R.id.widget_rows,if (unlocked && place != null) android.view.View.VISIBLE else android.view.View.GONE)
            if (Build.VERSION.SDK_INT >= 31) {
                val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
                rows.forEachIndexed { index,text ->
                    items.addItem(index.toLong(),RemoteViews(context.packageName,R.layout.widget_daily_row).apply {
                        setTextViewText(R.id.widget_row,text)
                        setTextColor(R.id.widget_row,if (dark) 0xffe4edf2.toInt() else 0xff172d3a.toInt())
                    })
                }
                views.setRemoteAdapter(R.id.widget_rows,items.build())
            } else {
                @Suppress("DEPRECATION") // The intent adapter supports Android 8–11.
                views.setRemoteAdapter(R.id.widget_rows,Intent(context,DailyRowsService::class.java)
                    .setData("hayya-widget://daily/$id".toUri()))
            }
            manager.updateAppWidget(id, views)
        }
        // Collections scroll at large text sizes; no text shrinking or minute polling.
        @Suppress("DEPRECATION") // RemoteViews collections support the API26 minimum.
        if (Build.VERSION.SDK_INT < 31) manager.notifyAppWidgetViewDataChanged(daily,R.id.widget_rows)
        val boundary = (schedules.flatMap { it.prayers.values + it.sunrise } + listOfNotNull(today?.plusDays(1)?.atStartOfDay(place!!.zone)?.toInstant())).filter { it > now }.minOrNull()
        if (boundary == null) alarm.cancel(refresh)
        else if (AlarmCoordinator.exactAccess(context)) {
            try { alarm.setExact(AlarmManager.RTC, boundary.toEpochMilli(), refresh) }
            catch (_: SecurityException) { alarm.set(AlarmManager.RTC, boundary.toEpochMilli(), refresh) }
        } else alarm.set(AlarmManager.RTC, boundary.toEpochMilli(), refresh)
    }
}
