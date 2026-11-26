// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.media.*
import android.os.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

/** Real MediaPlayer/AudioManager state; does not claim physically audible/call behavior. */
@RunWith(AndroidJUnit4::class)
class AudioPlaybackIntegrationTest {
    @Test fun realAudioFocusPauseResumeFajrPriorityAndEmergencyStopNeverRecord() = runBlocking {
        check(Build.HARDWARE in setOf("ranchu","goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
        val activity = instrumentation.startActivitySync(Intent(context,CaptureHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val app = context.appContainer
        val original = app.settings.preferences.first()
        val records = app.repository.snapshot().records
        val audio = context.getSystemService(AudioManager::class.java)
        assertEquals(AudioManager.RINGER_MODE_NORMAL,audio.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,context.getSystemService(NotificationManager::class.java).currentInterruptionFilter)
        val zone = ZoneId.of("Asia/Baku")
        val date = LocalDate.now(zone)
        val normal = AlarmEntry("synthetic-normal",PrayerKey(date,Prayer.DHUHR),EventKind.AT_TIME,Instant.now(),zone.id,"builtin_adhan",ChallengeDifficulty.OFF)
        val fajr = AlarmEntry("synthetic-fajr",PrayerKey(date,Prayer.FAJR),EventKind.FAJR_WAKE_UP,Instant.now(),zone.id,"builtin_fajr",ChallengeDifficulty.EASY)
        var focus: AudioFocusRequest? = null
        var phase = "normal playback"
        fun playing() = audio.activePlaybackConfigurations.any { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM }
        suspend fun waitFor(test: () -> Boolean) {
            try { withTimeout(10000) { while (!test()) delay(100) } }
            catch (error: TimeoutCancellationException) { throw AssertionError("Audio policy failed during $phase; active=${AlarmPlayback.active?.id}; playing=${playing()}",error) }
        }
        try {
            app.settings.update { AppPreferences(ManualSetup("Public audio fixture",GeoPoint(40.4093,49.8671),zone,Profile.MWL),
                alerts = Prayer.entries.associateWith { PrayerAlert(AlertMode.ADHAN) },fajr = FajrPreferences(enabled = true)) }
            AlarmCoordinator.reconcile(context)
            AlertNotifications.channels(context)
            AlarmPlayback.start(context,normal)
            waitFor { AlarmPlayback.active?.token == normal.token && playing() }
            phase = "transient focus loss"
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener({},Handler(Looper.getMainLooper())).build()
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,audio.requestAudioFocus(focus))
            waitFor { AlarmPlayback.active?.token == normal.token && !playing() }
            phase = "focus regain"
            audio.abandonAudioFocusRequest(focus); focus = null
            waitFor { AlarmPlayback.active?.token == normal.token && playing() }
            phase = "Fajr priority"
            AlarmPlayback.start(context,fajr)
            waitFor { AlarmPlayback.active?.token == fajr.token && playing() }
            AlarmPlayback.start(context,normal.copy(token = "late-normal-fixture"))
            delay(300)
            assertEquals(fajr.token,AlarmPlayback.active?.token)
            phase = "emergency stop"
            AlarmPlayback.stop(context)
            waitFor { AlarmPlayback.active == null && !playing() }
            assertEquals(records,app.repository.snapshot().records)
        } finally {
            focus?.let(audio::abandonAudioFocusRequest)
            AlarmPlayback.stop(context)
            app.settings.update { original }; AlarmCoordinator.cancelAll(context)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
