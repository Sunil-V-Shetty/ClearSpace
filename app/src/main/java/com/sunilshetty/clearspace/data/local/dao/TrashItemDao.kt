package com.sunilshetty.clearspace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunilshetty.clearspace.data.local.entity.TrashItemEntity

@Dao
interface TrashItemDao {

    @Query(
        """
        SELECT * FROM trash_item
        WHERE deletedAtEpochMs >= :minDeletedAtEpochMs
        ORDER BY deletedAtEpochMs DESC
        """,
    )
    suspend fun getActive(minDeletedAtEpochMs: Long): List<TrashItemEntity>

    @Query("SELECT * FROM trash_item WHERE id IN (:ids)")
    suspend fun getByIds(ids: Set<String>): List<TrashItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: TrashItemEntity)

    @Query("DELETE FROM trash_item WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)

    @Query("SELECT * FROM trash_item WHERE deletedAtEpochMs < :cutoffEpochMs")
    suspend fun listOlderThan(cutoffEpochMs: Long): List<TrashItemEntity>

    @Query("DELETE FROM trash_item WHERE deletedAtEpochMs < :cutoffEpochMs")
    suspend fun deleteOlderThan(cutoffEpochMs: Long): Int
}
