package com.sunilshetty.clearspace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sunilshetty.clearspace.core.constants.Constants

@Entity(tableName = Constants.Database.STORAGE_CATEGORY_CACHE_TABLE)
data class StorageCategoryCacheEntity(
    @PrimaryKey val category: String,
    val itemCount: Long,
    val totalBytes: Long,
    val scannedAtEpochMs: Long,
)
