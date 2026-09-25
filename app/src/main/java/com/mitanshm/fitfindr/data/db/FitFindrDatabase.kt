package com.mitanshm.fitfindr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [OutfitEntity::class, GarmentEntity::class], version = 1, exportSchema = false)
abstract class FitFindrDatabase : RoomDatabase() {
    abstract fun outfitDao(): OutfitDao

    companion object {
        const val DATABASE_NAME = "fitfindr.db"
    }
}
