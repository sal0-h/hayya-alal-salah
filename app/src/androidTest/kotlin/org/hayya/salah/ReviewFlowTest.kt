// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.os.Build
import android.view.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ReviewFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun onboardingShowsTimesAndInlineStatusPersistsAndClears() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.appContainer
        val original = runBlocking { app.settings.preferences.first() }
        assertTrue("Fixture requires emulator QA", Build.HARDWARE in listOf("ranchu", "goldfish"))
        var tracked: PrayerKey? = null
        try {
            runBlocking { app.settings.update { AppPreferences() } }
            ActivityScenario.launch(MainActivity::class.java).use {
                fun await(tag: String) = compose.waitUntil(20_000) {
                    compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
                }
                await("search-city")
                compose.onNodeWithTag("search-city").performTextInput("Baku")
                await("city-baku-az")
                compose.onNodeWithTag("city-baku-az").performScrollTo().performClick()
                compose.onNodeWithTag("save-location").performScrollTo().performClick()
                await("finish-setup")
                assertNull(runBlocking { app.settings.preferences.first().place })
                compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
                await("prayer-data-ready")
                assertEquals("Asia/Baku", runBlocking { app.settings.preferences.first().place?.zone?.id })

                val now = Instant.now()
                val currentPlace = requireNotNull(runBlocking { app.settings.preferences.first().place })
                val fixtures = listOf(currentPlace,
                    ManualSetup("Tokyo", GeoPoint(35.68, 139.69), ZoneId.of("Asia/Tokyo"), Profile.MWL),
                    ManualSetup("New York", GeoPoint(40.71, -74.0), ZoneId.of("America/New_York"), Profile.MWL))
                val (place, key) = fixtures.firstNotNullOf { place ->
                    val date = now.atZone(place.zone).toLocalDate()
                    val schedule = LocalPrayerCalculator().calculate(date, place.point, place.zone, place.calculation)
                        as? CalculationResult.Available
                    schedule?.schedule?.prayers?.entries?.firstOrNull { it.value <= now }?.let { place to PrayerKey(date, it.key) }
                }
                if (place != currentPlace) {
                    runBlocking { app.settings.save(place) }
                    compose.waitUntil(10_000) { compose.onAllNodesWithText(place.name).fetchSemanticsNodes().isNotEmpty() }
                }
                tracked = key
                val tag = "record-status-${key.prayer.name.lowercase(Locale.ROOT)}"
                await(tag)
                val button = compose.onNodeWithTag(tag).performScrollTo()
                val states = listOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.MISSED, PrayerStatus.EXCUSED, null)
                for (status in states) {
                    val label = context.getString(when (status) {
                        PrayerStatus.ON_TIME -> R.string.status_on_time
                        PrayerStatus.MADE_UP -> R.string.status_made_up
                        PrayerStatus.MISSED -> R.string.status_missed
                        PrayerStatus.EXCUSED -> R.string.status_excused
                        null -> R.string.status_unrecorded
                    })
                    button.performClick()
                    compose.waitUntil(10_000) {
                        button.fetchSemanticsNode().config.getOrElse(SemanticsProperties.StateDescription) { "" } == label
                    }
                    assertEquals(status, runBlocking { app.repository.snapshot().records[key]?.status })
                }
                compose.onNodeWithTag("status-legend").performScrollTo().performClick()
                compose.onNodeWithText(context.getString(R.string.status_legend_instructions)).assertExists()
                compose.onNodeWithTag("record-status-sunrise").assertDoesNotExist()
                compose.onNodeWithText(context.getString(R.string.close)).performClick()
                compose.onNodeWithTag("nav-settings").performClick()
                compose.onNodeWithTag("settings-focus").performScrollTo().performClick()
                await("focus-choose-apps")
                compose.waitUntil(10_000) {
                    runCatching { compose.onNodeWithTag("focus-choose-apps").assertIsEnabled() }.isSuccess
                }
                compose.onNodeWithTag("focus-choose-apps").performScrollTo().performClick()
                compose.onNodeWithTag("focus-select-all").performClick()
                val packageName = InstrumentationRegistry.getInstrumentation().context.packageName
                assertTrue(runBlocking { app.settings.preferences.first().focus.packages.isEmpty() })
                compose.onNodeWithText(context.getString(R.string.cancel)).performClick()
                assertTrue(runBlocking { app.settings.preferences.first().focus.packages.isEmpty() })
                compose.onNodeWithTag("focus-choose-apps").performClick()
                compose.onNodeWithTag("focus-select-all").performClick()
                compose.onNodeWithTag("focus-apply-apps").performClick()
                runBlocking {
                    kotlinx.coroutines.withTimeout(10_000) {
                        app.settings.preferences.first { packageName in it.focus.packages }
                    }.let { prefs ->
                        assertFalse(prefs.focus.enabled)
                        assertTrue(prefs.focus.packages.intersect(org.hayya.salah.focus.EssentialApps.excluded(context)).isEmpty())
                    }
                }
            }
        } finally {
            runBlocking {
                tracked?.let { app.repository.record(it, null, ZoneId.of("UTC")) }
                app.settings.update { original }
            }
        }
    }

    @Test fun displayRotationDeclinationAndTiltUseCoherentFlatHeading() {
        val north = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val compass = HeadingWorkspace()
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
        rotations.forEachIndexed { index, rotation ->
            assertEquals(index * 90f + 12f, requireNotNull(compass.trueHeading(north, rotation, 12f)), 0.01f)
        }
        assertEquals(353f, requireNotNull(compass.trueHeading(north, Surface.ROTATION_0, -7f)), 0.01f)
        val vertical = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        assertNull(compass.trueHeading(vertical, Surface.ROTATION_0, 0f))
    }
}
