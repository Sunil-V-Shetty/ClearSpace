package com.sunilshetty.clearspace.data.local

import android.content.Context
import com.sunilshetty.clearspace.data.model.DeviceMemorySnapshot
import com.sunilshetty.clearspace.data.model.QuickScanResult
import com.sunilshetty.clearspace.data.model.StorageScanSnapshot
import com.sunilshetty.clearspace.data.repository.DeviceStorageRepository
import com.sunilshetty.clearspace.data.repository.StorageScanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.delay

class QuickScanOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceMemoryReader: DeviceMemoryReader,
    private val deviceStorageRepository: DeviceStorageRepository,
    private val storageScanRepository: StorageScanRepository,
) {

    suspend fun run(
        mediaAccessGranted: Boolean,
        onStep: suspend (QuickScanStep) -> Unit,
    ): Pair<QuickScanResult, StorageScanSnapshot?> {
        onStep(QuickScanStep.CheckingMemory)
        delay(STEP_DELAY_MS)
        val memoryBefore = deviceMemoryReader.readMemory()

        onStep(QuickScanStep.RefreshingStorage)
        delay(STEP_DELAY_MS)
        deviceStorageRepository.getDeviceStorage()

        var mediaSnapshot: StorageScanSnapshot? = null
        if (mediaAccessGranted) {
            onStep(QuickScanStep.ScanningMedia)
            delay(STEP_DELAY_MS)
            mediaSnapshot = storageScanRepository.scanAndCache()
        }

        onStep(QuickScanStep.ClearingTempCache)
        delay(STEP_DELAY_MS)
        val cacheCleared = clearAppCacheDirs()

        onStep(QuickScanStep.FreeingMemory)
        delay(STEP_DELAY_MS)
        System.runFinalization()
        @Suppress("DEPRECATION")
        System.gc()

        onStep(QuickScanStep.Finishing)
        delay(STEP_DELAY_MS)
        val memoryAfter = deviceMemoryReader.readMemory()

        val result = QuickScanResult(
            memoryBefore = memoryBefore,
            memoryAfter = memoryAfter,
            storageRefreshed = true,
            mediaScanCompleted = mediaSnapshot != null,
            cacheBytesCleared = cacheCleared,
            completedAtEpochMs = System.currentTimeMillis(),
        )
        return result to mediaSnapshot
    }

    private fun clearAppCacheDirs(): Long {
        var cleared = 0L
        listOfNotNull(context.cacheDir, context.externalCacheDir, context.codeCacheDir).forEach { dir ->
            cleared += deleteChildren(dir)
        }
        return cleared
    }

    private fun deleteChildren(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += if (child.isDirectory) {
                deleteChildren(child) + if (child.delete()) 0L else 0L
            } else {
                val size = child.length()
                if (child.delete()) size else 0L
            }
        }
        return total
    }

    enum class QuickScanStep {
        CheckingMemory,
        RefreshingStorage,
        ScanningMedia,
        ClearingTempCache,
        FreeingMemory,
        Finishing,
    }

    companion object {
        private const val STEP_DELAY_MS = 450L
    }
}
