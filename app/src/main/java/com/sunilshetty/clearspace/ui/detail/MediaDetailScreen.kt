package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.BentoValue
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.components.BentoLabel
import com.sunilshetty.clearspace.ui.components.ClearSpaceStorageRow
import com.sunilshetty.clearspace.ui.components.ClearSpaceLoadingIndicator
import com.sunilshetty.clearspace.ui.home.HomeUiState
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.util.StorageFormatter

@Composable
fun MediaDetailScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    val viewModel = rememberHomeViewModel(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatBytes = rememberStorageFormatBytes()

    DetailScreenScaffold(titleRes = R.string.detail_media_title, onBack = onBack) {
        when (val state = uiState) {
            is HomeUiState.Success -> {
                val snapshot = state.scanSnapshot
                if (snapshot == null) {
                    BentoCard(Modifier.padding(vertical = 16.dp)) {
                        Text("No media scan yet", style = MaterialTheme.typography.titleMedium)
                        DetailBodyText("Scan to update sizes, or browse a category.")
                    }
                } else {
                    val total = snapshot.categories.sumOf { it.totalBytes }
                    BentoCard(Modifier.padding(vertical = 16.dp)) {
                        BentoLabel("Accessible media")
                        BentoValue(text = formatBytes(total))
                        if (total == 0L) DetailBodyText("No media found. Check access and scan again.")
                        DetailBodyText(stringResource(
                            R.string.scan_last_run,
                            StorageFormatter.formatTimestamp(snapshot.scannedAtEpochMs),
                        ))
                    }
                }
                Button(onClick = { navController.navigate(Constants.Navigation.SMART_SCAN) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan media")
                }
                DetailBodyText("Includes only files this app can access.")
                StorageCategory.entries.forEach { category ->
                    val bytes = snapshot?.categories?.firstOrNull { it.category == category }?.totalBytes
                    ClearSpaceStorageRow(stringResource(category.labelRes()), bytes?.let(formatBytes) ?: "View files",
                        when (category) {
                            StorageCategory.PHOTOS -> Icons.Outlined.Photo
                            StorageCategory.VIDEOS -> Icons.Outlined.VideoLibrary
                            StorageCategory.DOWNLOADS -> Icons.Outlined.Download
                        }, bytes?.takeIf { state.storage.totalBytes > 0L }?.let { it.toFloat() / state.storage.totalBytes },
                        { navController.navigate(category.navigationRoute()) }, Modifier.padding(vertical = 6.dp))
                }
                if (snapshot != null) DetailBodyText("Bars show a share of total storage.")
            }
            HomeUiState.Loading -> {
                ClearSpaceLoadingIndicator()
                DetailBodyText("Reading media overview…")
            }
            is HomeUiState.Error -> {
                DetailBodyText(state.message)
                Button(onClick = viewModel::loadHome) { Text(stringResource(R.string.action_try_again)) }
            }
        }
    }
}
