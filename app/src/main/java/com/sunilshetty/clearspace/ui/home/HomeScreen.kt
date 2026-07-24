package com.sunilshetty.clearspace.ui.home

import android.Manifest
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import com.sunilshetty.clearspace.data.model.StorageCategoryBreakdown
import com.sunilshetty.clearspace.data.model.StorageScanSnapshot
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.components.BentoLabel
import com.sunilshetty.clearspace.ui.components.BentoQuickAction
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.BentoStorageProgress
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.BentoValue
import com.sunilshetty.clearspace.ui.components.ClearSpaceScreenHeader
import com.sunilshetty.clearspace.ui.components.bentoHorizontalPadding
import com.sunilshetty.clearspace.ui.components.rememberBentoScrollState
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatBytes
import com.sunilshetty.clearspace.ui.components.rememberStorageFormatPercent
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.hasMediaAccess
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions
import com.sunilshetty.clearspace.ui.theme.ClearSpaceTheme
import com.sunilshetty.clearspace.util.StorageFormatter

@Composable
fun HomeScreen(
    navController: NavHostController,
    onOpenSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberHomeViewModel(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mediaPermissions = appRuntimePermissions()
    val permissionActions = LocalAppPermissionActions.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateMediaAccessGranted(hasMediaAccess(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestMediaPermissions: () -> Unit = {
        permissionActions?.requestAllPermissions()
            ?: viewModel.updateMediaAccessGranted(hasMediaAccess(context))
    }

    DisposableEffect(Unit) {
        viewModel.updateMediaAccessGranted(hasMediaAccess(context))
        onDispose { }
    }

    when (val state = uiState) {
        HomeUiState.Loading -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            HomeMessageColumn(
                modifier = modifier.statusBarsPadding(),
                title = stringResource(R.string.error_storage_title),
                message = state.message,
                actionLabel = stringResource(R.string.action_try_again),
                onAction = viewModel::loadHome,
            )
        }

        is HomeUiState.Success -> {
            HomeContent(
                modifier = modifier,
                storage = state.storage,
                mediaAccessGranted = state.mediaAccessGranted,
                scanSnapshot = state.scanSnapshot,
                isScanning = state.isScanning,
                scanError = state.scanError,
                memorySnapshot = state.memorySnapshot,
                mediaPermissions = mediaPermissions,
                onOpenSettings = onOpenSettings,
                onNavigate = onNavigate,
                onRefreshStorage = viewModel::refreshStorage,
                onRequestMediaAccess = requestMediaPermissions,
            )
        }
    }
}

@Composable
private fun HomeContent(
    storage: DeviceStorageInfo,
    mediaAccessGranted: Boolean,
    scanSnapshot: StorageScanSnapshot?,
    isScanning: Boolean,
    scanError: String?,
    memorySnapshot: com.sunilshetty.clearspace.data.model.DeviceMemorySnapshot?,
    mediaPermissions: Array<String>,
    onOpenSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    onRefreshStorage: () -> Unit,
    onRequestMediaAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatBytes = rememberStorageFormatBytes()
    val formatPercent = rememberStorageFormatPercent()
    val mediaTotalBytes = scanSnapshot?.categories?.sumOf { it.totalBytes } ?: 0L

    BentoScreenBackground(
        modifier = modifier.fillMaxSize(),
    ) {
        ClearSpaceScreenHeader(
            titleRes = R.string.home_screen_title,
            onOpenSettings = onOpenSettings,
            onRefreshStorage = onRefreshStorage,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberBentoScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoCard(
                modifier = Modifier.bentoHorizontalPadding(),
                onClick = { onNavigate(Constants.Navigation.SMART_SCAN) },
            ) {
                BentoLabel(text = stringResource(R.string.home_bento_quick_actions))
                BentoStorageProgress(usedFraction = storage.usedPercent)
                BentoSubtleText(
                    text = stringResource(
                        R.string.storage_used_of_total,
                        formatBytes(storage.usedBytes),
                        formatBytes(storage.totalBytes),
                        formatPercent(storage.usedPercent),
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BentoQuickAction(
                        label = stringResource(R.string.quick_scan_action),
                        icon = Icons.Outlined.Radar,
                        onClick = { onNavigate(Constants.Navigation.SMART_SCAN) },
                        modifier = Modifier.weight(1f),
                        caption = when {
                            isScanning -> stringResource(R.string.scan_running)
                            memorySnapshot != null -> stringResource(
                                R.string.quick_scan_home_ram_caption,
                                formatPercent(memorySnapshot.usedFraction),
                            )
                            else -> null
                        },
                    )
                    CategoryQuickAction(
                        scanSnapshot = scanSnapshot,
                        categoryLabelRes = R.string.category_photos,
                        icon = Icons.Outlined.Photo,
                        formatBytes = formatBytes,
                        category = StorageCategory.PHOTOS,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                    CategoryQuickAction(
                        scanSnapshot = scanSnapshot,
                        categoryLabelRes = R.string.category_videos,
                        icon = Icons.Outlined.VideoLibrary,
                        formatBytes = formatBytes,
                        category = StorageCategory.VIDEOS,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                    CategoryQuickAction(
                        scanSnapshot = scanSnapshot,
                        categoryLabelRes = R.string.category_downloads,
                        icon = Icons.Outlined.Download,
                        formatBytes = formatBytes,
                        category = StorageCategory.DOWNLOADS,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (scanError != null) {
                    Text(
                        text = scanError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (mediaPermissions.isNotEmpty() && !mediaAccessGranted) {
                    TextButton(
                        onClick = onRequestMediaAccess,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.media_access_request))
                    }
                }
            }

            Row(
                modifier = Modifier.bentoHorizontalPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 148.dp),
                    onClick = { onNavigate(Constants.Navigation.STORAGE_FREE) },
                ) {
                    BentoLabel(text = stringResource(R.string.home_bento_label_free))
                    BentoValue(text = formatBytes(storage.freeBytes))
                    BentoSubtleText(text = stringResource(R.string.storage_on_device_note))
                }
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 148.dp),
                    onClick = { onNavigate(Constants.Navigation.STORAGE_USED) },
                ) {
                    BentoLabel(text = stringResource(R.string.home_bento_label_used))
                    BentoValue(text = formatBytes(storage.usedBytes))
                    BentoSubtleText(
                        text = formatPercent(storage.usedPercent) + " · " +
                            formatBytes(storage.totalBytes),
                    )
                }
            }

            Row(
                modifier = Modifier.bentoHorizontalPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BentoCard(
                    modifier = Modifier
                        .weight(0.58f)
                        .heightIn(min = 212.dp),
                    onClick = { onNavigate(Constants.Navigation.MEDIA) },
                ) {
                    BentoLabel(text = stringResource(R.string.home_bento_label_media))
                    if (scanSnapshot != null) {
                        BentoValue(text = formatBytes(mediaTotalBytes))
                        Spacer(modifier = Modifier.height(4.dp))
                        scanSnapshot.categories.forEach { breakdown ->
                            CategoryBreakdownLine(
                                breakdown = breakdown,
                                formatBytes = formatBytes,
                            )
                        }
                        BentoSubtleText(
                            text = stringResource(
                                R.string.scan_last_run,
                                StorageFormatter.formatTimestamp(scanSnapshot.scannedAtEpochMs),
                            ),
                        )
                    } else {
                        BentoValue(text = stringResource(R.string.home_bento_reclaim_short))
                        BentoSubtleText(text = stringResource(R.string.home_bento_scan_cta))
                    }
                }
                Column(
                    modifier = Modifier.weight(0.42f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        onClick = { onNavigate(Constants.Navigation.INSIGHTS) },
                    ) {
                        BentoLabel(text = stringResource(R.string.card_insights))
                        BentoValue(text = stringResource(R.string.home_bento_insights_short))
                        BentoSubtleText(
                            text = stringResource(R.string.card_insights_empty),
                        )
                    }
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        onClick = { onNavigate(Constants.Navigation.RECLAIMABLE) },
                    ) {
                        BentoLabel(text = stringResource(R.string.card_reclaimable))
                        BentoValue(text = stringResource(R.string.home_bento_reclaim_short))
                        BentoSubtleText(
                            text = stringResource(R.string.card_reclaimable_empty),
                        )
                    }
                }
            }

            if (isScanning) {
                Row(
                    modifier = Modifier.bentoHorizontalPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.scan_running),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryQuickAction(
    scanSnapshot: StorageScanSnapshot?,
    categoryLabelRes: Int,
    icon: ImageVector,
    formatBytes: (Long) -> String,
    category: StorageCategory,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val breakdown = scanSnapshot?.categories?.firstOrNull { it.category == category }
    BentoQuickAction(
        label = stringResource(categoryLabelRes),
        icon = icon,
        onClick = { onNavigate(category.navigationRoute()) },
        modifier = modifier,
        caption = breakdown?.let { formatBytes(it.totalBytes) },
    )
}

@Composable
private fun CategoryBreakdownLine(
    breakdown: StorageCategoryBreakdown,
    formatBytes: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(breakdown.category.labelRes()) + ": " + formatBytes(breakdown.totalBytes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HomeMessageColumn(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ClearSpaceTheme {
        HomeContent(
            storage = DeviceStorageInfo(
                totalBytes = 128_000_000_000L,
                usedBytes = 96_000_000_000L,
                freeBytes = 32_000_000_000L,
            ),
            mediaAccessGranted = true,
            scanSnapshot = null,
            isScanning = false,
            scanError = null,
            memorySnapshot = null,
            mediaPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            onOpenSettings = {},
            onNavigate = {},
            onRefreshStorage = {},
            onRequestMediaAccess = {},
        )
    }
}
