package com.mitanshm.fitfindr.data.db

import androidx.room.Embedded
import androidx.room.Relation

/** Room relation: one outfit row plus all of its garment rows. */
data class OutfitWithGarments(
    @Embedded val outfit: OutfitEntity,
    @Relation(parentColumn = "id", entityColumn = "outfitId")
    val garments: List<GarmentEntity>,
)
