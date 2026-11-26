// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
        compose.onNodeWithTag("palette-ink").performScrollTo().assertIsNotSelected().performClick()
        compose.onNodeWithTag("palette-ink").assertIsSelected()
        compose.onNodeWithTag("palette-teal").assertIsNotSelected()
        compose.onNodeWithText("Reduce motion").performScrollTo().performClick()
        compose.onNodeWithText("Reduce motion").assertIsOn()
    }

    @Test fun todayCalendarShowsFutureScheduleWithNoRecordingAndReturnsToLiveDay() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-date").performClick()
        val inCalendar = hasAnyAncestor(hasTestTag("prayer-date-picker"))
        val day = compose.onAllNodes(inCalendar and hasClickAction() and hasText("November 14, 2026", substring = true))
        if (day.fetchSemanticsNodes().isNotEmpty()) {
            day[0].performClick()
        } else {
            compose.onNode(inCalendar and hasSetTextAction()).performTextReplacement("11142026")
        }
        compose.onNodeWithTag("apply-prayer-date").performClick()
        compose.onNodeWithTag("today-return").assertIsDisplayed()
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("prayer-data-ready").assertDoesNotExist()
        compose.onNodeWithTag("record-status-fajr").assertDoesNotExist()
        compose.onNodeWithTag("today-return").performClick()
        compose.onNodeWithTag("record-status-fajr").assertExists()
    }
}
