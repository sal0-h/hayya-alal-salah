// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class LocalDeletionTest {
    @Test fun fullResetRemovesRecordsPausesImportedAudioAndUpcomingSnapshot() = runBlocking {
        assumeTrue("Deletion fixture requires the isolated CI emulator",Build.HARDWARE in setOf("ranchu","goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "org.hayya.salah.dev")
        val app = context.appContainer
        val before = app.repository.snapshot()
        check(before.records.isEmpty() && before.pauses.isEmpty() && before.audio.isEmpty())
        check(app.settings.preferences.first().place == null)
        val zone = ZoneId.of("Asia/Baku")
        val date = LocalDate.of(1999,1,2)
        val key = PrayerKey(date,Prayer.ISHA)
        app.settings.update { AppPreferences(ManualSetup("Public deletion fixture",GeoPoint(40.4093,49.8671),zone,Profile.MWL),language = "az") }
        app.repository.record(key,PrayerStatus.EXCUSED,zone)
        app.repository.addPause(PauseInterval(date,date.plusDays(1)))
        val audio = AudioImporter.import(context,"content://org.hayya.salah.test.audio/valid.wav".toUri(),app.repository)
        val file = context.filesDir.resolve("audio").resolve(audio.fileName)
        assertTrue(file.isFile)
        DeliveryLocale.save(context,app.settings.preferences.first())
        context.getSharedPreferences("locale-bootstrap",0).edit(commit = true) { putString("language","az") }
        AlarmSnapshot(context).replace(listOf(AlarmEntry("synthetic-delete",key,EventKind.AT_TIME,Instant.now().plusSeconds(3600),zone.id,null,ChallengeDifficulty.OFF)))
        app.resetLocalData()
        val after = app.repository.snapshot()
        assertTrue(after.records.isEmpty()); assertTrue(after.pauses.isEmpty()); assertTrue(after.audio.isEmpty())
        assertFalse(file.exists())
        assertEquals(AppPreferences(),app.settings.preferences.first())
        assertTrue(AlarmSnapshot(context).read().isEmpty())
        assertNull(AlarmPlayback.active)
        val delivery = context.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale",0)
        assertTrue(delivery.getString("language",null) in setOf(null,"system"))
        assertFalse(delivery.all.values.contains("az"))
        assertTrue(context.getSharedPreferences("locale-bootstrap",0).all.isEmpty())
    }
}
