// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class FajrChallengeIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun recreatingRingingActivityPreservesChallengeAndNeverRecords() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.appContainer
        val original = runBlocking { app.settings.preferences.first() }
        val records = runBlocking { app.repository.snapshot().records }
        val zone = ZoneId.of("UTC")
        val place = ManualSetup("Synthetic Fajr challenge", GeoPoint(40.0, 50.0), zone, Profile.MWL)
        val entry = AlarmEntry("challenge-fixture", PrayerKey(LocalDate.now(zone), Prayer.FAJR), EventKind.FAJR_WAKE_UP,
            Instant.now(), zone.id, "builtin_fajr", ChallengeDifficulty.MEDIUM)
        if (Build.VERSION.SDK_INT >= 33) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}"
            )).use { it.readBytes() }
        }
        fun start(alarm: AlarmEntry) {
            AlarmPlayback.start(context, alarm)
            compose.waitUntil(10_000) { AlarmPlayback.active?.token == alarm.token }
        }
        fun question() = compose.onNode(hasText(" = ?", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        fun answer(question: String): String {
            val (left, operator, right) = question.split(" ")
            assertEquals("×", operator)
            return (left.toInt() * right.toInt()).toString()
        }
        fun submit() = compose.onNodeWithText(context.getString(R.string.check_answer)).performScrollTo().performClick()
        fun assertProgress(step: Int) {
            compose.onNodeWithText(context.getString(R.string.question_progress, step.toString(), "3")).assertExists()
        }
        fun assertRecordsUnchanged() = assertEquals(records, runBlocking { app.repository.snapshot().records })
        try {
            runBlocking {
                app.settings.update { AppPreferences(place, language = "en", fajr = FajrPreferences(enabled = true)) }
                AlarmCoordinator.reconcile(context)
            }
            ActivityScenario.launch<CaptureHostActivity>(Intent(context, CaptureHostActivity::class.java)).use {
                start(entry)
                ActivityScenario.launch(RingingActivity::class.java).use { ringing ->
                    assertProgress(1)
                    compose.onNodeWithTag("alarm-answer").performTextInput(answer(question()))
                    submit()
                    assertProgress(2)
                    val second = question()
                    val pending = answer(second)
                    compose.onNodeWithTag("alarm-answer").performTextInput(pending)
                    ringing.recreate()
                    assertProgress(2)
                    assertEquals(second, question())
                    compose.onNodeWithTag("alarm-answer").assertTextContains(pending)
                    submit()
                    assertProgress(3)
                    val third = question()
                    ringing.recreate()
                    assertProgress(3)
                    assertEquals(third, question())
                    compose.onNodeWithTag("alarm-answer").performTextInput(answer(third))
                    submit()
                    compose.waitUntil(10_000) { AlarmPlayback.active == null }
                }
                assertRecordsUnchanged()

                val next = entry.copy(token = "next-challenge-fixture", difficulty = ChallengeDifficulty.HARD)
                start(next)
                ActivityScenario.launch(RingingActivity::class.java).close()
                assertEquals(next.token, AlarmPlayback.active?.token)
                assertRecordsUnchanged()
                ActivityScenario.launch(RingingActivity::class.java).use {
                    compose.onNodeWithText(context.getString(R.string.question_progress, "1", "5")).assertExists()
                    compose.onNodeWithTag("alarm-emergency-stop").performClick()
                    compose.waitUntil(10_000) { AlarmPlayback.active == null }
                }
                assertRecordsUnchanged()
            }
        } finally {
            AlarmPlayback.stop(context)
            runBlocking {
                app.settings.update { original }
                AlarmCoordinator.cancelAll(context)
                AlarmCoordinator.reconcile(context)
            }
        }
    }
}
