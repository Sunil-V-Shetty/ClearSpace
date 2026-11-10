package com.sunilshetty.clearspace.data.model

data class DeviceMemorySnapshot(
    val totalBytes: Long,
    val availBytes: Long,
    val usedBytes: Long,
    val usedFraction: Float,
    val lowMemory: Boolean,
    val backgroundProcessCount: Int,
)

data class QuickScanResult(
    val memoryBefore: DeviceMemorySnapshot,
    val memoryAfter: DeviceMemorySnapshot,
    val storageRefreshed: Boolean,
    val mediaScanCompleted: Boolean,
    val cacheBytesCleared: Long,
    val completedAtEpochMs: Long,
) {
    val memoryFreedBytes: Long
        get() = (memoryAfter.availBytes - memoryBefore.availBytes).coerceAtLeast(0L)

}
