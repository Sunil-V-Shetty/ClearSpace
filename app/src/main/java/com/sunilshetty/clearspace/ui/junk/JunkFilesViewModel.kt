package com.sunilshetty.clearspace.ui.junk

import android.app.PendingIntent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.JunkFileCategory
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.JunkFilesRepository
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
class JunkFilesViewModel @Inject constructor(
    private val junkFilesRepository: JunkFilesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<JunkFilesUiState>(JunkFilesUiState.Loading)
    val uiState: StateFlow<JunkFilesUiState> = _uiState.asStateFlow()

    private val _deleteCelebrationVisible = MutableStateFlow(false)
    val deleteCelebrationVisible: StateFlow<Boolean> = _deleteCelebrationVisible.asStateFlow()

    private var lastScanAtEpochMs: Long = 0L
    private var lastScanAccessGranted: Boolean = false
    private var scanInFlight: Boolean = false

    private val cleanup = com.sunilshetty.clearspace.ui.components.CleanupController()
    private var deleteFlowInFlight = false
    private var pendingMessage: String? = null

    init {
        loadJunkFiles(force = true)
    }

    fun loadJunkFiles(force: Boolean = false) {
        val accessGranted = hasJunkReadAccess(context)
        val cacheFresh = !force &&
            accessGranted == lastScanAccessGranted &&
            System.currentTimeMillis() - lastScanAtEpochMs < SCAN_CACHE_TTL_MS &&
            _uiState.value is JunkFilesUiState.Ready

        if (cacheFresh || scanInFlight || deleteFlowInFlight) return

        viewModelScope.launch {
            scanInFlight = true
            val showFullScreenLoading = force || _uiState.value !is JunkFilesUiState.Ready
            if (showFullScreenLoading) {
                _uiState.value = JunkFilesUiState.Loading
            }
            if (!accessGranted) {
                _uiState.value = JunkFilesUiState.Ready(
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
                    junkFilesRepository.loadJunkFiles()
                }
            }.onSuccess { files ->
                val groups = buildGroups(files)
                val fileIndex = buildFileIndex(files)
                val previous = _uiState.value as? JunkFilesUiState.Ready
                val fileKeys = fileIndex.keys
                val preservedSelection = previous?.selectedKeys?.intersect(fileKeys)
                val selectedKeys = preservedSelection?.takeIf { it.isNotEmpty() }
                    ?: defaultSelectedKeys(files)
                _uiState.value = JunkFilesUiState.Ready(
                    groups = groups,
                    fileIndex = fileIndex,
                    selectedKeys = selectedKeys,
                    selectedBytes = selectedBytesForKeys(fileIndex, selectedKeys),
                    expandedGroupIds = previous?.expandedGroupIds ?: defaultExpandedGroupIds(groups),
                    isDeleting = false,
                    message = pendingMessage.also { pendingMessage = null },
                    limitedScan = needsLimitedScanHint(),
                )
                lastScanAtEpochMs = System.currentTimeMillis()
                lastScanAccessGranted = true
            }.onFailure {
                if (_uiState.value is JunkFilesUiState.Loading) {
                    _uiState.value = JunkFilesUiState.Error(
                        message = context.getString(R.string.junk_error_load),
                    )
                }
            }
            scanInFlight = false
        }
    }

    fun onScreenResumed() {
        val accessGranted = hasJunkReadAccess(context)
        val accessChanged = accessGranted != lastScanAccessGranted
        loadJunkFiles(force = accessChanged)
    }

    fun toggleSelection(selectionKey: String) {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        val updatedKeys = if (selectionKey in current.selectedKeys) {
            current.selectedKeys - selectionKey
        } else {
            current.selectedKeys + selectionKey
        }
        _uiState.value = current.withSelection(updatedKeys)
    }

    fun toggleGroup(category: JunkFileCategory) {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        val group = current.groups.firstOrNull { it.category == category } ?: return
        val keys = group.files.map { it.selectionKey }.toSet()
        val allSelected = keys.isNotEmpty() && keys.all { it in current.selectedKeys }
        val updatedKeys = if (allSelected) {
            current.selectedKeys - keys
        } else {
            current.selectedKeys + keys
        }
        _uiState.value = current.withSelection(updatedKeys)
    }

    fun toggleGroupExpanded(categoryId: String) {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        val updated = if (categoryId in current.expandedGroupIds) {
            current.expandedGroupIds - categoryId
        } else {
            current.expandedGroupIds + categoryId
        }
        _uiState.value = current.copy(expandedGroupIds = updated)
    }

    fun selectAllSafe() {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        val safeKeys = current.files
            .filter { it.category.autoSelectByDefault }
            .map { it.selectionKey }
            .toSet()
        _uiState.value = current.withSelection(safeKeys)
    }

    fun selectAll() {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        _uiState.value = current.withSelection(current.fileIndex.keys)
    }

    fun clearSelection() {
        val current = _uiState.value as? JunkFilesUiState.Ready ?: return
        _uiState.value = current.withSelection(emptySet())
    }

    fun runCleanSelected(launchSystemDelete: (PendingIntent) -> Unit) {
        val ready = _uiState.value as? JunkFilesUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isDeleting || deleteFlowInFlight) return
        val items = ready.selectedKeys.mapNotNull { ready.fileIndex[it] }
        deleteFlowInFlight = true
        _uiState.value = ready.copy(isDeleting = true, message = null)
        viewModelScope.launch {
            try {
                val deleted = cleanup.clean(items, junkFilesRepository::planDelete,
                    junkFilesRepository::createDeleteRequest, junkFilesRepository::deleteSelected, launchSystemDelete)
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
            loadJunkFiles(force = true)
        }
    }

    fun onDeleteFinished(success: Boolean) {
        cleanup.onApproval(success)
    }

    private fun JunkFilesUiState.Ready.withSelection(selectedKeys: Set<String>): JunkFilesUiState.Ready {
        return copy(
            selectedKeys = selectedKeys,
            selectedBytes = selectedBytesForKeys(fileIndex, selectedKeys),
            message = null,
        )
    }

    private fun needsLimitedScanHint(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !canScanJunkFilesystem(context)
    }

    companion object {
        private const val SCAN_CACHE_TTL_MS = 60_000L
    }
}
