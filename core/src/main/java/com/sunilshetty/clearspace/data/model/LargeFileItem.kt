package com.sunilshetty.clearspace.data.model

import android.net.Uri

data class LargeFileItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val relativePath: String?,
    val locationLabel: String,
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
