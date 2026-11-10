package com.sunilshetty.clearspace.ui.duplicates

import android.app.PendingIntent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.DuplicateFileItem
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.DuplicatesRepository
import com.sunilshetty.clearspace.ui.detail.canScanJunkFilesystem
import com.sunilshetty.clearspace.ui.detail.hasJunkReadAccess
import com.sunilshetty.clearspace.ui.components.showDeleteCelebration
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val duplicatesRepository: DuplicatesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DuplicatesUiState>(DuplicatesUiState.Loading)
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    private val _deleteCelebrationVisible = MutableStateFlow(false)
    val deleteCelebrationVisible: StateFlow<Boolean> = _deleteCelebrationVisible.asStateFlow()

    private var lastScanAtEpochMs: Long = 0L
    private var lastScanAccessGranted: Boolean = false
    private var scanInFlight: Boolean = false

    private val cleanup = com.sunilshetty.clearspace.ui.components.CleanupController()
    private var deleteFlowInFlight = false
    private var pendingMessage: String? = null

    init {
        loadDuplicates(force = true)
    }

    fun loadDuplicates(force: Boolean = false) {
        val accessGranted = hasJunkReadAccess(context)
        val cacheFresh = !force &&
            accessGranted == lastScanAccessGranted &&
            System.currentTimeMillis() - lastScanAtEpochMs < SCAN_CACHE_TTL_MS &&
            _uiState.value is DuplicatesUiState.Ready

        if (cacheFresh || scanInFlight || deleteFlowInFlight) return

        viewModelScope.launch {
            scanInFlight = true
            val showLoading = force || _uiState.value !is DuplicatesUiState.Ready
            if (showLoading) {
                _uiState.value = DuplicatesUiState.Loading
            }
            if (!accessGranted) {
                _uiState.value = DuplicatesUiState.Ready(
                    groups = emptyList(),
                    fileIndex = emptyMap(),
                    selectedKeys = emptySet(),
                    selectedBytes = 0L,
                    expandedGroupIds = emptySet(),
                    isDeleting = false,
                    message = pendingMessage.also { pendingMessage = null },
                    limitedScan = needsLimitedScanHint(),
                )
                lastScanAccessGranted = false
                scanInFlight = false
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    duplicatesRepository.scanDuplicateGroups()
                }
            }.onSuccess { groups ->
                val fileIndex = buildDuplicateFileIndex(groups)
                val previous = _uiState.value as? DuplicatesUiState.Ready
                val selectedKeys = previous?.selectedKeys?.intersect(fileIndex.keys) ?: emptySet()
                _uiState.value = DuplicatesUiState.Ready(
                    groups = groups,
                    fileIndex = fileIndex,
                    selectedKeys = selectedKeys,
                    selectedBytes = selectedBytesForDuplicateKeys(fileIndex, selectedKeys),
                    expandedGroupIds = previous?.expandedGroupIds ?: defaultExpandedDuplicateGroups(groups),
                    isDeleting = false,
                    message = pendingMessage.also { pendingMessage = null },
                    limitedScan = needsLimitedScanHint(),
                )
                lastScanAtEpochMs = System.currentTimeMillis()
                lastScanAccessGranted = true
            }.onFailure {
                if (_uiState.value is DuplicatesUiState.Loading) {
                    _uiState.value = DuplicatesUiState.Error(
                        message = context.getString(R.string.duplicates_error_load),
                    )
                }
            }
            scanInFlight = false
        }
    }

    fun onScreenResumed() {
        val accessChanged = hasJunkReadAccess(context) != lastScanAccessGranted
        loadDuplicates(force = accessChanged)
    }

    fun toggleSelection(selectionKey: String) {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        val updated = if (selectionKey in current.selectedKeys) {
            current.selectedKeys - selectionKey
        } else {
            current.selectedKeys + selectionKey
        }
        _uiState.value = current.withSelection(updated)
    }

    fun toggleGroupDuplicates(groupKey: String) {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        val group = current.groups.firstOrNull { it.groupKey == groupKey } ?: return
        val duplicateKeys = group.defaultSelectedKeys()
        val allSelected = duplicateKeys.isNotEmpty() && duplicateKeys.all { it in current.selectedKeys }
        val updated = if (allSelected) {
            current.selectedKeys - duplicateKeys
        } else {
            current.selectedKeys + duplicateKeys
        }
        _uiState.value = current.withSelection(updated)
    }

    fun toggleGroupExpanded(groupKey: String) {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        val updated = if (groupKey in current.expandedGroupIds) {
            current.expandedGroupIds - groupKey
        } else {
            current.expandedGroupIds + groupKey
        }
        _uiState.value = current.copy(expandedGroupIds = updated)
    }

    fun selectAllDuplicates() {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        _uiState.value = current.withSelection(defaultDuplicateSelection(current.groups))
    }

    fun selectAll() {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        _uiState.value = current.withSelection(current.fileIndex.keys)
    }

    fun clearSelection() {
        val current = _uiState.value as? DuplicatesUiState.Ready ?: return
        _uiState.value = current.withSelection(emptySet())
    }

    fun runCleanSelected(launchSystemDelete: (PendingIntent) -> Unit) {
        val ready = _uiState.value as? DuplicatesUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isDeleting || deleteFlowInFlight) return
        val items = ready.selectedKeys.mapNotNull { ready.fileIndex[it]?.toJunkFileItem() }
        deleteFlowInFlight = true
        _uiState.value = ready.copy(isDeleting = true, message = null)
        viewModelScope.launch {
            try {
                val deleted = cleanup.clean(items, duplicatesRepository::planDelete,
                    duplicatesRepository::createDeleteRequest, duplicatesRepository::deleteSelected, launchSystemDelete)
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
            loadDuplicates(force = true)
        }
    }

    fun onDeleteFinished(success: Boolean) {
        cleanup.onApproval(success)
    }

    private fun DuplicatesUiState.Ready.withSelection(selectedKeys: Set<String>): DuplicatesUiState.Ready {
        return copy(
            selectedKeys = selectedKeys,
            selectedBytes = selectedBytesForDuplicateKeys(fileIndex, selectedKeys),
            message = null,
        )
    }

    private fun needsLimitedScanHint(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !canScanJunkFilesystem(context)
    }

    companion object {
        private const val SCAN_CACHE_TTL_MS = 60_000L
    }
}
