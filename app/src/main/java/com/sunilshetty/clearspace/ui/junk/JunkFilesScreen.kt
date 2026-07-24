package com.sunilshetty.clearspace.ui.junk

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
import com.sunilshetty.clearspace.data.model.JunkFileCategory
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.InfinityLoaderGif
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceCompleteCelebrationOverlay
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.hasJunkReadAccess
import com.sunilshetty.clearspace.ui.detail.needsFullStorageAccessForJunkOnDevice
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkFilesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JunkFilesViewModel = hiltViewModel(),
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

    DisposableEffect(lifecycleOwner, context) {
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
                title = stringResource(R.string.junk_files_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? JunkFilesUiState.Ready
            if (ready != null && ready.fileCount > 0) {
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
                        R.string.junk_clean_selected,
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
                JunkFilesUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InfinityLoaderGif(size = 128.dp)
                        Text(
                            text = stringResource(R.string.junk_scanning),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is JunkFilesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadJunkFiles(force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is JunkFilesUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        JunkFilesHeader(
                            fileCount = state.fileCount,
                            selectedCount = state.selectedCount,
                            limitedScan = state.limitedScan,
                            showAccessHint = runtimePermissions.isNotEmpty() && !hasJunkReadAccess(context),
                            onSelectAllSafe = viewModel::selectAllSafe,
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
                                text = if (state.limitedScan) {
                                    stringResource(R.string.junk_empty_limited)
                                } else {
                                    stringResource(R.string.junk_empty)
                                },
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                state.groups.forEach { group ->
                                    val expanded = group.category.id in state.expandedGroupIds
                                    item(key = "header-${group.category.id}") {
                                        JunkFileGroupHeader(
                                            group = group,
                                            groupChecked = group.files.isNotEmpty() &&
                                                group.files.all { it.selectionKey in state.selectedKeys },
                                            expanded = expanded,
                                            formatBytes = formatBytes,
                                            onToggleGroup = { viewModel.toggleGroup(group.category) },
                                            onToggleExpanded = {
                                                viewModel.toggleGroupExpanded(group.category.id)
                                            },
                                        )
                                    }
                                    if (expanded) {
                                        items(
                                            items = group.files,
                                            key = { it.selectionKey },
                                        ) { file ->
                                            JunkFileRow(
                                                file = file,
                                                checked = file.selectionKey in state.selectedKeys,
                                                formatBytes = formatBytes,
                                                onCheckedChange = {
                                                    viewModel.toggleSelection(file.selectionKey)
                                                },
                                                modifier = Modifier.padding(start = 48.dp, end = 12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isDeleting,
                            message = stringResource(R.string.junk_preparing_delete),
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
private fun JunkFilesHeader(
    fileCount: Int,
    selectedCount: Int,
    limitedScan: Boolean,
    showAccessHint: Boolean,
    onSelectAllSafe: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestAccess: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.junk_files_subtitle))
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
            summary = stringResource(R.string.junk_summary, fileCount, selectedCount),
            totalCount = fileCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSelectAllSafe) {
                Text(stringResource(R.string.junk_select_all_safe))
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
private fun JunkFileGroupHeader(
    group: JunkFileGroupUi,
    groupChecked: Boolean,
    expanded: Boolean,
    formatBytes: (Long) -> String,
    onToggleGroup: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val expandDesc = if (expanded) {
        stringResource(R.string.junk_collapse_group)
    } else {
        stringResource(R.string.junk_expand_group)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (group.category.autoSelectByDefault) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = groupChecked,
                onCheckedChange = { onToggleGroup() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = junkCategoryTitle(group.category),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = junkCategoryHint(group.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.junk_group_meta,
                        group.files.size,
                        formatBytes(group.totalBytes),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 2.dp),
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
}

@Composable
private fun junkCategoryTitle(category: JunkFileCategory): String {
    return when (category) {
        JunkFileCategory.SAFE_TO_CLEAN -> stringResource(R.string.junk_cat_safe_title)
        JunkFileCategory.THUMBNAIL_CACHE -> stringResource(R.string.junk_cat_thumbnails_title)
        JunkFileCategory.REVIEW_INSTALLERS -> stringResource(R.string.junk_cat_installers_title)
        JunkFileCategory.REVIEW_BACKUPS -> stringResource(R.string.junk_cat_backups_title)
        JunkFileCategory.REVIEW_OTHER -> stringResource(R.string.junk_cat_other_title)
    }
}

@Composable
private fun junkCategoryHint(category: JunkFileCategory): String {
    return when (category) {
        JunkFileCategory.SAFE_TO_CLEAN -> stringResource(R.string.junk_cat_safe_hint)
        JunkFileCategory.THUMBNAIL_CACHE -> stringResource(R.string.junk_cat_thumbnails_hint)
        JunkFileCategory.REVIEW_INSTALLERS -> stringResource(R.string.junk_cat_installers_hint)
        JunkFileCategory.REVIEW_BACKUPS -> stringResource(R.string.junk_cat_backups_hint)
        JunkFileCategory.REVIEW_OTHER -> stringResource(R.string.junk_cat_other_hint)
    }
}

@Composable
private fun JunkFileRow(
    file: JunkFileItem,
    checked: Boolean,
    formatBytes: (Long) -> String,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pathLine = buildString {
        append(file.locationLabel)
        val folder = file.relativePath?.trim().orEmpty()
        if (folder.isNotEmpty()) {
            append(" · ")
            append(folder)
        }
    }
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
                text = stringResource(
                    R.string.junk_file_meta,
                    formatBytes(file.sizeBytes),
                    pathLine,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
