package com.sunilshetty.clearspace.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.local.QuickScanOptimizer
import com.sunilshetty.clearspace.data.model.QuickScanResult
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.BentoStorageProgress
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.BentoValue
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpaceSuccessMark
import com.sunilshetty.clearspace.ui.components.QuickScanPrimaryButton
import com.sunilshetty.clearspace.ui.components.QuickScanSecondaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceLoadingIndicator
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatPercent
import com.sunilshetty.clearspace.ui.home.HomeUiState
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions
import com.sunilshetty.clearspace.util.StorageFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScanDetailScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    val viewModel = rememberHomeViewModel(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickScanCelebration by viewModel.quickScanCelebration.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaPermissions = mediaReadPermissions()
    val permissionActions = LocalAppPermissionActions.current
    val formatBytes = rememberStorageFormatBytes()
    val formatPercent = rememberStorageFormatPercent()

    val runQuickScan: () -> Unit = {
        viewModel.updateMediaAccessGranted(hasMediaAccess(context))
        viewModel.runQuickScan()
    }

    val successState = uiState as? HomeUiState.Success
    val showQuickScanBar = successState != null &&
        !successState.isScanning &&
        quickScanCelebration == null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ClearSpaceTopAppBar(
                title = stringResource(R.string.quick_scan_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            if (showQuickScanBar) {
                QuickScanPrimaryButton(
                    onClick = runQuickScan,
                    modifier = Modifier.clearSpaceBottomBarPadding(),
                )
            }
        },
    ) { innerPadding ->
        BentoScreenBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is HomeUiState.Success -> {
                    when {
                        state.isScanning -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                QuickScanRunningContent(step = state.quickScanStep)
                            }
                        }
                        quickScanCelebration != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                QuickScanCompleteContent(
                                    result = quickScanCelebration!!,
                                    formatPercent = formatPercent,
                                    onContinue = viewModel::dismissQuickScanCelebration,
                                )
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                                    .padding(bottom = 8.dp),
                            ) {
                                QuickScanIdleContent(
                                    memorySnapshot = state.memorySnapshot,
                                    lastResult = state.lastQuickScanResult,
                                    scanError = state.scanError,
                                    formatBytes = formatBytes,
                                    formatPercent = formatPercent,
                                    mediaAccessGranted = state.mediaAccessGranted,
                                    showMediaAccessHint = mediaPermissions.isNotEmpty() &&
                                        !state.mediaAccessGranted,
                                    onRequestMediaAccess = {
                                        permissionActions?.requestAllPermissions()
                                    },
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DetailBodyText(text = stringResource(R.string.error_storage_title))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickScanCompleteContent(
    result: QuickScanResult,
    formatPercent: (Float) -> String,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ClearSpaceSuccessMark(size = 120.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.quick_scan_done_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                R.string.quick_scan_cache_result,
                com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes()(result.cacheBytesCleared),
            ),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        BentoSubtleText(
            text = stringResource(
                R.string.quick_scan_done_subtitle,
                formatPercent(result.memoryAfter.usedFraction),
            ),
            modifier = Modifier.padding(top = 12.dp).padding(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        QuickScanSecondaryButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuickScanRunningContent(
    step: QuickScanOptimizer.QuickScanStep?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ClearSpaceLoadingIndicator(size = 120.dp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.quick_scan_running),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = quickScanStepLabel(step),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun QuickScanIdleContent(
    memorySnapshot: com.sunilshetty.clearspace.data.model.DeviceMemorySnapshot?,
    lastResult: QuickScanResult?,
    scanError: String?,
    formatBytes: (Long) -> String,
    formatPercent: (Float) -> String,
    mediaAccessGranted: Boolean,
    showMediaAccessHint: Boolean,
    onRequestMediaAccess: () -> Unit,
) {
    DetailBodyText(text = stringResource(R.string.quick_scan_description))

    if (memorySnapshot != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.quick_scan_ram_heading),
            style = MaterialTheme.typography.titleSmall,
        )
        BentoValue(
            text = stringResource(
                R.string.quick_scan_ram_used,
                formatBytes(memorySnapshot.usedBytes),
                formatBytes(memorySnapshot.totalBytes),
                formatPercent(memorySnapshot.usedFraction),
            ),
        )
        BentoStorageProgress(usedFraction = memorySnapshot.usedFraction)
        BentoSubtleText(
            text = stringResource(
                R.string.quick_scan_ram_meta,
                formatBytes(memorySnapshot.availBytes),
                memorySnapshot.backgroundProcessCount,
            ),
        )
        if (memorySnapshot.lowMemory) {
            Text(
                text = stringResource(R.string.quick_scan_low_memory_hint),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    lastResult?.let { result ->
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.quick_scan_last_run_heading),
            style = MaterialTheme.typography.titleSmall,
        )
        DetailBodyText(
            text = stringResource(
                R.string.quick_scan_last_run_time,
                StorageFormatter.formatTimestamp(result.completedAtEpochMs),
            ),
        )
        QuickScanResultLines(result = result, formatBytes = formatBytes)
    }

    if (scanError != null) {
        Text(
            text = scanError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (showMediaAccessHint) {
        TextButton(onClick = onRequestMediaAccess) {
            Text(stringResource(R.string.quick_scan_media_hint))
        }
    } else if (!mediaAccessGranted) {
        BentoSubtleText(
            text = stringResource(R.string.quick_scan_without_media_hint),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun QuickScanResultLines(
    result: QuickScanResult,
    formatBytes: (Long) -> String,
) {
    val lines = buildList {
        if (result.memoryFreedBytes > 0L) {
            add(stringResource(R.string.quick_scan_result_memory, formatBytes(result.memoryFreedBytes)))
        } else {
            add(stringResource(R.string.quick_scan_result_memory_refreshed))
        }
        if (result.mediaScanCompleted) {
            add(stringResource(R.string.quick_scan_result_media))
        }
        if (result.cacheBytesCleared > 0L) {
            add(stringResource(R.string.quick_scan_result_cache, formatBytes(result.cacheBytesCleared)))
        }
        add(stringResource(R.string.quick_scan_result_storage))
    }
    lines.forEach { line ->
        DetailBodyText(text = line)
    }
}

@Composable
private fun quickScanStepLabel(step: QuickScanOptimizer.QuickScanStep?): String {
    val res = when (step) {
        QuickScanOptimizer.QuickScanStep.CheckingMemory -> R.string.quick_scan_step_memory
        QuickScanOptimizer.QuickScanStep.RefreshingStorage -> R.string.quick_scan_step_storage
        QuickScanOptimizer.QuickScanStep.ScanningMedia -> R.string.quick_scan_step_media
        QuickScanOptimizer.QuickScanStep.ClearingTempCache -> R.string.quick_scan_step_cache
        QuickScanOptimizer.QuickScanStep.FreeingMemory -> R.string.quick_scan_step_gc
        QuickScanOptimizer.QuickScanStep.Finishing -> R.string.quick_scan_step_finish
        null -> R.string.quick_scan_step_memory
    }
    return stringResource(res)
}
