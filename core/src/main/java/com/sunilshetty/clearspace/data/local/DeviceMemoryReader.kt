package com.sunilshetty.clearspace.data.local

import android.app.ActivityManager
import android.content.Context
import com.sunilshetty.clearspace.data.model.DeviceMemorySnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceMemoryReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun readMemory(): DeviceMemorySnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val total = info.totalMem.coerceAtLeast(1L)
        val avail = info.availMem.coerceIn(0L, total)
        val used = (total - avail).coerceAtLeast(0L)
        val usedFraction = (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)

        val processCount = runCatching {
            activityManager.runningAppProcesses
                ?.count { process ->
                    process.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
                }
                ?: 0
        }.getOrDefault(0)

        return DeviceMemorySnapshot(
            totalBytes = total,
            availBytes = avail,
            usedBytes = used,
            usedFraction = usedFraction,
            lowMemory = info.lowMemory,
            backgroundProcessCount = processCount,
        )
    }
}
