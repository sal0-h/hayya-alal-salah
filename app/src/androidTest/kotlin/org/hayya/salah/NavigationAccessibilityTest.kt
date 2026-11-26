// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationAccessibilityTest {
    init { ScreenshotActivity.language = "en"; ScreenshotActivity.dark = false; ScreenshotActivity.route = "today"; ScreenshotActivity.recordDialog = false; ScreenshotActivity.showNight = false }
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

    @Test fun todaySwipesOneDateAndShowsEitherNightRowOrBoth() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-pager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("record-status-fajr").assertDoesNotExist()
        compose.onNodeWithTag("today-pager").performTouchInput { swipeRight() }
        compose.onNodeWithTag("today-return").assertDoesNotExist()
        compose.onNodeWithTag("record-status-fajr").assertExists()

        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-appearance").performScrollTo().performClick()
        val midnight = compose.onNodeWithText("Midnight (sunset to Fajr)")
        val lastThird = compose.onNodeWithText("Last third (sunset to Fajr)")
        midnight.performScrollTo().assertIsOff().performClick()
        lastThird.performScrollTo().assertIsOff().performClick()
        midnight.assertIsOn()
        lastThird.assertIsOn()
        compose.onNodeWithTag("screen-back").performScrollTo().performClick()
        compose.onNodeWithTag("nav-today").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("today-night-last-third").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("today-night-midnight").assertExists()
        compose.onNodeWithTag("today-night-last-third").assertExists()
        compose.onNodeWithTag("record-status-sunrise").assertDoesNotExist()
    }

    @Test fun dateArrowsAndSelectedDaySurviveTabNavigation() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-previous").assertHasClickAction().assertContentDescriptionEquals("Previous day")
        compose.onNodeWithTag("today-next").assertHasClickAction().assertContentDescriptionEquals("Next day").performClick()
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("record-status-fajr").assertDoesNotExist()
        compose.onNodeWithTag("nav-history").performClick()
        compose.onNodeWithTag("nav-today").performClick()
        compose.onNodeWithTag("today-return").assertIsDisplayed()
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("record-status-fajr").assertDoesNotExist()
        compose.onNodeWithTag("today-previous").performClick()
        compose.onNodeWithTag("today-return").assertDoesNotExist()
        compose.onNodeWithTag("record-status-fajr").assertExists()
        compose.onNodeWithTag("nav-history").performClick()
        compose.onNodeWithTag("nav-today").performClick()
        compose.onNodeWithTag("prayer-data-ready").assertExists()
    }


    @Test fun gentleDaySwipeAdvancesButASmallNudgeDoesNot() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-pager").performTouchInput {
            swipe(start = Offset(width * .85f, height * .5f), end = Offset(width * .52f, height * .5f), durationMillis = 1200)
        }
        compose.onNodeWithTag("today-return").assertIsDisplayed()
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("today-pager").performTouchInput {
            swipe(start = Offset(width * .8f, height * .5f), end = Offset(width * .7f, height * .5f), durationMillis = 1000)
        }
        compose.onNodeWithText("Saturday, November 14, 2026").assertExists()
        compose.onNodeWithTag("today-return").performClick()
        compose.onNodeWithTag("today-return").assertDoesNotExist()
        compose.onNodeWithTag("record-status-fajr").assertExists()
    }


    @Test fun resettingTheSyntheticAppForgetsTheRetainedFutureDate() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-next").performClick()
        compose.onNodeWithTag("today-return").assertIsDisplayed()
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-privacy").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.full_reset)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.remove)).performClick()
        compose.onNodeWithTag("search-city").performScrollTo().performTextInput("Baku")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("city-baku-az").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("city-baku-az").performScrollTo().performClick()
        compose.onNodeWithTag("save-location").performScrollTo().performClick()
        compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("prayer-data-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("today-return").assertDoesNotExist()
        compose.onNodeWithTag("record-status-fajr").assertExists()
    }

}
