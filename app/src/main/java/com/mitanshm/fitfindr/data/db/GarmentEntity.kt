package com.mitanshm.fitfindr.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for one garment belonging to an [OutfitEntity]. `searchQueriesCsv`
 * joins the garment's search-query strings with a separator unlikely to
 * appear inside one (see [OutfitMapper.QUERY_SEPARATOR]) -- garment search
 * queries are short phrases like "olive green cotton twill jacket", so this
 * is simpler than a second child table for what the PRD scopes as a flat,
 * small list per garment.
 */
@Entity(
    tableName = "garments",
    foreignKeys = [
        ForeignKey(
            entity = OutfitEntity::class,
            parentColumns = ["id"],
            childColumns = ["outfitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("outfitId")],
)
data class GarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val outfitId: Long,
    val type: String,
    val color: String,
    val material: String,
    val fit: String,
    val searchQueriesCsv: String,
)
