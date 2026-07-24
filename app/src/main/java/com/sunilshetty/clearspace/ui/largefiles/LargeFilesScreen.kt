package com.sunilshetty.clearspace.ui.largefiles

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.LargeFileItem
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceCompleteCelebrationOverlay
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.InfinityLoaderGif
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.hasJunkReadAccess
import com.sunilshetty.clearspace.ui.detail.needsFullStorageAccessForJunkOnDevice
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LargeFilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteCelebrationVisible by viewModel.deleteCelebrationVisible.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val formatBytes = rememberStorageFormatBytes()
    val runtimePermissions = appRuntimePermissions()
    val permissionActions = LocalAppPermissionActions.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteFinished(result.resultCode == Activity.RESULT_OK)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ClearSpaceTopAppBar(
                title = stringResource(R.string.large_files_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? LargeFilesUiState.Ready
            if (ready != null && ready.fileCount > 0) {
                ClearSpacePrimaryButton(
                    onClick = {
                        viewModel.runDeleteSelected { pending ->
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pending.intentSender).build(),
                            )
                        }
                    },
                    enabled = ready.selectedKeys.isNotEmpty() && !ready.isDeleting,
                    label = stringResource(
                        R.string.large_files_delete_selected,
                        ready.selectedCount,
                        formatBytes(ready.selectedBytes),
                    ),
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
                LargeFilesUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InfinityLoaderGif(size = 128.dp)
                        Text(
                            text = stringResource(R.string.large_files_scanning),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is LargeFilesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadLargeFiles(force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is LargeFilesUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        LargeFilesHeader(
                            fileCount = state.fileCount,
                            selectedCount = state.selectedCount,
                            limitedScan = viewModel.needsLimitedScanHint(),
                            showAccessHint = state.needsStorageAccess ||
                                (runtimePermissions.isNotEmpty() && !hasJunkReadAccess(context)),
                            onSelectAll = viewModel::selectAll,
                            onClearSelection = viewModel::clearSelection,
                            onRequestAccess = { permissionActions?.requestAllPermissions() },
                            onRequestFullAccess = { permissionActions?.openFullStorageAccessSettings() },
                        )
                        if (state.message != null) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (state.files.isEmpty()) {
                            Text(
                                text = if (state.needsStorageAccess) {
                                    stringResource(R.string.media_access_request)
                                } else {
                                    stringResource(R.string.large_files_empty)
                                },
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(state.files, key = { it.selectionKey }) { file ->
                                    LargeFileRow(
                                        file = file,
                                        checked = file.selectionKey in state.selectedKeys,
                                        formatBytes = formatBytes,
                                        onCheckedChange = { viewModel.toggleSelection(file.selectionKey) },
                                    )
                                }
                            }
                        }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isDeleting,
                            message = stringResource(R.string.large_files_preparing_delete),
                        )
                    }
                }
            }
        }
    }
    ClearSpaceCompleteCelebrationOverlay(visible = deleteCelebrationVisible)
    }
}

@Composable
private fun LargeFilesHeader(
    fileCount: Int,
    selectedCount: Int,
    limitedScan: Boolean,
    showAccessHint: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestAccess: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.large_files_subtitle))
        if (limitedScan && needsFullStorageAccessForJunkOnDevice()) {
            BentoSubtleText(
                text = stringResource(R.string.junk_limited_scan_hint),
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(
                onClick = onRequestFullAccess,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.junk_allow_full_access))
            }
        }
        ClearSpaceSelectionSummaryRow(
            summary = stringResource(R.string.large_files_summary, fileCount, selectedCount),
            totalCount = fileCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (showAccessHint) {
            TextButton(onClick = onRequestAccess) {
                Text(stringResource(R.string.media_access_request))
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    file: LargeFileItem,
    checked: Boolean,
    formatBytes: (Long) -> String,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${formatBytes(file.sizeBytes)} · ${file.locationLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
