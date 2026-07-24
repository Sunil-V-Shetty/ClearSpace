package com.sunilshetty.clearspace.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.BentoStorageProgress
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.BentoValue
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
                BentoValue(text = formatBytes(valueSelector(state.storage)))
                if (showUsageBar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BentoStorageProgress(usedFraction = state.storage.usedPercent)
                    BentoSubtleText(
                        text = stringResource(
                            R.string.storage_used_of_total,
                            formatBytes(state.storage.usedBytes),
                            formatBytes(state.storage.totalBytes),
                            formatPercent(state.storage.usedPercent),
                        ),
                    )
                }
                if (subtitleRes != null) {
                    DetailBodyText(text = stringResource(subtitleRes))
                }
                DetailBodyText(
                    text = stringResource(
                        R.string.storage_free_amount,
                        formatBytes(state.storage.freeBytes),
                    ),
                )
            }
            else -> DetailBodyText(text = stringResource(R.string.error_storage_title))
        }
    }
}
