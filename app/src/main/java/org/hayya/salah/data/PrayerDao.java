// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.data;

import androidx.room.*;
import java.util.List;

@Dao
public abstract class PrayerDao {
    @Query("SELECT * FROM prayer_records ORDER BY serviceDay, prayer")
    public abstract List<PrayerEntity> records();
    @Query("SELECT * FROM prayer_records WHERE serviceDay=:day AND prayer=:prayer")
    public abstract PrayerEntity record(long day, String prayer);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertRecord(PrayerEntity entity);
    @Query("DELETE FROM prayer_records WHERE serviceDay=:day AND prayer=:prayer")
    public abstract void deleteRecord(long day, String prayer);
    @Transaction
    public void setRecord(long day, String prayer, String status, long now, String zone) {
        PrayerEntity old = record(day, prayer);
        insertRecord(new PrayerEntity(day, prayer, status, old == null ? now : old.createdAt,
            old == null ? now : Math.max(now, old.createdAt), old == null ? zone : old.originatingZone));
    }
    @Query("SELECT * FROM pause_intervals ORDER BY startDay")
    public abstract List<PauseEntity> pauses();
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void addPause(PauseEntity interval);
    @Query("DELETE FROM pause_intervals WHERE startDay=:start AND endDay=:end")
    public abstract void deletePause(long start, long end);
    @Query("SELECT * FROM audio_assets ORDER BY name")
    public abstract List<AudioEntity> audio();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void putAudio(AudioEntity audio);
    @Query("DELETE FROM audio_assets WHERE id=:id")
    public abstract void deleteAudio(String id);
    @Query("DELETE FROM prayer_records") public abstract void deleteAllRecords();
    @Query("DELETE FROM pause_intervals") public abstract void deleteAllPauses();
    @Query("DELETE FROM audio_assets") public abstract void deleteAllAudio();
    @Transaction public void reset() { deleteAllRecords(); deleteAllPauses(); deleteAllAudio(); }
}
