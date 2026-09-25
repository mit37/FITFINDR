package com.mitanshm.fitfindr.di

import android.content.Context
import androidx.room.Room
import com.mitanshm.fitfindr.data.db.FitFindrDatabase
import com.mitanshm.fitfindr.data.db.OutfitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FitFindrDatabase =
        Room.databaseBuilder(context, FitFindrDatabase::class.java, FitFindrDatabase.DATABASE_NAME).build()

    @Provides
    fun provideOutfitDao(database: FitFindrDatabase): OutfitDao = database.outfitDao()
}
