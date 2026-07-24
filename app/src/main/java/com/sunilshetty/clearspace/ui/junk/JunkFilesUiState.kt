package com.sunilshetty.clearspace.ui.junk

import com.sunilshetty.clearspace.data.model.JunkFileCategory
import com.sunilshetty.clearspace.data.model.JunkFileItem

data class JunkFileGroupUi(
    val category: JunkFileCategory,
    val files: List<JunkFileItem>,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

sealed interface JunkFilesUiState {
    data object Loading : JunkFilesUiState

    data class Ready(
        val groups: List<JunkFileGroupUi>,
        val fileIndex: Map<String, JunkFileItem>,
        val selectedKeys: Set<String>,
        val selectedBytes: Long,
        val expandedGroupIds: Set<String>,
        val isDeleting: Boolean,
        val message: String?,
        val limitedScan: Boolean,
    ) : JunkFilesUiState {
        val files: List<JunkFileItem> get() = groups.flatMap { it.files }
        val fileCount: Int get() = files.size
        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(val message: String) : JunkFilesUiState
}

fun buildFileIndex(files: List<JunkFileItem>): Map<String, JunkFileItem> {
    return files.associateBy { it.selectionKey }
}

fun selectedBytesForKeys(fileIndex: Map<String, JunkFileItem>, selectedKeys: Set<String>): Long {
    return selectedKeys.sumOf { key -> fileIndex[key]?.sizeBytes ?: 0L }
}

fun defaultSelectedKeys(files: List<JunkFileItem>): Set<String> {
    return files
        .filter { it.category.autoSelectByDefault }
        .map { it.selectionKey }
        .toSet()
}

fun defaultExpandedGroupIds(groups: List<JunkFileGroupUi>): Set<String> {
    return groups
        .filter { group -> !group.category.autoSelectByDefault && group.files.isNotEmpty() }
        .map { it.category.id }
        .toSet()
}

fun buildGroups(files: List<JunkFileItem>): List<JunkFileGroupUi> {
    return JunkFileCategory.entries
        .sortedBy { it.sortOrder }
        .mapNotNull { category ->
            val inCategory = files.filter { it.category == category }
            if (inCategory.isEmpty()) null else JunkFileGroupUi(category, inCategory)
        }
}
