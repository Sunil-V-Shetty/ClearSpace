package com.sunilshetty.clearspace.ui.home

import com.sunilshetty.clearspace.data.model.DeviceMemorySnapshot
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import com.sunilshetty.clearspace.data.model.QuickScanResult
import com.sunilshetty.clearspace.data.model.StorageScanSnapshot
import com.sunilshetty.clearspace.data.local.QuickScanOptimizer

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val storage: DeviceStorageInfo,
        val mediaAccessGranted: Boolean,
        val scanSnapshot: StorageScanSnapshot?,
        val isScanning: Boolean,
        val scanError: String?,
        val memorySnapshot: DeviceMemorySnapshot?,
        val quickScanStep: QuickScanOptimizer.QuickScanStep?,
        val lastQuickScanResult: QuickScanResult?,
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
