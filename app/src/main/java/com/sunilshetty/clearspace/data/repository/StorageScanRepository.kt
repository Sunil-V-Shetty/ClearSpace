package com.sunilshetty.clearspace.data.repository

import com.sunilshetty.clearspace.data.local.MediaStorageScanner
import com.sunilshetty.clearspace.data.local.dao.StorageCategoryCacheDao
import com.sunilshetty.clearspace.data.local.entity.StorageCategoryCacheEntity
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.model.StorageCategoryBreakdown
import com.sunilshetty.clearspace.data.model.StorageScanSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageScanRepository @Inject constructor(
    private val mediaStorageScanner: MediaStorageScanner,
    private val storageCategoryCacheDao: StorageCategoryCacheDao,
) {

    suspend fun getCachedSnapshot(): StorageScanSnapshot? {
        val cached = storageCategoryCacheDao.getAll()
        if (cached.isEmpty()) return null
        return cached.toSnapshot()
    }

    suspend fun scanAndCache(): StorageScanSnapshot {
        val scannedAtEpochMs = System.currentTimeMillis()
        val categories = mediaStorageScanner.scanCategories()
        storageCategoryCacheDao.clearAll()
        storageCategoryCacheDao.insertAll(
            categories.map { breakdown ->
                StorageCategoryCacheEntity(
                    category = breakdown.category.id,
                    itemCount = breakdown.itemCount,
                    totalBytes = breakdown.totalBytes,
                    scannedAtEpochMs = scannedAtEpochMs,
                )
            },
        )
        return StorageScanSnapshot(
            scannedAtEpochMs = scannedAtEpochMs,
            categories = categories.sortedByDescending { it.totalBytes },
        )
    }

    private fun List<StorageCategoryCacheEntity>.toSnapshot(): StorageScanSnapshot {
        val scannedAt = maxOf { it.scannedAtEpochMs }
        val categories = mapNotNull { entity ->
            val category = StorageCategory.fromId(entity.category) ?: return@mapNotNull null
            StorageCategoryBreakdown(
                category = category,
                itemCount = entity.itemCount,
                totalBytes = entity.totalBytes,
            )
        }.sortedByDescending { it.totalBytes }
        return StorageScanSnapshot(
            scannedAtEpochMs = scannedAt,
            categories = categories,
        )
    }
}
