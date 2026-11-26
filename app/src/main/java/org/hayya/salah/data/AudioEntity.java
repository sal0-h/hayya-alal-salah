// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.data;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "audio_assets")
public class AudioEntity {
    @PrimaryKey @NonNull public final String id;
    @NonNull public final String name;
    @NonNull public final String fileName;
    public final long durationMillis;
    @NonNull public final String provenance;
    public AudioEntity(@NonNull String id, @NonNull String name, @NonNull String fileName,
            long durationMillis, @NonNull String provenance) {
        this.id = id; this.name = name; this.fileName = fileName;
        this.durationMillis = durationMillis; this.provenance = provenance;
    }
}
