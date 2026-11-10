package com.sunilshetty.clearspace.di

import android.content.Context
import androidx.room.Room
import com.sunilshetty.clearspace.data.local.ClearSpaceDatabase
import com.sunilshetty.clearspace.data.local.dao.StorageCategoryCacheDao
import com.sunilshetty.clearspace.data.local.dao.TrashItemDao
import com.sunilshetty.clearspace.core.constants.Constants
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
    fun provideClearSpaceDatabase(
        @ApplicationContext context: Context,
    ): ClearSpaceDatabase = Room.databaseBuilder(
        context,
        ClearSpaceDatabase::class.java,
        Constants.Database.FILE_NAME,
    )
        .addMigrations(ClearSpaceDatabase.MIGRATION_1_2)
        .build()

    @Provides
    fun provideTrashItemDao(
        database: ClearSpaceDatabase,
    ): TrashItemDao = database.trashItemDao()

    @Provides
    fun provideStorageCategoryCacheDao(
        database: ClearSpaceDatabase,
    ): StorageCategoryCacheDao = database.storageCategoryCacheDao()
}
