package com.sunilshetty.clearspace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sunilshetty.clearspace.core.constants.Constants

@Entity(tableName = Constants.Database.TRASH_ITEM_TABLE)
data class TrashItemEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val mediaBucket: String,
    val relativePath: String?,
    val deletedAtEpochMs: Long,
    val storedFileName: String,
)
