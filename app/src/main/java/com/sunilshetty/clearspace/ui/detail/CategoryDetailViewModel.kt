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

    private val cleanup = com.sunilshetty.clearspace.ui.components.CleanupController()
    private var deleteFlowInFlight = false
    private var pendingMessage: String? = null

    fun loadCategory(category: StorageCategory, force: Boolean = false) {
        if (!force && activeCategory == category && _uiState.value is CategoryDetailUiState.Ready) {
            return
        }
        if (scanInFlight || deleteFlowInFlight) return

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
                    message = pendingMessage.also { pendingMessage = null },
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
                    message = pendingMessage.also { pendingMessage = null },
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
        val items = ready.items.filter { it.selectionKey in ready.selectedKeys }.map { it.toJunkFileItem(ready.category.mediaLocationLabel()) }
        deleteFlowInFlight = true
        _uiState.value = ready.copy(isDeleting = true, message = null)
        viewModelScope.launch {
            try {
                val deleted = cleanup.clean(items, categoryMediaRepository::planDelete,
                    categoryMediaRepository::createDeleteRequest, categoryMediaRepository::deleteSelected, launchSystemDelete)
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
            loadCategory(ready.category, force = true)
        }
    }

    fun onDeleteFinished(category: StorageCategory, success: Boolean) {
        cleanup.onApproval(success)
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
