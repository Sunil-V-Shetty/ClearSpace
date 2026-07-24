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

    private var postSystemDeleteDirect: List<JunkFileItem>? = null
    private var pendingSystemDeleteBatches: ArrayDeque<List<JunkFileItem>> = ArrayDeque()
    private var systemDeleteLauncher: ((PendingIntent) -> Unit)? = null
    private var deleteFlowInFlight: Boolean = false

    init {
        loadLargeFiles(force = true)
    }

    fun loadLargeFiles(force: Boolean = false) {
        val accessGranted = hasJunkReadAccess(context)
        val cacheFresh = !force &&
            accessGranted == lastScanAccessGranted &&
            System.currentTimeMillis() - lastScanAtEpochMs < SCAN_CACHE_TTL_MS &&
            _uiState.value is LargeFilesUiState.Ready

        if (cacheFresh || scanInFlight) return

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
                    message = null,
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
                    message = null,
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

        val fileIndex = buildLargeFileIndex(ready.files)
        _uiState.value = ready.copy(isDeleting = true, message = null)
        systemDeleteLauncher = launchSystemDelete
        deleteFlowInFlight = true

        viewModelScope.launch {
            val selectedItems = withContext(Dispatchers.Default) {
                ready.selectedKeys.mapNotNull { key -> fileIndex[key]?.toJunkFileItem() }
            }
            val plan = withContext(Dispatchers.Default) {
                largeFilesRepository.planDelete(selectedItems)
            }
            postSystemDeleteDirect = plan.directDelete.takeIf { it.isNotEmpty() }
            pendingSystemDeleteBatches = ArrayDeque(plan.systemPromptBatches)

            if (pendingSystemDeleteBatches.isNotEmpty()) {
                startNextSystemDeleteBatch()
            } else {
                deleteFlowInFlight = false
                systemDeleteLauncher = null
                postSystemDeleteDirect = null
                deleteWithoutSystemPrompt(selectedItems)
            }
        }
    }

    fun onDeleteFinished(success: Boolean) {
        if (success && pendingSystemDeleteBatches.isNotEmpty()) {
            startNextSystemDeleteBatch()
            return
        }
        val followUp = postSystemDeleteDirect
        postSystemDeleteDirect = null
        pendingSystemDeleteBatches.clear()
        systemDeleteLauncher = null
        deleteFlowInFlight = false

        if (success) {
            showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
            if (!followUp.isNullOrEmpty()) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        largeFilesRepository.deleteSelected(followUp)
                    }
                    loadLargeFiles(force = true)
                }
            } else {
                loadLargeFiles(force = true)
            }
        } else {
            val current = _uiState.value as? LargeFilesUiState.Ready ?: return
            _uiState.value = current.copy(
                isDeleting = false,
                message = context.getString(R.string.large_files_error_delete),
            )
        }
    }

    private fun startNextSystemDeleteBatch() {
        viewModelScope.launch {
            val launcher = systemDeleteLauncher
            val batch = pendingSystemDeleteBatches.removeFirstOrNull()
            if (batch == null || launcher == null) {
                deleteFlowInFlight = false
                onDeleteFinished(success = true)
                return@launch
            }
            val pending = withContext(Dispatchers.IO) {
                largeFilesRepository.createDeleteRequest(batch)
            }
            if (pending != null) {
                deleteFlowInFlight = false
                launcher(pending)
            } else {
                postSystemDeleteDirect = (postSystemDeleteDirect.orEmpty() + batch).distinctBy { it.selectionKey }
                startNextSystemDeleteBatch()
            }
        }
    }

    private fun deleteWithoutSystemPrompt(items: List<JunkFileItem>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                largeFilesRepository.deleteSelected(items)
            }
            showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
            loadLargeFiles(force = true)
        }
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
