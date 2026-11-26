// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class AlarmSnapshotTest {
    @Test fun duplicateClaimsStaleGenerationsAndDelayedBurstsAreRejectedAfterReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "alarm-snapshot-fixture"
        val store = AlarmSnapshot(context, name)
        val now = Instant.parse("2026-11-13T08:00:00Z")
        val entry = AlarmEntry("2026-11-13/FAJR/AT_TIME", PrayerKey(LocalDate.of(2026, 11, 13), Prayer.FAJR),
            EventKind.AT_TIME, now, "Asia/Baku", "builtin_adhan", ChallengeDifficulty.OFF)
        try {
            store.replace(listOf(entry))
            assertNull(store.claim(entry.id, "obsolete-generation", now))
            assertNull(store.claim(entry.id, entry.token, now.minusSeconds(10)))
            assertNotNull(store.claim(entry.id, entry.token, now.plusSeconds(5)))
            assertNull(AlarmSnapshot(context, name).claim(entry.id, entry.token, now.plusSeconds(6)))
            store.replace(listOf(entry.copy(token = "new-config")))
            assertNull(store.claim(entry.id, entry.token, now.plusSeconds(10)))
            assertNull(store.claim(entry.id, "new-config", now.plusSeconds(301)))
            val extra = entry.copy(kind = EventKind.AFTER, token = "extra")
            store.replace(listOf(extra))
            assertNull(store.claim(extra.id, extra.token, now.plusSeconds(121)))
            val fajr = entry.copy(kind = EventKind.FAJR_WAKE_UP, token = "fajr", difficulty = ChallengeDifficulty.HARD)
            store.replace(listOf(fajr))
            assertEquals(fajr, AlarmSnapshot(context, name).read().single())
            assertNotNull(store.claim(fajr.id, fajr.token, now.plusSeconds(599)))
            val stored = context.createDeviceProtectedStorageContext().getSharedPreferences(name, 0).all.toString()
            assertFalse(stored.contains("latitude")); assertFalse(stored.contains("longitude"))
            assertFalse(stored.contains("ON_TIME")); assertFalse(stored.contains("pause"))
        } finally { store.replace(emptyList()) }
    }
    @Test fun bundledRecordingsHaveActualDecodableAudioAndFajrIsDistinct() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val durations = listOf("adhan_1.mp3", "adhan_2.mp3", "fajr.mp3").map { name ->
            val player = android.media.MediaPlayer()
            try { context.assets.openFd("audio/$name").use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                player.prepare(); assertTrue(player.duration in 1000..600000); player.duration
            } finally { player.release() }
        }
        assertEquals(3, durations.distinct().size)
    }
}
