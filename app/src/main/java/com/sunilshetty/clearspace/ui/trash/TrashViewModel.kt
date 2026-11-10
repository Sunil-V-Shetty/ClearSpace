package com.sunilshetty.clearspace.ui.trash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.repository.TrashRepository
import com.sunilshetty.clearspace.ui.components.showDeleteCelebration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrashUiState>(TrashUiState.Loading)
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val _deleteCelebrationVisible = MutableStateFlow(false)
    private var operationMessage: String? = null
    val deleteCelebrationVisible: StateFlow<Boolean> = _deleteCelebrationVisible.asStateFlow()

    init {
        loadTrash(force = true)
    }

    fun onScreenResumed() {
        loadTrash(force = false)
    }

    fun loadTrash(force: Boolean = false) {
        val current = _uiState.value
        if (!force && current is TrashUiState.Ready) return

        viewModelScope.launch {
            val showFullScreenLoading = force || current !is TrashUiState.Ready
            if (showFullScreenLoading) {
                _uiState.value = TrashUiState.Loading
            }
            runCatching {
                trashRepository.loadActiveItems()
            }.onSuccess { items ->
                val previous = _uiState.value as? TrashUiState.Ready
                val selectedKeys = previous?.selectedKeys?.intersect(items.map { it.id }.toSet()) ?: emptySet()
                _uiState.value = TrashUiState.Ready(
                    items = items,
                    selectedKeys = selectedKeys,
                    isWorking = false,
                    message = operationMessage.also { operationMessage = null },
                )
            }.onFailure {
                _uiState.value = TrashUiState.Error(
                    message = context.getString(R.string.trash_error_load),
                )
            }
        }
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value as? TrashUiState.Ready ?: return
        val updated = if (id in current.selectedKeys) {
            current.selectedKeys - id
        } else {
            current.selectedKeys + id
        }
        _uiState.value = current.copy(selectedKeys = updated, message = null)
    }

    fun selectAll() {
        val current = _uiState.value as? TrashUiState.Ready ?: return
        _uiState.value = current.copy(
            selectedKeys = current.items.map { it.id }.toSet(),
            message = null,
        )
    }

    fun clearSelection() {
        val current = _uiState.value as? TrashUiState.Ready ?: return
        _uiState.value = current.copy(selectedKeys = emptySet(), message = null)
    }

    fun restoreSelected() {
        val ready = _uiState.value as? TrashUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isWorking) return

        viewModelScope.launch {
            _uiState.value = ready.copy(isWorking = true, message = null)
            runCatching {
                trashRepository.restoreItems(ready.selectedKeys)
            }.onSuccess { count ->
                if (count == 0) {
                    _uiState.value = ready.copy(
                        isWorking = false,
                        message = context.getString(R.string.trash_error_restore),
                    )
                } else {
                    if (count == ready.selectedKeys.size) showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
                    operationMessage = "Restored $count of ${ready.selectedKeys.size} selected files."
                    loadTrash(force = true)
                }
            }.onFailure {
                val latest = _uiState.value as? TrashUiState.Ready ?: return@launch
                _uiState.value = latest.copy(
                    isWorking = false,
                    message = context.getString(R.string.trash_error_restore),
                )
            }
        }
    }

    fun deleteForeverSelected() {
        val ready = _uiState.value as? TrashUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isWorking) return

        viewModelScope.launch {
            _uiState.value = ready.copy(isWorking = true, message = null)
            runCatching {
                trashRepository.deleteForever(ready.selectedKeys)
            }.onSuccess { count ->
                if (count == ready.selectedKeys.size) showDeleteCelebration(viewModelScope, _deleteCelebrationVisible)
                operationMessage = "Deleted $count of ${ready.selectedKeys.size} copies forever."
                loadTrash(force = true)
            }.onFailure {
                val latest = _uiState.value as? TrashUiState.Ready ?: return@launch
                _uiState.value = latest.copy(
                    isWorking = false,
                    message = context.getString(R.string.trash_error_delete),
                )
            }
        }
    }
}
