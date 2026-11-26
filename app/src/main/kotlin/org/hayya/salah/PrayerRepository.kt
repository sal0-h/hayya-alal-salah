// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.room.InvalidationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.hayya.core.*
import org.hayya.salah.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Callable

data class HistorySnapshot(val records: Map<PrayerKey, PrayerRecord>, val pauses: List<PauseInterval>, val audio: List<AudioEntity>)
class PrayerRepository(private val database: PrayerDatabase) {
    private val dao get() = database.prayers()
    suspend fun snapshot(): HistorySnapshot = withContext(Dispatchers.IO) {
        database.runInTransaction(Callable {
            val records = dao.records().map { entity -> PrayerRecord(
                PrayerKey(LocalDate.ofEpochDay(entity.serviceDay), Prayer.valueOf(entity.prayer)),
                PrayerStatus.valueOf(entity.status), Instant.ofEpochMilli(entity.createdAt),
                Instant.ofEpochMilli(entity.updatedAt), ZoneId.of(entity.originatingZone)) }.associateBy { it.key }
            HistorySnapshot(records, dao.pauses().map { PauseInterval(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }, dao.audio())
        })
    }
    val changes: Flow<HistorySnapshot> = callbackFlow {
        val observer = object : InvalidationTracker.Observer("prayer_records", "pause_intervals", "audio_assets") {
            override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
        }
        database.invalidationTracker.addObserver(observer)
        trySend(Unit)
        awaitClose { database.invalidationTracker.removeObserver(observer) }
    }.conflate().map { snapshot() }
    suspend fun record(key: PrayerKey, status: PrayerStatus?, zone: ZoneId, now: Instant = Instant.now()) = withContext(Dispatchers.IO) {
        if (status == null) dao.deleteRecord(key.date.toEpochDay(), key.prayer.name)
        else dao.setRecord(key.date.toEpochDay(), key.prayer.name, status.name, now.toEpochMilli(), zone.id)
    }
    suspend fun addPause(pause: PauseInterval) = withContext(Dispatchers.IO) { dao.addPause(PauseEntity(pause.start.toEpochDay(), pause.endInclusive.toEpochDay())) }
    suspend fun deletePause(pause: PauseInterval) = withContext(Dispatchers.IO) { dao.deletePause(pause.start.toEpochDay(), pause.endInclusive.toEpochDay()) }
    suspend fun putAudio(audio: AudioEntity) = withContext(Dispatchers.IO) { dao.putAudio(audio) }
    suspend fun deleteAudio(id: String) = withContext(Dispatchers.IO) { dao.deleteAudio(id) }
    suspend fun deleteRecords() = withContext(Dispatchers.IO) { dao.deleteAllRecords() }
    suspend fun reset() = withContext(Dispatchers.IO) { dao.reset() }
}
