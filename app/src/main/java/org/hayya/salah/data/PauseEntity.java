// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.data;
import androidx.room.Entity;
@Entity(tableName = "pause_intervals", primaryKeys = {"startDay", "endDay"})
public class PauseEntity {
    public final long startDay;
    public final long endDay;
    public PauseEntity(long startDay, long endDay) { this.startDay = startDay; this.endDay = endDay; }
}
