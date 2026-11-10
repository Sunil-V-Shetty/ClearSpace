package com.sunilshetty.clearspace.ui.duplicates

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.DuplicateFileItem
import com.sunilshetty.clearspace.data.model.DuplicateGroup
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceCompleteCelebrationOverlay
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.ClearSpaceLoadingIndicator
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.hasJunkReadAccess
import com.sunilshetty.clearspace.ui.detail.needsFullStorageAccessForJunkOnDevice
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuplicatesViewModel = hiltViewModel(),
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
                title = stringResource(R.string.duplicates_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? DuplicatesUiState.Ready
            if (ready != null && ready.groupCount > 0) {
                ClearSpacePrimaryButton(
                    onClick = {
                        viewModel.runCleanSelected { pending ->
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pending.intentSender).build(),
                            )
                        }
                    },
                    enabled = ready.selectedKeys.isNotEmpty() && !ready.isDeleting,
                    label = stringResource(
                        R.string.duplicates_delete_selected,
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
                DuplicatesUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClearSpaceLoadingIndicator(size = 96.dp)
                        Text(
                            text = stringResource(R.string.duplicates_scanning),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is DuplicatesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadDuplicates(force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is DuplicatesUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        DuplicatesHeader(
                            groupCount = state.groupCount,
                            fileCount = state.duplicateFileCount,
                            selectedCount = state.selectedCount,
                            limitedScan = state.limitedScan,
                            showAccessHint = runtimePermissions.isNotEmpty() && !hasJunkReadAccess(context),
                            onSelectAllDuplicates = viewModel::selectAllDuplicates,
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
                        if (state.groups.isEmpty()) {
                            Text(
                                text = stringResource(R.string.duplicates_empty),
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.groups, key = { it.groupKey }) { group ->
                                    val expanded = group.groupKey in state.expandedGroupIds
                                    DuplicateGroupSection(
                                        group = group,
                                        keeperKey = group.keeperKey,
                                        selectedKeys = state.selectedKeys,
                                        expanded = expanded,
                                        formatBytes = formatBytes,
                                        onToggleGroup = { viewModel.toggleGroupDuplicates(group.groupKey) },
                                        onToggleExpanded = { viewModel.toggleGroupExpanded(group.groupKey) },
                                        onToggleFile = viewModel::toggleSelection,
                                    )
                                }
                            }
                        }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isDeleting,
                            message = stringResource(R.string.duplicates_preparing_delete),
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
private fun DuplicatesHeader(
    groupCount: Int,
    fileCount: Int,
    selectedCount: Int,
    limitedScan: Boolean,
    showAccessHint: Boolean,
    onSelectAllDuplicates: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestAccess: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.duplicates_subtitle))
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
            summary = stringResource(R.string.duplicates_summary, groupCount, selectedCount),
            totalCount = fileCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSelectAllDuplicates) {
                Text(stringResource(R.string.duplicates_select_all_copies))
            }
            if (showAccessHint) {
                TextButton(onClick = onRequestAccess) {
                    Text(stringResource(R.string.media_access_request))
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupSection(
    group: DuplicateGroup,
    keeperKey: String,
    selectedKeys: Set<String>,
    expanded: Boolean,
    formatBytes: (Long) -> String,
    onToggleGroup: () -> Unit,
    onToggleExpanded: () -> Unit,
    onToggleFile: (String) -> Unit,
) {
    val duplicateKeys = group.defaultSelectedKeys()
    val groupChecked = duplicateKeys.isNotEmpty() && duplicateKeys.all { it in selectedKeys }
    val expandDesc = if (expanded) {
        stringResource(R.string.junk_collapse_group)
    } else {
        stringResource(R.string.junk_expand_group)
    }

    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = groupChecked, onCheckedChange = { onToggleGroup() })
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = group.titleName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(
                            R.string.duplicates_group_meta,
                            group.files.size,
                            formatBytes(group.totalBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.semantics { contentDescription = expandDesc },
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                    )
                }
            }
        }
        if (expanded) {
            group.files.forEach { file ->
                val isKeeper = file.selectionKey == keeperKey
                DuplicateFileRow(
                    file = file,
                    checked = file.selectionKey in selectedKeys,
                    isKeeper = isKeeper,
                    formatBytes = formatBytes,
                    onCheckedChange = { onToggleFile(file.selectionKey) },
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DuplicateFileRow(
    file: DuplicateFileItem,
    checked: Boolean,
    isKeeper: Boolean,
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
                text = buildString {
                    append(formatBytes(file.sizeBytes))
                    append(" · ")
                    append(file.locationLabel)
                    if (isKeeper) {
                        append(" · ")
                        append(stringResource(R.string.duplicates_keeper))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
