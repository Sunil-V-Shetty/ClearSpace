package com.sunilshetty.clearspace.ui.trash

import com.sunilshetty.clearspace.data.model.TrashItem

sealed interface TrashUiState {
    data object Loading : TrashUiState

    data class Ready(
        val items: List<TrashItem>,
        val selectedKeys: Set<String>,
        val isWorking: Boolean,
        val message: String?,
    ) : TrashUiState {
        val itemCount: Int get() = items.size
        val selectedCount: Int get() = selectedKeys.size
        val selectedBytes: Long get() = items.filter { it.id in selectedKeys }.sumOf { it.sizeBytes }
    }

    data class Error(val message: String) : TrashUiState
}
