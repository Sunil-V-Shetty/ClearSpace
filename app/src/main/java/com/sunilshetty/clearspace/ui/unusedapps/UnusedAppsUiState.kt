package com.sunilshetty.clearspace.ui.unusedapps

import com.sunilshetty.clearspace.data.model.UnusedAppItem

sealed interface UnusedAppsUiState {
    data object Loading : UnusedAppsUiState

    data class Ready(
        val apps: List<UnusedAppItem>,
        val selectedKeys: Set<String>,
        val isUninstalling: Boolean,
        val message: String?,
        val needsUsageAccess: Boolean,
    ) : UnusedAppsUiState {
        val appCount: Int get() = apps.size
        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(val message: String) : UnusedAppsUiState
}
