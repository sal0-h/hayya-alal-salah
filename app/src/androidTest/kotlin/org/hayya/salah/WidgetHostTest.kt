// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.appwidget.*
import android.content.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.widgets.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneId

/** Binds actual providers through the OS and renders their real RemoteViews. */
@RunWith(AndroidJUnit4::class)
class WidgetHostTest {
    @Test fun actualHostUpdatesResizesScrollsAndClearsWithoutRecording() = runBlocking {
        assumeTrue("Synthetic host fixture is emulator only",Build.HARDWARE in setOf("ranchu","goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val original = context.appContainer.settings.preferences.first()
        val before = context.appContainer.repository.snapshot()
        var activity: CaptureHostActivity? = null
        var host: AppWidgetHost? = null
        val ids = mutableListOf<Int>()
        try {
            context.appContainer.settings.update { AppPreferences(ManualSetup("Baku · Bakı",GeoPoint(40.4093,49.8671),ZoneId.of("Asia/Baku"),Profile.MWL),appearance = AppearancePreferences(theme = ThemePreference.LIGHT)) }
            DeliveryLocale.save(context,context.appContainer.settings.preferences.first())
            shell("appwidget grantbind --package ${context.packageName} --user 0")
            activity = instrumentation.startActivitySync(Intent(context,CaptureHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as CaptureHostActivity
            val manager = AppWidgetManager.getInstance(context)
            val views = mutableListOf<AppWidgetHostView>()
            val container = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            instrumentation.runOnMainSync {
                activity.setContentView(container)
                host = AppWidgetHost(activity,7301).apply { startListening() }
                for (provider in listOf(NextPrayerWidget::class.java,DailyPrayerWidget::class.java)) {
                    val id = host!!.allocateAppWidgetId(); ids += id
                    assertTrue(manager.bindAppWidgetIdIfAllowed(id,ComponentName(context,provider)))
                    val view = host!!.createView(activity,id,checkNotNull(manager.getAppWidgetInfo(id)))
                    views += view
                    container.addView(view,LinearLayout.LayoutParams(-1,if (provider == NextPrayerWidget::class.java) 480 else 800))
                }
            }
            WidgetUpdater.update(context)
            suspend fun waitFor(test: () -> Boolean) {
                withTimeout(12000) {
                    var done = false
                    while (!done) { instrumentation.runOnMainSync { done = test() }; if (!done) delay(100) }
                }
            }
            waitFor { views[0].findViewById<TextView>(R.id.widget_place)?.text?.contains("Baku") == true &&
                views[0].findViewById<TextView>(R.id.widget_time)?.text?.isNotEmpty() == true &&
                views[1].findViewById<ListView>(R.id.widget_rows)?.count == 6 }
            val countdown = views[0].findViewById<Chronometer>(R.id.widget_countdown)
            assertTrue(countdown.isCountDown); assertTrue(countdown.base > SystemClock.elapsedRealtime())
            for (height in listOf(560,1000)) {
                instrumentation.runOnMainSync {
                    views[1].layoutParams = LinearLayout.LayoutParams(-1,height)
                    manager.updateAppWidgetOptions(ids[1],Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,220)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,320)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,height/3)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height/3)
                    })
                    views[1].findViewById<ListView>(R.id.widget_rows).setSelection(5)
                }
                waitFor { views[1].findViewById<ListView>(R.id.widget_rows).lastVisiblePosition == 5 }
                instrumentation.waitForIdleSync(); delay(200)
                InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { output ->
                    val directory = File(output,"widgets").apply { mkdirs() }
                    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                    directory.resolve("actual-host-height-$height.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
                }
            }
            context.appContainer.settings.update { AppPreferences() }; WidgetUpdater.update(context)
            waitFor { views[0].findViewById<TextView>(R.id.widget_time)?.text?.isEmpty() == true &&
                views[1].findViewById<ListView>(R.id.widget_rows)?.count == 0 }
            assertEquals(before.records,context.appContainer.repository.snapshot().records)
        } finally {
            instrumentation.runOnMainSync {
                ids.forEach { host?.deleteAppWidgetId(it) }; host?.stopListening(); host?.deleteHost(); activity?.finish()
            }
            shell("appwidget revokebind --package ${context.packageName} --user 0")
            context.appContainer.settings.update { original }; DeliveryLocale.save(context,original); WidgetUpdater.update(context)
        }
    }
}
