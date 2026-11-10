package com.sunilshetty.clearspace.ui.trash

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
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sunilshetty.clearspace.data.model.TrashItem
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceCompleteCelebrationOverlay
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.ClearSpaceSecondaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpaceLoadingIndicator
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteCelebrationVisible by viewModel.deleteCelebrationVisible.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val formatBytes = rememberStorageFormatBytes()
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("Delete recovery copies forever?") },
            text = { Text("Selected files cannot be restored after this action.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteForeverSelected() }) { Text("Delete forever") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
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
                title = stringResource(R.string.trash_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? TrashUiState.Ready
            if (ready != null && ready.itemCount > 0) {
                Column(
                    modifier = Modifier.clearSpaceBottomBarPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClearSpacePrimaryButton(
                        onClick = viewModel::restoreSelected,
                        enabled = ready.selectedKeys.isNotEmpty() && !ready.isWorking,
                        label = stringResource(
                            R.string.trash_restore_selected,
                            ready.selectedCount,
                        ),
                        icon = Icons.Outlined.Restore,
                    )
                    ClearSpaceSecondaryButton(
                        onClick = { confirmDelete = true },
                        enabled = ready.selectedKeys.isNotEmpty() && !ready.isWorking,
                        label = stringResource(
                            R.string.trash_delete_forever_selected,
                            ready.selectedCount,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        BentoScreenBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                TrashUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClearSpaceLoadingIndicator(size = 96.dp)
                        Text(
                            text = stringResource(R.string.trash_loading),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is TrashUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadTrash(force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is TrashUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TrashHeader(
                                itemCount = state.itemCount,
                                selectedCount = state.selectedCount,
                                onSelectAll = viewModel::selectAll,
                                onClearSelection = viewModel::clearSelection,
                            )
                            if (state.message != null) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                            if (state.items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.trash_empty),
                                    modifier = Modifier.padding(16.dp),
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(state.items, key = { it.id }) { item ->
                                        TrashItemRow(
                                            item = item,
                                            checked = item.id in state.selectedKeys,
                                            formatBytes = formatBytes,
                                            dateFormat = dateFormat,
                                            onCheckedChange = { viewModel.toggleSelection(item.id) },
                                        )
                                    }
                                }
                            }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isWorking,
                            message = stringResource(R.string.trash_working),
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
private fun TrashHeader(
    itemCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.trash_subtitle))
        ClearSpaceSelectionSummaryRow(
            summary = stringResource(R.string.trash_summary, itemCount, selectedCount),
            totalCount = itemCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TrashItemRow(
    item: TrashItem,
    checked: Boolean,
    formatBytes: (Long) -> String,
    dateFormat: DateFormat,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectionLabel = stringResource(R.string.select_file, item.displayName)
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() },
            modifier = Modifier.semantics { contentDescription = selectionLabel })
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(
                    R.string.trash_item_meta,
                    formatBytes(item.sizeBytes),
                    dateFormat.format(Date(item.deletedAtEpochMs)),
                    item.daysLeft,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
