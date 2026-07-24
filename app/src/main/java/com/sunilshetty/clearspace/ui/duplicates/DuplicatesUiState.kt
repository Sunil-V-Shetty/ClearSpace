package com.sunilshetty.clearspace.ui.duplicates

import com.sunilshetty.clearspace.data.model.DuplicateFileItem
import com.sunilshetty.clearspace.data.model.DuplicateGroup

sealed interface DuplicatesUiState {
    data object Loading : DuplicatesUiState

    data class Ready(
        val groups: List<DuplicateGroup>,
        val fileIndex: Map<String, DuplicateFileItem>,
        val selectedKeys: Set<String>,
        val selectedBytes: Long,
        val expandedGroupIds: Set<String>,
        val isDeleting: Boolean,
        val message: String?,
        val limitedScan: Boolean,
    ) : DuplicatesUiState {
        val duplicateFileCount: Int get() = groups.sumOf { it.files.size }
        val groupCount: Int get() = groups.size
        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(val message: String) : DuplicatesUiState
}

fun buildDuplicateFileIndex(groups: List<DuplicateGroup>): Map<String, DuplicateFileItem> {
    return groups.flatMap { it.files }.associateBy { it.selectionKey }
}

fun defaultDuplicateSelection(groups: List<DuplicateGroup>): Set<String> {
    return groups.flatMap { it.defaultSelectedKeys() }.toSet()
}

fun selectedBytesForDuplicateKeys(
    fileIndex: Map<String, DuplicateFileItem>,
    selectedKeys: Set<String>,
): Long = selectedKeys.sumOf { key -> fileIndex[key]?.sizeBytes ?: 0L }

fun defaultExpandedDuplicateGroups(groups: List<DuplicateGroup>): Set<String> {
    return groups.take(3).map { it.groupKey }.toSet()
}
