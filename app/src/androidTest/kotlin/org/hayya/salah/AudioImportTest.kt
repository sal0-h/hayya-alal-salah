// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.hayya.salah.alerts.AudioImporter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioImportTest {
    @Test fun actualPcmImportRejectsMalformedInputAndLeavesNoOrphan() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = context.appContainer.repository
        val original = context.appContainer.settings.preferences.first()
        val before = repo.snapshot().audio.map { it.id }.toSet()
        val asset = AudioImporter.import(context,"content://org.hayya.salah.test.audio/valid.wav".toUri(),repo)
        try {
            assertEquals(1000L,asset.durationMillis)
            assertTrue(context.filesDir.resolve("audio").resolve(asset.fileName).isFile)
            val files = context.filesDir.resolve("audio").list()?.toSet().orEmpty()
            assertTrue(runCatching { AudioImporter.import(context,"content://org.hayya.salah.test.audio/invalid.bin".toUri(),repo) }.isFailure)
            assertEquals(before + asset.id,repo.snapshot().audio.map { it.id }.toSet())
            assertEquals(files,context.filesDir.resolve("audio").list()?.toSet().orEmpty())
            context.appContainer.settings.update { it.copy(fajr = it.fajr.copy(sound = asset.id)) }
            context.appContainer.removeImportedAudio(asset)
            assertFalse(context.filesDir.resolve("audio").resolve(asset.fileName).exists())
            assertEquals(before,repo.snapshot().audio.map { it.id }.toSet())
            assertEquals("builtin_fajr",context.appContainer.settings.preferences.first().fajr.sound)
        } finally { context.appContainer.settings.update { original }; repo.deleteAudio(asset.id); context.filesDir.resolve("audio").resolve(asset.fileName).delete() }
    }
}
