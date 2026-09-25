package com.mitanshm.fitfindr.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Insert
    suspend fun insertOutfit(outfit: OutfitEntity): Long

    @Insert
    suspend fun insertGarments(garments: List<GarmentEntity>)

    @Transaction
    @Query("SELECT * FROM outfits ORDER BY createdAtEpochMs DESC")
    fun observeAllWithGarments(): Flow<List<OutfitWithGarments>>

    @Transaction
    @Query("SELECT * FROM outfits WHERE id = :outfitId LIMIT 1")
    suspend fun getWithGarments(outfitId: Long): OutfitWithGarments?

    // Deleting the outfit row cascades to its garments via the ForeignKey
    // in GarmentEntity (onDelete = CASCADE), so no separate garment delete
    // query is needed here.
    @Query("DELETE FROM outfits WHERE id = :outfitId")
    suspend fun deleteOutfit(outfitId: Long)
}
