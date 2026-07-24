package com.sunilshetty.clearspace.ui.largefiles

import com.sunilshetty.clearspace.data.model.LargeFileItem

sealed interface LargeFilesUiState {
    data object Loading : LargeFilesUiState

    data class Ready(
        val files: List<LargeFileItem>,
        val selectedKeys: Set<String>,
        val selectedBytes: Long,
        val isDeleting: Boolean,
        val message: String?,
        val needsStorageAccess: Boolean,
    ) : LargeFilesUiState {
        val fileCount: Int get() = files.size
        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(val message: String) : LargeFilesUiState
}

fun buildLargeFileIndex(files: List<LargeFileItem>): Map<String, LargeFileItem> =
    files.associateBy { it.selectionKey }

fun selectedBytesForKeys(fileIndex: Map<String, LargeFileItem>, keys: Set<String>): Long =
    keys.sumOf { key -> fileIndex[key]?.sizeBytes ?: 0L }
