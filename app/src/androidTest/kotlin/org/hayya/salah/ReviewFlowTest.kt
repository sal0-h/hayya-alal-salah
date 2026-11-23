// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import android.view.Surface
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ReviewFlowTest {
    @Test(timeout = 90_000) fun onboardingShowsTimesAndInlineStatusPersistsAndClears() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.appContainer
        val original = runBlocking { app.settings.preferences.first() }
        assertTrue("Fixture requires emulator QA", Build.HARDWARE in listOf("ranchu", "goldfish"))
        var tracked: PrayerKey? = null
        try {
            runBlocking { app.settings.update { AppPreferences() } }
            ActivityScenario.launch(MainActivity::class.java).use {
                val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
                automation.serviceInfo = automation.serviceInfo.apply {
                    flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                }
                fun root() = automation.rootInActiveWindow
                fun byTag(tag: String) = findNode(root()) { it.viewIdResourceName == tag }
                fun byText(text: String) = findNode(root()) { it.text?.toString() == text }
                fun click(label: String, locate: () -> AccessibilityNodeInfo?) {
                    awaitValue(label) {
                        // Dialog labels can be exposed as children of the actionable node.
                        val button = generateSequence(locate()) { it.parent }.firstOrNull {
                            it.isVisibleToUser && it.isEnabled && it.isClickable
                        }
                        if (button != null) {
                            Unit.takeIf { button.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
                        } else {
                            findNode(root()) { it.isScrollable }
                                ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            null
                        }
                    }
                }
                fun clickTag(tag: String) = click(tag) { byTag(tag) }
                val search = awaitValue("offline city search") { byTag("search-city") }
                assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Baku")
                }))
                // Wait for the result before scrolling. Search is deliberately debounced.
                awaitValue("Baku search result") { byTag("city-baku-az") }
                clickTag("city-baku-az")
                clickTag("save-location")
                awaitValue("method confirmation") { byTag("finish-setup") }
                assertNull(runBlocking { app.settings.preferences.first().place })
                clickTag("setup-back")
                awaitValue("retained location") { byText("Baku") }
                clickTag("save-location")
                clickTag("finish-setup")
                awaitValue("first-run times") { byTag("prayer-data-ready") }
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
                    awaitValue("fixture location") { byText(place.name) }
                }
                tracked = key
                val tag = "record-status-${key.prayer.name.lowercase(Locale.ROOT)}"
                val statuses = listOf(PrayerStatus.ON_TIME, PrayerStatus.MADE_UP, PrayerStatus.MISSED, PrayerStatus.EXCUSED, null)
                for (status in statuses) {
                    val label = context.getString(when (status) {
                        PrayerStatus.ON_TIME -> R.string.status_on_time
                        PrayerStatus.MADE_UP -> R.string.status_made_up
                        PrayerStatus.MISSED -> R.string.status_missed
                        PrayerStatus.EXCUSED -> R.string.status_excused
                        null -> R.string.status_unrecorded
                    })
                    val action = context.getString(R.string.set_prayer_status, label)
                    click(tag) {
                        byTag(tag)?.takeIf { node ->
                            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK && it.label?.toString() == action }
                        }
                    }
                    awaitValue("saved $label") {
                        val stored = runBlocking { app.repository.snapshot().records[key]?.status }
                        val announced = byTag(tag)?.let { AccessibilityNodeInfoCompat.wrap(it).stateDescription?.toString() }
                        Unit.takeIf { stored == status && announced == label }
                    }
                }
                clickTag("status-legend")
                awaitValue("status legend") { byText(context.getString(R.string.status_legend_instructions)) }
                assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
                awaitValue("Today after dismissing the legend") { byTag("screen-today") }
                assertNull(byTag("record-status-sunrise"))
                clickTag("nav-settings")
                clickTag("settings-focus")
                clickTag("focus-choose-apps")
                clickTag("focus-select-all")
                val packageName = InstrumentationRegistry.getInstrumentation().context.packageName
                assertTrue(runBlocking { app.settings.preferences.first().focus.packages.isEmpty() })
                assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
                awaitValue("Focus screen") { byTag("focus-choose-apps") }
                assertTrue(runBlocking { app.settings.preferences.first().focus.packages.isEmpty() })
                clickTag("focus-choose-apps")
                clickTag("focus-select-all")
                clickTag("focus-apply-apps")
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

private fun findNode(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    if (root == null || predicate(root)) return root
    for (index in 0 until root.childCount) {
        findNode(root.getChild(index), predicate)?.let { return it }
    }
    return null
}

private fun <T : Any> awaitValue(label: String, read: () -> T?): T {
    val deadline = SystemClock.uptimeMillis() + 10_000
    do {
        read()?.let { return it }
        Thread.sleep(120)
    } while (SystemClock.uptimeMillis() < deadline)
    throw AssertionError("Timed out waiting for $label")
}
