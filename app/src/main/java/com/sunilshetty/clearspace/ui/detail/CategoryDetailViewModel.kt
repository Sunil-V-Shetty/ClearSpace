package com.sunilshetty.clearspace.ui.detail

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.repository.CategoryMediaRepository
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
class CategoryDetailViewModel @Inject constructor(
    private val categoryMediaRepository: CategoryMediaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailUiState>(CategoryDetailUiState.Idle)
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    private val _deleteCelebrationVisible = MutableStateFlow(false)
    val deleteCelebrationVisible: StateFlow<Boolean> = _deleteCelebrationVisible.asStateFlow()

    private var activeCategory: StorageCategory? = null
    private var scanInFlight: Boolean = false

    private var postSystemDeleteDirect: List<JunkFileItem>? = null
    private var pendingSystemDeleteBatches: ArrayDeque<List<JunkFileItem>> = ArrayDeque()
    private var systemDeleteLauncher: ((PendingIntent) -> Unit)? = null
    private var deleteFlowInFlight: Boolean = false

    fun loadCategory(category: StorageCategory, force: Boolean = false) {
        if (!force && activeCategory == category && _uiState.value is CategoryDetailUiState.Ready) {
            return
        }
        if (scanInFlight) return

        activeCategory = category
        val accessGranted = hasJunkReadAccess(context)

        viewModelScope.launch {
            scanInFlight = true
            _uiState.value = CategoryDetailUiState.Loading

            if (!accessGranted) {
                _uiState.value = CategoryDetailUiState.Ready(
                    category = category,
                    items = emptyList(),
                    selectedKeys = emptySet(),
                    selectedBytes = 0L,
                    isDeleting = false,
                    message = null,
                    needsStorageAccess = true,
                )
                scanInFlight = false
                return@launch
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    categoryMediaRepository.loadCategoryMedia(category)
                }
            }.onSuccess { items ->
                _uiState.value = CategoryDetailUiState.Ready(
                    category = category,
                    items = items,
                    selectedKeys = emptySet(),
                    selectedBytes = 0L,
                    isDeleting = false,
                    message = null,
                    needsStorageAccess = false,
                )
            }.onFailure {
                _uiState.value = CategoryDetailUiState.Error(
                    message = context.getString(R.string.category_detail_error_load),
                )
            }
            scanInFlight = false
        }
    }

    fun onScreenResumed(category: StorageCategory) {
        val accessGranted = hasJunkReadAccess(context)
        val ready = _uiState.value as? CategoryDetailUiState.Ready
        if (ready?.needsStorageAccess == true && accessGranted) {
            loadCategory(category, force = true)
        }
    }

    fun toggleSelection(selectionKey: String) {
        val current = _uiState.value as? CategoryDetailUiState.Ready ?: return
        val index = buildCategoryMediaIndex(current.items)
        val updated = if (selectionKey in current.selectedKeys) {
            current.selectedKeys - selectionKey
        } else {
            current.selectedKeys + selectionKey
        }
        _uiState.value = current.withSelection(updated, index)
    }

    fun selectAll() {
        val current = _uiState.value as? CategoryDetailUiState.Ready ?: return
        val index = buildCategoryMediaIndex(current.items)
        _uiState.value = current.withSelection(index.keys, index)
    }

    fun clearSelection() {
        val current = _uiState.value as? CategoryDetailUiState.Ready ?: return
        val index = buildCategoryMediaIndex(current.items)
        _uiState.value = current.withSelection(emptySet(), index)
    }

    fun runDeleteSelected(launchSystemDelete: (PendingIntent) -> Unit) {
        val ready = _uiState.value as? CategoryDetailUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isDeleting || deleteFlowInFlight) return

        val index = buildCategoryMediaIndex(ready.items)
        val locationLabel = ready.category.mediaLocationLabel()
        _uiState.value = ready.copy(isDeleting = true, message = null)
        systemDeleteLauncher = launchSystemDelete
        deleteFlowInFlight = true

        viewModelScope.launch {
            val selectedItems = withContext(Dispatchers.Default) {
                ready.selectedKeys.mapNotNull { key ->
                    index[key]?.toJunkFileItem(locationLabel)
                }
            }
            val plan = withContext(Dispatchers.Default) {
                categoryMediaRepository.planDelete(selectedItems)
            }
            postSystemDeleteDirect = plan.directDelete.takeIf { it.isNotEmpty() }
            pendingSystemDeleteBatches = ArrayDeque(plan.systemPromptBatches)

            if (pendingSystemDeleteBatches.isNotEmpty()) {
                startNextSystemDeleteBatch(ready.category)
            } else {
                deleteFlowInFlight = false
                systemDeleteLauncher = null
                postSystemDeleteDirect = null
                deleteWithoutSystemPrompt(selectedItems, ready.category)
            }
        }
    }

    fun onDeleteFinished(category: StorageCategory, success: Boolean) {
        if (success && pendingSystemDeleteBatches.isNotEmpty()) {
            startNextSystemDeleteBatch(category)
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
                        categoryMediaRepository.deleteSelected(followUp)
                    }
                    loadCategory(category, force = true)
                }
            } else {
                loadCategory(category, force = true)
            }
        } else {
            val current = _uiState.value as? CategoryDetailUiState.Ready ?: return
            _uiState.value = current.copy(
                isDeleting = false,
                message = context.getString(R.string.category_detail_error_delete),
            )
        }
    }

    private fun startNextSystemDeleteBatch(category: StorageCategory) {
        viewModelScope.launch {
            val launcher = systemDeleteLauncher
            val batch = pendingSystemDeleteBatches.removeFirstOrNull()
            if (batch == null || launcher == null) {
                deleteFlowInFlight = false
                onDeleteFinished(category, success = true)
                return@launch
            }
            val pending = withContext(Dispatchers.IO) {
                categoryMediaRepository.createDeleteRequest(batch)
            }
            if (pending != null) {
                deleteFlowInFlight = false
                launcher(pending)
            } else {
                postSystemDeleteDirect = (postSystemDeleteDirect.orEmpty() + batch).distinctBy { it.selectionKey }
                startNextSystemDeleteBatch(category)
            }
        }
    }

    private fun deleteWithoutSystemPrompt(items: List<JunkFileItem>, category: StorageCategory) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                categoryMediaRepository.deleteSelected(items)
            }
            showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
            loadCategory(category, force = true)
        }
    }

    private fun CategoryDetailUiState.Ready.withSelection(
        selectedKeys: Set<String>,
        index: Map<String, com.sunilshetty.clearspace.data.model.CategoryMediaItem>,
    ): CategoryDetailUiState.Ready {
        return copy(
            selectedKeys = selectedKeys,
            selectedBytes = selectedBytesForCategoryKeys(index, selectedKeys),
            message = null,
        )
    }

    fun needsLimitedScanHint(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !canScanJunkFilesystem(context)
    }
}
