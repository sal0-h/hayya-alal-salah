// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AppearanceCaptureTest {
    @Test fun captureTodayInFourLanguagesAtNormalAndLargeText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // AGP collects this directory before uninstalling the app after instrumentation.
        val output = checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")) {
            "Run through Gradle so additional test output is collected"
        }
        val directory = File(output, "screenshots").apply { check(isDirectory || mkdirs()) }
        val originalLocale = Locale.getDefault()
        try {
            for (language in listOf("en", "ru", "az", "ar")) {
                for (scale in listOf(1f, 2f)) for (dark in listOf(false, true)) {
                    ScreenshotActivity.language = language
                    ScreenshotActivity.scale = scale
                    ScreenshotActivity.dark = dark
                    Locale.setDefault(Locale.forLanguageTag(language))
                    val activity = instrumentation.startActivitySync(Intent(context, ScreenshotActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    try {
                        instrumentation.waitForIdleSync()
                        // Wait for the asynchronous local calculation and Compose frame.
                        Thread.sleep(1500)
                        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                        File(directory, "today-$language-${scale.toInt()}x-${if (dark) "dark" else "light"}.png")
                            .outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
                        bitmap.recycle()
                    } finally { instrumentation.runOnMainSync { activity.finish() } }
                }
            }
        } finally { Locale.setDefault(originalLocale) }
    }
}
