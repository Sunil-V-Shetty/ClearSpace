package com.sunilshetty.clearspace.data.model

data class UnusedAppItem(
    val packageName: String,
    val appLabel: String,
    val lastUsedEpochMs: Long,
    val isNeverUsed: Boolean,
    val usageUnknown: Boolean = false,
) {
    val selectionKey: String get() = packageName
}
