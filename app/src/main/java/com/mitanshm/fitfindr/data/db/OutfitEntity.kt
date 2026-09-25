package com.mitanshm.fitfindr.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one saved [com.mitanshm.fitfindr.domain.OutfitResult].
 * [paletteCsv] stores the hex palette as a comma-joined string (see
 * [OutfitMapper]) rather than adding a `TypeConverter` + JSON dependency
 * for what is always a short, flat list of hex strings.
 */
@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val styleLabel: String,
    val notes: String?,
    val paletteCsv: String,
    val createdAtEpochMs: Long,
)
