// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.app.LocaleManager
import android.appwidget.*
import android.content.*
import android.os.*
import android.widget.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.widgets.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneId

/** Actual OS-hosted widgets, separately captured from the ordinary integration suite. */
@RunWith(AndroidJUnit4::class)
class WidgetAppearanceTest {
    @Test fun captureRealWidgetsInFourLanguagesThemesAndActualSystemFonts() = runBlocking {
        check(Build.HARDWARE in setOf("ranchu","goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val oldScale = shell("settings get system font_scale")
        val oldLocales = if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales else null
        val original = context.appContainer.settings.preferences.first()
        val directory = File(checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")),"widget-screenshots").apply { mkdirs() }
        val manager = AppWidgetManager.getInstance(context)
        var host: AppWidgetHost? = null
        val ids = mutableListOf<Int>()
        var activity: CaptureHostActivity? = null
        try {
            shell("appwidget grantbind --package ${context.packageName} --user 0")
            instrumentation.runOnMainSync {
                host = AppWidgetHost(context,7302).apply { startListening() }
                for (provider in listOf(NextPrayerWidget::class.java,DailyPrayerWidget::class.java)) {
                    val id = checkNotNull(host).allocateAppWidgetId(); ids += id
                    assertTrue(manager.bindAppWidgetIdIfAllowed(id,ComponentName(context,provider)))
                }
            }
            for (scale in listOf(1,2)) {
                shell("settings put system font_scale $scale"); delay(600)
                for (language in listOf("en","ru","az","ar")) for (dark in listOf(false,true)) {
                    val prefs = AppPreferences(ManualSetup("Baku · Bakı",GeoPoint(40.4093,49.8671),ZoneId.of("Asia/Baku"),Profile.MWL),
                        appearance = AppearancePreferences(theme = if (dark) ThemePreference.DARK else ThemePreference.LIGHT),language = language)
                    context.appContainer.settings.update { prefs }; DeliveryLocale.save(context,prefs)
                    if (Build.VERSION.SDK_INT >= 33) {
                        instrumentation.runOnMainSync { context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language) }
                        withTimeout(5000) { while (context.resources.configuration.locales[0].language != language) delay(50) }
                    }
                    val currentActivity = instrumentation.startActivitySync(Intent(context,CaptureHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as CaptureHostActivity
                    activity = currentActivity
                    val density = currentActivity.resources.displayMetrics.density
                    fun dp(value: Int) = (value*density).toInt()
                    val views = mutableListOf<AppWidgetHostView>()
                    instrumentation.runOnMainSync {
                        val root = LinearLayout(currentActivity).apply {
                            orientation = LinearLayout.VERTICAL; setPadding(dp(12),dp(36),dp(12),dp(48))
                            setBackgroundColor(if (dark) 0xff101d26.toInt() else 0xfff8fafc.toInt())
                        }
                        currentActivity.setContentView(root)
                        for ((index,id) in ids.withIndex()) {
                            val view = checkNotNull(host).createView(currentActivity,id,checkNotNull(manager.getAppWidgetInfo(id))); views += view
                            root.addView(view,LinearLayout.LayoutParams(dp(if (index == 0) 180 else 280),dp(if (index == 0) 180 else 350)))
                            manager.updateAppWidgetOptions(id,Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280); putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,280)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,if (index == 0) 180 else 350)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,if (index == 0) 180 else 350)
                            })
                        }
                    }
                    WidgetUpdater.update(context)
                    withTimeout(12000) {
                        var ready = false
                        while (!ready) {
                            instrumentation.runOnMainSync { ready = views[0].findViewById<TextView>(R.id.widget_time)?.text?.isNotEmpty() == true && views[1].findViewById<ListView>(R.id.widget_rows)?.count == 6 }
                            if (!ready) delay(100)
                        }
                    }
                    check(kotlin.math.abs(currentActivity.resources.configuration.fontScale-scale) < .05)
                    for (position in listOf("top","bottom")) {
                        instrumentation.runOnMainSync { views[1].findViewById<ListView>(R.id.widget_rows).setSelection(if (position == "top") 0 else 5) }
                        withTimeout(5000) {
                            var scrolled = false
                            while (!scrolled) {
                                instrumentation.runOnMainSync {
                                    val list = views[1].findViewById<ListView>(R.id.widget_rows)
                                    scrolled = if (position == "top") list.firstVisiblePosition == 0 else list.lastVisiblePosition == 5
                                    if (!scrolled) list.setSelection(if (position == "top") 0 else 5)
                                }
                                if (!scrolled) delay(100)
                            }
                        }
                        instrumentation.waitForIdleSync(); delay(200)
                        val deadline = SystemClock.uptimeMillis()+5000
                        while (instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() != context.packageName && SystemClock.uptimeMillis() < deadline) delay(100)
                        check(instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName) { "System dialog covered the widget fixture" }
                        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                        directory.resolve("widgets-$language-${scale}x-${if(dark) "dark" else "light"}-$position.png").outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }; bitmap.recycle()
                    }
                    instrumentation.runOnMainSync { currentActivity.finish() }; activity = null
                }
            }
        } finally {
            instrumentation.runOnMainSync { ids.forEach { host?.deleteAppWidgetId(it) }; host?.stopListening(); host?.deleteHost(); activity?.finish() }
            shell("appwidget revokebind --package ${context.packageName} --user 0")
            context.appContainer.settings.update { original }; DeliveryLocale.save(context,original)
            if (Build.VERSION.SDK_INT >= 33 && oldLocales != null) instrumentation.runOnMainSync { context.getSystemService(LocaleManager::class.java).applicationLocales = oldLocales }
            shell(if (oldScale == "null") "settings delete system font_scale" else "settings put system font_scale $oldScale")
            WidgetUpdater.update(context)
        }
    }
}
