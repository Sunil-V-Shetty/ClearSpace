package com.sunilshetty.clearspace.data.model

import android.net.Uri

data class CategoryMediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val relativePath: String?,
    val dateModifiedEpochMs: Long,
) {
    val selectionKey: String get() = uri.toString()

    fun toJunkFileItem(locationLabel: String): JunkFileItem = JunkFileItem(
        id = id,
        uri = uri,
        displayName = displayName,
        sizeBytes = sizeBytes,
        relativePath = relativePath,
        locationLabel = locationLabel,
    )
}
