package com.sunilshetty.clearspace.data.model

import android.net.Uri

data class DuplicateFileItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val relativePath: String?,
    val locationLabel: String,
    val dateModifiedSec: Long,
    val contentHash: String,
) {
    val selectionKey: String get() = uri.toString()

    fun toJunkFileItem(): JunkFileItem = JunkFileItem(
        id = id,
        uri = uri,
        displayName = displayName,
        sizeBytes = sizeBytes,
        relativePath = relativePath,
        locationLabel = locationLabel,
    )
}

data class DuplicateGroup(
    val groupKey: String,
    val files: List<DuplicateFileItem>,
) {
    val keeperKey: String
        get() = files.maxByOrNull { it.dateModifiedSec }?.selectionKey ?: files.first().selectionKey

    fun defaultSelectedKeys(): Set<String> =
        files.map { it.selectionKey }.filterNot { it == keeperKey }.toSet()

    val duplicateCount: Int get() = (files.size - 1).coerceAtLeast(0)

    val totalBytes: Long get() = files.sumOf { it.sizeBytes }

    val titleName: String get() = files.firstOrNull()?.displayName ?: ""
}
