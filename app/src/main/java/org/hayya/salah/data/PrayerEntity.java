// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "prayer_records", primaryKeys = {"serviceDay", "prayer"})
public class PrayerEntity {
    public final long serviceDay;
    @NonNull public final String prayer;
    @NonNull public final String status;
    public final long createdAt;
    public final long updatedAt;
    @NonNull public final String originatingZone;
    public PrayerEntity(long serviceDay, @NonNull String prayer, @NonNull String status,
            long createdAt, long updatedAt, @NonNull String originatingZone) {
        this.serviceDay = serviceDay; this.prayer = prayer; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt; this.originatingZone = originatingZone;
    }
}
