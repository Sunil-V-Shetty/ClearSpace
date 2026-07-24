package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R

@Composable
fun StorageFreeDetailScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    StorageDetailContent(
        titleRes = R.string.detail_storage_free_title,
        onBack = onBack,
        navController = navController,
        valueSelector = { it.freeBytes },
        subtitleRes = R.string.storage_on_device_note,
    )
}
