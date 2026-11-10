package com.sunilshetty.clearspace.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.BentoStorageProgress
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.BentoValue
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.components.BentoLabel
import com.sunilshetty.clearspace.ui.components.ClearSpaceLoadingIndicator
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatPercent
import com.sunilshetty.clearspace.ui.home.HomeUiState

@Composable
internal fun StorageDetailContent(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    navController: NavHostController,
    valueSelector: (DeviceStorageInfo) -> Long,
    subtitleRes: Int?,
    showUsageBar: Boolean = false,
) {
    val viewModel = rememberHomeViewModel(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatBytes = rememberStorageFormatBytes()
    val formatPercent = rememberStorageFormatPercent()

    DetailScreenScaffold(titleRes = titleRes, onBack = onBack) {
        when (val state = uiState) {
            is HomeUiState.Success -> {
                BentoCard(modifier = Modifier.padding(vertical = 16.dp)) {
                    BentoLabel(if (showUsageBar) "Space in use" else "Available space")
                    BentoValue(text = formatBytes(valueSelector(state.storage)))
                    BentoStorageProgress(usedFraction = state.storage.usedPercent)
                    BentoSubtleText(
                        text = stringResource(
                            R.string.storage_used_of_total,
                            formatBytes(state.storage.usedBytes),
                            formatBytes(state.storage.totalBytes),
                            formatPercent(state.storage.usedPercent),
                        ),
                    )
                    Text("Total capacity: ${formatBytes(state.storage.totalBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Available: ${formatBytes(state.storage.freeBytes)}", style = MaterialTheme.typography.bodyMedium)
                }
                DetailBodyText("Includes Android, apps and files.")
                DetailBodyText("Empty Trash to free space used by recovery copies.")
                Button(onClick = { navController.navigate(Constants.Navigation.RECLAIMABLE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Review cleanup")
                }
                Button(onClick = { navController.navigate(Constants.Navigation.MEDIA) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Media overview")
                }
            }
            HomeUiState.Loading -> {
                ClearSpaceLoadingIndicator()
                DetailBodyText("Reading internal storage…")
            }
            is HomeUiState.Error -> {
                DetailBodyText(state.message)
                Button(onClick = viewModel::loadHome) { Text(stringResource(R.string.action_try_again)) }
            }
        }
    }
}
