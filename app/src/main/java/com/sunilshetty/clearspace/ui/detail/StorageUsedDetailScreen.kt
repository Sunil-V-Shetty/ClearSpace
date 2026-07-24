package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R

@Composable
fun StorageUsedDetailScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    StorageDetailContent(
        titleRes = R.string.detail_storage_used_title,
        onBack = onBack,
        navController = navController,
        valueSelector = { it.usedBytes },
        subtitleRes = null,
        showUsageBar = true,
    )
}
