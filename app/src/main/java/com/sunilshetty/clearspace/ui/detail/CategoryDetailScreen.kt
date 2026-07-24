package com.sunilshetty.clearspace.ui.detail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.CategoryMediaItem
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceCompleteCelebrationOverlay
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.InfinityLoaderGif
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: StorageCategory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryDetailViewModel = hiltViewModel(key = category.id),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteCelebrationVisible by viewModel.deleteCelebrationVisible.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val formatBytes = rememberStorageFormatBytes()
    val dateFormat = androidx.compose.runtime.remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val runtimePermissions = appRuntimePermissions()
    val permissionActions = LocalAppPermissionActions.current

    LaunchedEffect(category) {
        viewModel.loadCategory(category, force = true)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteFinished(
            category = category,
            success = result.resultCode == Activity.RESULT_OK,
        )
    }

    DisposableEffect(lifecycleOwner, category) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed(category)
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
                title = stringResource(category.labelRes()),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? CategoryDetailUiState.Ready
            if (ready != null && ready.itemCount > 0 && !ready.needsStorageAccess) {
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
                        R.string.category_detail_delete_selected,
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
                CategoryDetailUiState.Idle,
                CategoryDetailUiState.Loading,
                -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InfinityLoaderGif(size = 128.dp)
                        Text(
                            text = stringResource(R.string.category_detail_loading),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is CategoryDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadCategory(category, force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is CategoryDetailUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        CategoryDetailHeader(
                            itemCount = state.itemCount,
                            totalBytes = state.totalBytes,
                            selectedCount = state.selectedCount,
                            limitedScan = viewModel.needsLimitedScanHint(),
                            showAccessHint = state.needsStorageAccess ||
                                (runtimePermissions.isNotEmpty() && !hasJunkReadAccess(context)),
                            formatBytes = formatBytes,
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
                        if (state.needsStorageAccess) {
                            Text(
                                text = stringResource(R.string.media_access_request),
                                modifier = Modifier.padding(16.dp),
                            )
                        } else if (state.items.isEmpty()) {
                            Text(
                                text = stringResource(R.string.category_detail_empty),
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 8.dp,
                                    bottom = 16.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(state.items, key = { it.selectionKey }) { item ->
                                    CategoryMediaRow(
                                        item = item,
                                        category = category,
                                        checked = item.selectionKey in state.selectedKeys,
                                        formatBytes = formatBytes,
                                        dateFormat = dateFormat,
                                        onCheckedChange = { viewModel.toggleSelection(item.selectionKey) },
                                    )
                                }
                                if (state.itemCount >= 1_000) {
                                    item(key = "list_cap_hint") {
                                        BentoSubtleText(
                                            text = stringResource(R.string.category_detail_list_cap),
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }
                            }
                        }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isDeleting,
                            message = stringResource(R.string.category_detail_preparing_delete),
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
private fun CategoryDetailHeader(
    itemCount: Int,
    totalBytes: Long,
    selectedCount: Int,
    limitedScan: Boolean,
    showAccessHint: Boolean,
    formatBytes: (Long) -> String,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestAccess: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.category_detail_subtitle))
        Text(
            text = stringResource(
                R.string.category_summary,
                itemCount,
                formatBytes(totalBytes),
            ),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
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
            summary = stringResource(R.string.category_detail_selected_line, selectedCount),
            totalCount = itemCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (showAccessHint) {
            TextButton(onClick = onRequestAccess) {
                Text(stringResource(R.string.media_access_request))
            }
        }
    }
}

@Composable
private fun CategoryMediaRow(
    item: CategoryMediaItem,
    category: StorageCategory,
    checked: Boolean,
    formatBytes: (Long) -> String,
    dateFormat: DateFormat,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showThumbnail = category == StorageCategory.PHOTOS || category == StorageCategory.VIDEOS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        Surface(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (showThumbnail) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Text(
                text = buildString {
                    append(formatBytes(item.sizeBytes))
                    if (item.dateModifiedEpochMs > 0L) {
                        append(" · ")
                        append(dateFormat.format(Date(item.dateModifiedEpochMs)))
                    }
                    item.relativePath?.let { path ->
                        append(" · ")
                        append(path.trimEnd('/').substringAfterLast('/'))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
