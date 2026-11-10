package com.sunilshetty.clearspace.data.model

data class StorageCategoryBreakdown(
    val category: StorageCategory,
    val itemCount: Long,
    val totalBytes: Long,
)

data class StorageScanSnapshot(
    val scannedAtEpochMs: Long,
    val categories: List<StorageCategoryBreakdown>,
)
