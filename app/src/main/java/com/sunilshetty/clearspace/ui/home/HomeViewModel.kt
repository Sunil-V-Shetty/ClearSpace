package com.sunilshetty.clearspace.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.local.DeviceMemoryReader
import com.sunilshetty.clearspace.data.local.QuickScanOptimizer
import com.sunilshetty.clearspace.data.model.QuickScanResult
import com.sunilshetty.clearspace.data.repository.DeviceStorageRepository
import com.sunilshetty.clearspace.data.repository.StorageScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceStorageRepository: DeviceStorageRepository,
    private val storageScanRepository: StorageScanRepository,
    private val deviceMemoryReader: DeviceMemoryReader,
    private val quickScanOptimizer: QuickScanOptimizer,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _quickScanCelebration = MutableStateFlow<QuickScanResult?>(null)
    val quickScanCelebration: StateFlow<QuickScanResult?> = _quickScanCelebration.asStateFlow()

    private var mediaAccessGranted: Boolean = false

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                withContext(Dispatchers.IO) {
                    val storage = deviceStorageRepository.getDeviceStorage()
                    val scanSnapshot = storageScanRepository.getCachedSnapshot()
                    val memory = deviceMemoryReader.readMemory()
                    Triple(storage, scanSnapshot, memory)
                }
            }.onSuccess { (storage, scanSnapshot, memory) ->
                val previous = _uiState.value as? HomeUiState.Success
                _uiState.value = HomeUiState.Success(
                    storage = storage,
                    mediaAccessGranted = mediaAccessGranted,
                    scanSnapshot = scanSnapshot,
                    isScanning = false,
                    scanError = null,
                    memorySnapshot = memory,
                    quickScanStep = null,
                    lastQuickScanResult = previous?.lastQuickScanResult,
                )
            }.onFailure {
                _uiState.value = HomeUiState.Error(
                    message = context.getString(R.string.error_unable_read_device_storage),
                )
            }
        }
    }

    fun refreshStorage() {
        viewModelScope.launch {
            val current = _uiState.value
            runCatching {
                withContext(Dispatchers.IO) {
                    val storage = deviceStorageRepository.getDeviceStorage()
                    val memory = deviceMemoryReader.readMemory()
                    storage to memory
                }
            }.onSuccess { (storage, memory) ->
                when (current) {
                    is HomeUiState.Success -> {
                        _uiState.value = current.copy(
                            storage = storage,
                            memorySnapshot = memory,
                        )
                    }
                    else -> loadHome()
                }
            }.onFailure {
                _uiState.value = HomeUiState.Error(
                    message = context.getString(R.string.error_unable_read_device_storage),
                )
            }
        }
    }

    /** Runs Quick Scan (optimize phone). */
    fun runQuickScan() {
        val current = _uiState.value as? HomeUiState.Success ?: return
        if (current.isScanning) return

        viewModelScope.launch {
            _quickScanCelebration.value = null
            _uiState.value = current.copy(
                isScanning = true,
                scanError = null,
                quickScanStep = QuickScanOptimizer.QuickScanStep.CheckingMemory,
            )
            runCatching {
                withContext(Dispatchers.IO) {
                    quickScanOptimizer.run(
                        mediaAccessGranted = mediaAccessGranted,
                        onStep = { step ->
                            withContext(Dispatchers.Main.immediate) {
                                val latest = _uiState.value as? HomeUiState.Success ?: return@withContext
                                _uiState.value = latest.copy(quickScanStep = step)
                            }
                        },
                    )
                }
            }.onSuccess { (result, mediaSnapshot) ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        deviceStorageRepository.getDeviceStorage()
                    }
                }.onSuccess { storage ->
                    val latest = _uiState.value as? HomeUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        isScanning = false,
                        quickScanStep = null,
                        storage = storage,
                        scanSnapshot = mediaSnapshot ?: latest.scanSnapshot,
                        scanError = null,
                        memorySnapshot = result.memoryAfter,
                        lastQuickScanResult = result,
                    )
                    _quickScanCelebration.value = result
                    viewModelScope.launch {
                        delay(QUICK_SCAN_CELEBRATION_MS)
                        dismissQuickScanCelebration()
                    }
                }
            }.onFailure {
                val latest = _uiState.value as? HomeUiState.Success ?: return@onFailure
                _quickScanCelebration.value = null
                _uiState.value = latest.copy(
                    isScanning = false,
                    quickScanStep = null,
                    scanError = context.getString(R.string.error_quick_scan_failed),
                )
            }
        }
    }

    fun dismissQuickScanCelebration() {
        _quickScanCelebration.value = null
    }

    fun updateMediaAccessGranted(granted: Boolean) {
        mediaAccessGranted = granted
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(
                mediaAccessGranted = granted,
                scanError = if (granted) null else current.scanError,
            )
        }
    }

    companion object {
        private const val QUICK_SCAN_CELEBRATION_MS = 4_500L
    }
}
