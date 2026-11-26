// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AppearanceCaptureTest {
    @Test fun captureFullShellInFourLanguagesWithActualSystemScaleAndWidths() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val output = checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))
        val directory = File(output,"screenshots").apply { check(isDirectory || mkdirs()) }
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val oldScale = shell("settings get system font_scale")
        val oldSize = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val oldLocale = Locale.getDefault()
        val host = instrumentation.startActivitySync(Intent(context,CaptureHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        instrumentation.waitForIdleSync()
        shell("am force-stop com.android.launcher3")
        try {
            for (width in listOf("phone","tablet")) {
                shell(if (width == "phone") "wm size 840x1960" else "wm size 1800x2400")
                val routes = if (width == "phone") listOf("today","history","settings","onboarding","location","calculation","alerts","fajr","focus","qibla","pause","appearance","privacy","credits","record","date","focus-consent","privacy-confirm","polar","focus-overlay","ringing","ringing-keyboard")
                    else listOf("today","history","settings")
                for (scale in listOf(1,2)) {
                    shell("settings put system font_scale $scale"); Thread.sleep(600)
                    for (language in listOf("en","ru","az","ar")) for (dark in listOf(false,true)) for (route in routes) {
                        ScreenshotActivity.language = language; ScreenshotActivity.dark = dark
                        ScreenshotActivity.route = when (route) { "record" -> "today"; "date" -> "history"; "focus-consent" -> "focus"; "privacy-confirm" -> "privacy"; else -> route }
                        ScreenshotActivity.recordDialog = route == "record"
                        Locale.setDefault(Locale.forLanguageTag(language))
                        val activity = instrumentation.startActivitySync(Intent(context,ScreenshotActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ScreenshotActivity
                        try {
                            val deadline = android.os.SystemClock.uptimeMillis() + 10000
                            while (!activity.fullyDrawnReporter.isFullyDrawnReported && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
                            check(activity.fullyDrawnReporter.isFullyDrawnReported) { "Fixture did not finish drawing: $route" }
                            instrumentation.waitForIdleSync(); Thread.sleep(200)
                            check(kotlin.math.abs(activity.resources.configuration.fontScale - scale) < 0.05) { "Actual system font scale did not update" }
                            val trigger = when (route) { "date" -> "history-date"; "focus-consent" -> "focus-permission"; "privacy-confirm" -> "privacy-delete"; "ringing-keyboard" -> "alarm-answer"; else -> null }
                            if (trigger != null) {
                                fun find(node: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
                                    if (node == null || node.viewIdResourceName == trigger) return node
                                    for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
                                    return null
                                }
                                var node = find(instrumentation.uiAutomation.rootInActiveWindow)
                                var attempts = 0
                                while (node?.isVisibleToUser != true && attempts++ < 12) {
                                    fun scrollable(n: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
                                        if (n == null || n.isScrollable) return n
                                        for (i in 0 until n.childCount) scrollable(n.getChild(i))?.let { return it }
                                        return null
                                    }
                                    checkNotNull(scrollable(instrumentation.uiAutomation.rootInActiveWindow)).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                                    instrumentation.waitForIdleSync(); Thread.sleep(150)
                                    node = find(instrumentation.uiAutomation.rootInActiveWindow)
                                }
                                checkNotNull(node).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                                instrumentation.waitForIdleSync(); Thread.sleep(200)
                            }
                            for (position in if (width == "phone" && route !in listOf("record","date","focus-consent","privacy-confirm","focus-overlay","ringing","ringing-keyboard")) listOf("top","bottom") else listOf("top")) {
                                if (position == "bottom") { instrumentation.runOnMainSync { activity.bottom() }; instrumentation.waitForIdleSync(); Thread.sleep(100) }
                                var active = instrumentation.uiAutomation.windows.firstOrNull { it.isActive }
                                val windowDeadline = android.os.SystemClock.uptimeMillis() + 5000
                                while (active?.root?.packageName == null && android.os.SystemClock.uptimeMillis() < windowDeadline) {
                                    Thread.sleep(100); active = instrumentation.uiAutomation.windows.firstOrNull { it.isActive }
                                }
                                val owner = active?.root?.packageName?.toString()
                                check(owner == context.packageName || active?.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD) { "Unexpected capture window for $route/$position: $owner" }
                                val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                                File(directory,"$route-$language-${scale}x-${if(dark) "dark" else "light"}-$width-$position.png").outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
                                bitmap.recycle()
                            }
                        } finally { instrumentation.runOnMainSync { activity.finish() }; instrumentation.waitForIdleSync() }
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync { host.finish() }
            Locale.setDefault(oldLocale)
            shell(if (oldScale == "null") "settings delete system font_scale" else "settings put system font_scale $oldScale")
            shell(if (oldSize == null) "wm size reset" else "wm size $oldSize")
            ScreenshotActivity.route = "today"; ScreenshotActivity.recordDialog = false; ScreenshotActivity.language = "en"; ScreenshotActivity.dark = false
        }
    }
}
