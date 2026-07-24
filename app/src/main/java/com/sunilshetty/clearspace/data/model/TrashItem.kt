package com.sunilshetty.clearspace.data.model

data class TrashItem(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val deletedAtEpochMs: Long,
    val daysLeft: Int,
) {
    val selectionKey: String get() = id
}
