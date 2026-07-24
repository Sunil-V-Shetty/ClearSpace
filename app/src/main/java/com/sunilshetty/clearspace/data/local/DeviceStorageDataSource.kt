package com.sunilshetty.clearspace.data.local

import android.os.Environment
import android.os.StatFs
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import javax.inject.Inject

class DeviceStorageDataSource @Inject constructor() {

    fun readInternalStorage(): DeviceStorageInfo {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        val blockSize = statFs.blockSizeLong
        val totalBytes = statFs.blockCountLong * blockSize
        val freeBytes = statFs.availableBlocksLong * blockSize
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        return DeviceStorageInfo(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
        )
    }
}
