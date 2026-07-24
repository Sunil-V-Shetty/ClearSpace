package com.sunilshetty.clearspace.ui.detail

import com.sunilshetty.clearspace.data.model.CategoryMediaItem
import com.sunilshetty.clearspace.data.model.StorageCategory

sealed interface CategoryDetailUiState {
    data object Idle : CategoryDetailUiState

    data object Loading : CategoryDetailUiState

    data class Ready(
        val category: StorageCategory,
        val items: List<CategoryMediaItem>,
        val selectedKeys: Set<String>,
        val selectedBytes: Long,
        val isDeleting: Boolean,
        val message: String?,
        val needsStorageAccess: Boolean,
    ) : CategoryDetailUiState {
        val itemCount: Int get() = items.size
        val totalBytes: Long get() = items.sumOf { it.sizeBytes }
        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(val message: String) : CategoryDetailUiState
}

fun buildCategoryMediaIndex(items: List<CategoryMediaItem>): Map<String, CategoryMediaItem> =
    items.associateBy { it.selectionKey }

fun selectedBytesForCategoryKeys(
    index: Map<String, CategoryMediaItem>,
    keys: Set<String>,
): Long = keys.sumOf { key -> index[key]?.sizeBytes ?: 0L }

fun StorageCategory.mediaLocationLabel(): String = when (this) {
    StorageCategory.PHOTOS -> "Photos"
    StorageCategory.VIDEOS -> "Videos"
    StorageCategory.DOWNLOADS -> "Downloads"
}
