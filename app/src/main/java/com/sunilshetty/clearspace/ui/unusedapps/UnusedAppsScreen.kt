package com.sunilshetty.clearspace.ui.unusedapps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.permissions.usageAccessSettingsIntent
import com.sunilshetty.clearspace.data.model.UnusedAppItem
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.ClearSpacePrimaryButton
import com.sunilshetty.clearspace.ui.components.ClearSpaceSelectionSummaryRow
import com.sunilshetty.clearspace.ui.components.clearSpaceBottomBarPadding
import com.sunilshetty.clearspace.ui.components.ClearSpaceWorkingOverlay
import com.sunilshetty.clearspace.ui.components.InfinityLoaderGif
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnusedAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnusedAppsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onUninstallActivityReturned()
    }

    val usageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.loadUnusedApps(force = true)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ClearSpaceTopAppBar(
                title = stringResource(R.string.unused_apps_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            val ready = uiState as? UnusedAppsUiState.Ready
            if (ready != null && ready.appCount > 0) {
                ClearSpacePrimaryButton(
                    onClick = {
                        viewModel.uninstallSelected { intent ->
                            uninstallLauncher.launch(intent)
                        }
                    },
                    enabled = ready.selectedKeys.isNotEmpty() && !ready.isUninstalling,
                    label = stringResource(
                        R.string.unused_apps_uninstall_selected,
                        ready.selectedCount,
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
                UnusedAppsUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InfinityLoaderGif(size = 128.dp)
                        Text(
                            text = stringResource(R.string.unused_apps_scanning),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                is UnusedAppsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message)
                        Button(onClick = { viewModel.loadUnusedApps(force = true) }) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                }

                is UnusedAppsUiState.Ready -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        UnusedAppsHeader(
                            appCount = state.appCount,
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
                        if (state.needsUsageAccess) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.unused_apps_usage_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    usageAccessLauncher.launch(usageAccessSettingsIntent())
                                }) {
                                    Text(stringResource(R.string.unused_apps_open_usage_access))
                                }
                            }
                        }
                        if (state.apps.isEmpty()) {
                            Text(
                                text = stringResource(R.string.unused_apps_empty),
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = true),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(state.apps, key = { it.packageName }) { app ->
                                    UnusedAppRow(
                                        app = app,
                                        checked = app.packageName in state.selectedKeys,
                                        dateFormat = dateFormat,
                                        onCheckedChange = { viewModel.toggleSelection(app.packageName) },
                                    )
                                }
                            }
                        }
                        }
                        ClearSpaceWorkingOverlay(
                            visible = state.isUninstalling,
                            message = stringResource(R.string.unused_apps_uninstalling),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnusedAppsHeader(
    appCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BentoSubtleText(text = stringResource(R.string.unused_apps_subtitle))
        ClearSpaceSelectionSummaryRow(
            summary = stringResource(R.string.unused_apps_summary, appCount, selectedCount),
            totalCount = appCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun UnusedAppRow(
    app: UnusedAppItem,
    checked: Boolean,
    dateFormat: DateFormat,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconBitmap = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.appLabel, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when {
                    app.usageUnknown -> stringResource(R.string.unused_apps_usage_unknown)
                    app.isNeverUsed -> stringResource(R.string.unused_apps_never_used)
                    else -> stringResource(
                        R.string.unused_apps_last_used,
                        dateFormat.format(Date(app.lastUsedEpochMs)),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
