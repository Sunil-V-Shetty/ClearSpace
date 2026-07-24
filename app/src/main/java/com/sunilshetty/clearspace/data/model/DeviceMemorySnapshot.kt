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

    /** Composite score from RAM freed and completed tune-up steps (1–100). */
    val optimizedPercent: Int
        get() {
            val total = memoryBefore.totalBytes.coerceAtLeast(1L)
            val ramUsedDrop = (memoryBefore.usedBytes - memoryAfter.usedBytes).coerceAtLeast(0L)
            var score = ramUsedDrop.toDouble() / total.toDouble() * 100.0
            if (mediaScanCompleted) score += 6.0
            if (cacheBytesCleared > 0L) {
                score += (cacheBytesCleared.toDouble() / (32.0 * 1024.0 * 1024.0)).coerceAtMost(8.0)
            }
            if (storageRefreshed) score += 4.0
            return score.coerceIn(1.0, 100.0).toInt()
        }
}
