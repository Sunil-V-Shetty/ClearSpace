package com.sunilshetty.clearspace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.local.entity.StorageCategoryCacheEntity

@Dao
interface StorageCategoryCacheDao {

    @Query(
        "SELECT * FROM ${Constants.Database.STORAGE_CATEGORY_CACHE_TABLE} ORDER BY totalBytes DESC",
    )
    suspend fun getAll(): List<StorageCategoryCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StorageCategoryCacheEntity>): List<Long>

    @Query("DELETE FROM ${Constants.Database.STORAGE_CATEGORY_CACHE_TABLE}")
    suspend fun clearAll(): Int

    @Transaction
    suspend fun replaceAll(entities: List<StorageCategoryCacheEntity>) {
        clearAll()
        insertAll(entities)
    }
}
