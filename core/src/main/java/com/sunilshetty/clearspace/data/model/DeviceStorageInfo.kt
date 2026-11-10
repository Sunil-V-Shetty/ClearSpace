package com.sunilshetty.clearspace.data.model

data class DeviceStorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
) {
    val usedPercent: Float
        get() = if (totalBytes <= 0L) 0f else usedBytes.toFloat() / totalBytes.toFloat()
}
