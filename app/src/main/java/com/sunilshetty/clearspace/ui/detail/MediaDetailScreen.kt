package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.BentoValue
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
                    DetailBodyText(text = stringResource(R.string.home_bento_scan_cta))
                } else {
                    val total = snapshot.categories.sumOf { it.totalBytes }
                    BentoValue(text = formatBytes(total))
                    snapshot.categories.forEach { breakdown ->
                        CategoryBreakdownLine(breakdown = breakdown, formatBytes = formatBytes)
                    }
                    DetailBodyText(
                        text = stringResource(
                            R.string.scan_last_run,
                            StorageFormatter.formatTimestamp(snapshot.scannedAtEpochMs),
                        ),
                    )
                }
            }
            else -> DetailBodyText(text = stringResource(R.string.error_storage_title))
        }
    }
}
