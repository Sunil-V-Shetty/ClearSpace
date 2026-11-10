package com.sunilshetty.clearspace.ui.largefiles

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.LargeFilesRepository
import com.sunilshetty.clearspace.ui.detail.canScanJunkFilesystem
import com.sunilshetty.clearspace.ui.detail.hasJunkReadAccess
import com.sunilshetty.clearspace.ui.components.showDeleteCelebration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LargeFilesViewModel @Inject constructor(
    private val largeFilesRepository: LargeFilesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LargeFilesUiState>(LargeFilesUiState.Loading)
    val uiState: StateFlow<LargeFilesUiState> = _uiState.asStateFlow()

    private val _deleteCelebrationVisible = MutableStateFlow(false)
    val deleteCelebrationVisible: StateFlow<Boolean> = _deleteCelebrationVisible.asStateFlow()

    private var lastScanAtEpochMs: Long = 0L
    private var lastScanAccessGranted: Boolean = false
    private var scanInFlight: Boolean = false

    private val cleanup = com.sunilshetty.clearspace.ui.components.CleanupController()
    private var deleteFlowInFlight = false
    private var pendingMessage: String? = null

    init {
        loadLargeFiles(force = true)
    }

    fun loadLargeFiles(force: Boolean = false) {
        val accessGranted = hasJunkReadAccess(context)
        val cacheFresh = !force &&
            accessGranted == lastScanAccessGranted &&
            System.currentTimeMillis() - lastScanAtEpochMs < SCAN_CACHE_TTL_MS &&
            _uiState.value is LargeFilesUiState.Ready

        if (cacheFresh || scanInFlight || deleteFlowInFlight) return

        viewModelScope.launch {
            scanInFlight = true
            val showLoading = force || _uiState.value !is LargeFilesUiState.Ready
            if (showLoading) {
                _uiState.value = LargeFilesUiState.Loading
            }
            if (!accessGranted) {
                _uiState.value = LargeFilesUiState.Ready(
                    files = emptyList(),
                    selectedKeys = emptySet(),
                    selectedBytes = 0L,
                    isDeleting = false,
                    message = pendingMessage.also { pendingMessage = null },
                    needsStorageAccess = true,
                )
                lastScanAccessGranted = false
                scanInFlight = false
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    largeFilesRepository.loadLargeFiles()
                }
            }.onSuccess { files ->
                _uiState.value = LargeFilesUiState.Ready(
                    files = files,
                    selectedKeys = emptySet(),
                    selectedBytes = 0L,
                    isDeleting = false,
                    message = pendingMessage.also { pendingMessage = null },
                    needsStorageAccess = false,
                )
                lastScanAtEpochMs = System.currentTimeMillis()
                lastScanAccessGranted = true
            }.onFailure {
                if (_uiState.value is LargeFilesUiState.Loading) {
                    _uiState.value = LargeFilesUiState.Error(
                        message = context.getString(R.string.large_files_error_load),
                    )
                }
            }
            scanInFlight = false
        }
    }

    fun onScreenResumed() {
        val accessChanged = hasJunkReadAccess(context) != lastScanAccessGranted
        loadLargeFiles(force = accessChanged)
    }

    fun toggleSelection(selectionKey: String) {
        val current = _uiState.value as? LargeFilesUiState.Ready ?: return
        val fileIndex = buildLargeFileIndex(current.files)
        val updated = if (selectionKey in current.selectedKeys) {
            current.selectedKeys - selectionKey
        } else {
            current.selectedKeys + selectionKey
        }
        _uiState.value = current.withSelection(updated, fileIndex)
    }

    fun selectAll() {
        val current = _uiState.value as? LargeFilesUiState.Ready ?: return
        val fileIndex = buildLargeFileIndex(current.files)
        _uiState.value = current.withSelection(fileIndex.keys, fileIndex)
    }

    fun clearSelection() {
        val current = _uiState.value as? LargeFilesUiState.Ready ?: return
        val fileIndex = buildLargeFileIndex(current.files)
        _uiState.value = current.withSelection(emptySet(), fileIndex)
    }

    fun runDeleteSelected(launchSystemDelete: (PendingIntent) -> Unit) {
        val ready = _uiState.value as? LargeFilesUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isDeleting || deleteFlowInFlight) return
        val items = ready.files.filter { it.selectionKey in ready.selectedKeys }.map { it.toJunkFileItem() }
        deleteFlowInFlight = true
        _uiState.value = ready.copy(isDeleting = true, message = null)
        viewModelScope.launch {
            try {
                val deleted = cleanup.clean(items, largeFilesRepository::planDelete,
                    largeFilesRepository::createDeleteRequest, largeFilesRepository::deleteSelected, launchSystemDelete)
                pendingMessage = context.getString(R.string.cleanup_result, deleted, items.size)
                if (deleted == items.size && deleted > 0) {
                    showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pendingMessage = context.getString(R.string.cleanup_failed)
            } finally {
                deleteFlowInFlight = false
            }
            loadLargeFiles(force = true)
        }
    }

    fun onDeleteFinished(success: Boolean) {
        cleanup.onApproval(success)
    }

    private fun LargeFilesUiState.Ready.withSelection(
        selectedKeys: Set<String>,
        fileIndex: Map<String, com.sunilshetty.clearspace.data.model.LargeFileItem>,
    ): LargeFilesUiState.Ready {
        return copy(
            selectedKeys = selectedKeys,
            selectedBytes = selectedBytesForKeys(fileIndex, selectedKeys),
            message = null,
        )
    }

    fun needsLimitedScanHint(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !canScanJunkFilesystem(context)
    }

    companion object {
        private const val SCAN_CACHE_TTL_MS = 60_000L
    }
}
