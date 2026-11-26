// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationAccessibilityTest {
    init { ScreenshotActivity.language = "en"; ScreenshotActivity.dark = false; ScreenshotActivity.route = "today"; ScreenshotActivity.recordDialog = false }
    @get:Rule val compose = createAndroidComposeRule<ScreenshotActivity>()
    @Test fun navigationExplicitRecordCorrectionAndLabeledControls() {
        if (android.os.Build.VERSION.SDK_INT >= 34) compose.enableAccessibilityChecks()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("record-open-fajr").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("record-open-fajr").performScrollTo().performClick()
        compose.onNodeWithTag("record-on_time").performScrollTo().performClick()
        compose.onNodeWithTag("record-open-fajr").assertContentDescriptionContains("On time", substring = true)
        compose.onNodeWithTag("record-open-fajr").performClick()
        compose.onNodeWithTag("record-made_up").performScrollTo().performClick()
        compose.onNodeWithTag("record-open-fajr").assertContentDescriptionContains("Made up", substring = true)
        compose.onNodeWithTag("nav-history").performClick()
        compose.onNodeWithTag("screen-history").assertIsDisplayed()
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-appearance").performScrollTo().performClick()
        compose.onNodeWithTag("screen-appearance").assertIsDisplayed()
        compose.onNodeWithText("Reduce motion").performScrollTo().performClick()
        compose.onNodeWithText("Reduce motion").assertIsOn()
    }
}
