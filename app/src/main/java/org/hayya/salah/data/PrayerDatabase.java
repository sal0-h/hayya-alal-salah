// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.data;
import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {PrayerEntity.class, PauseEntity.class, AudioEntity.class}, version = 1, exportSchema = true)
public abstract class PrayerDatabase extends RoomDatabase { public abstract PrayerDao prayers(); }
