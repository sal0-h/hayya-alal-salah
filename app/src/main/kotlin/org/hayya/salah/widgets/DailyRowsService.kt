// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.widgets

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.*
import org.hayya.salah.R
import org.hayya.salah.alerts.AlarmCoordinator
import org.hayya.salah.alerts.AlertNotifications
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Six local timetable rows. Binder refreshes are event driven, never periodic polling. */
class DailyRowsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = object : RemoteViewsFactory {
        private var rows: List<String> = emptyList()
        private var color = 0xff172d3a.toInt()
        override fun onCreate() = Unit
        override fun onDataSetChanged() {
            rows = emptyList()
            if (!AlarmCoordinator.unlocked(this@DailyRowsService)) return
            runCatching {
                runBlocking(Dispatchers.IO) {
                    withTimeout(5000) {
                        val prefs = appContainer.settings.preferences.first()
                        val place = prefs.place ?: return@withTimeout
                        val context = DeliveryLocale.context(this@DailyRowsService)
                        val locale = context.resources.configuration.locales[0]
                        val use24 = when (prefs.appearance.time) {
                            TimePreference.TWELVE -> false
                            TimePreference.TWENTY_FOUR -> true
                            else -> android.text.format.DateFormat.is24HourFormat(context)
                        }
                        val formatter = DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a",locale)
                            .withDecimalStyle(decimalStyle(locale,prefs.appearance.numerals))
                        val dark = prefs.appearance.theme == ThemePreference.DARK || prefs.appearance.theme == ThemePreference.SYSTEM &&
                            context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                        color = if (dark) 0xffe4edf2.toInt() else 0xff172d3a.toInt()
                        val date = Instant.now().atZone(place.zone).toLocalDate()
                        val schedule = (LocalPrayerCalculator().calculate(date,place.point,place.zone,place.calculation) as? CalculationResult.Available)?.schedule ?: return@withTimeout
                        val events = listOf(AlertNotifications.prayerName(context,Prayer.FAJR) to schedule.prayers.getValue(Prayer.FAJR),context.getString(R.string.sunrise) to schedule.sunrise) +
                            Prayer.entries.drop(1).map { AlertNotifications.prayerName(context,it) to schedule.prayers.getValue(it) }
                        rows = events.map { (name,at) -> "$name    \u2068${at.atZone(place.zone).format(formatter)}\u2069" }
                    }
                }
            }
        }
        override fun onDestroy() { rows = emptyList() }
        override fun getCount() = rows.size
        override fun getViewAt(position: Int) = RemoteViews(packageName,R.layout.widget_daily_row).apply {
            setTextViewText(R.id.widget_row,rows.getOrNull(position).orEmpty()); setTextColor(R.id.widget_row,color)
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = true
    }
}
