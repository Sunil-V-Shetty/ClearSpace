package com.sunilshetty.clearspace.data.model

import android.net.Uri

data class JunkFileItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val relativePath: String?,
    val locationLabel: String,
    val absolutePath: String? = null,
    val category: JunkFileCategory = JunkFileCategory.REVIEW_OTHER,
) {
    val selectionKey: String get() = absolutePath ?: uri.toString()
}
