package com.sunilshetty.clearspace.ui.home

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.R as CoreR
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.model.StorageScanSnapshot
import com.sunilshetty.clearspace.navigation.rememberHomeViewModel
import com.sunilshetty.clearspace.ui.components.*
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.hasMediaAccess
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions
import com.sunilshetty.clearspace.ui.theme.ClearSpaceTheme

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
    mediaPermissions: Array<String>,
    onOpenSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    onRefreshStorage: () -> Unit,
    onRequestMediaAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatBytes = rememberStorageFormatBytes()
    val formatPercent = rememberStorageFormatPercent()
    BentoScreenBackground(modifier.fillMaxSize()) {
        ClearSpaceScreenHeader(R.string.home_screen_title, onOpenSettings, onRefreshStorage = onRefreshStorage)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberBentoScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Storage is context; the cleanup action below owns the visual emphasis.
            BentoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Storage, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Internal storage", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formatBytes(storage.freeBytes)} available", style = MaterialTheme.typography.titleLarge)
                    }
                }
                BentoStorageProgress(storage.usedPercent)
                Text("${formatPercent(storage.usedPercent)} used · ${formatBytes(storage.totalBytes)} total",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onNavigate(Constants.Navigation.STORAGE_USED) }, modifier = Modifier.weight(1f)) {
                        Text("Storage details")
                    }
                    TextButton(onClick = { onNavigate(Constants.Navigation.STORAGE_FREE) }, modifier = Modifier.weight(1f)) {
                        Text("Free space")
                    }
                }
            }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Free up space", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { onNavigate(Constants.Navigation.RECLAIMABLE) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                        Text("Review cleanup")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Outlined.ManageSearch, null, Modifier.size(20.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cleanup tools", style = MaterialTheme.typography.titleMedium)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClearSpaceActionRow("Junk files", "Temporary files", Icons.Outlined.CleaningServices,
                                { onNavigate(Constants.Navigation.JUNK_FILES) })
                            ClearSpaceActionRow("Duplicates", "Identical copies", Icons.Outlined.ContentCopy,
                                { onNavigate(Constants.Navigation.DUPLICATE_FILES) })
                            ClearSpaceActionRow("Large files", "Files over 5 MB", Icons.Outlined.Folder,
                                { onNavigate(Constants.Navigation.LARGE_FILES) })
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CleanupShortcut("Junk files", Icons.Outlined.CleaningServices,
                                { onNavigate(Constants.Navigation.JUNK_FILES) }, Modifier.weight(1f))
                            CleanupShortcut("Duplicates", Icons.Outlined.ContentCopy,
                                { onNavigate(Constants.Navigation.DUPLICATE_FILES) }, Modifier.weight(1f))
                            CleanupShortcut("Large files", Icons.Outlined.Folder,
                                { onNavigate(Constants.Navigation.LARGE_FILES) }, Modifier.weight(1f))
                        }
                    }
                }
            }
            ClearSpaceActionRow("Trash", "Restore or delete forever",
                Icons.Outlined.RestoreFromTrash, { onNavigate(Constants.Navigation.TRASH) })
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Files", style = MaterialTheme.typography.titleMedium)
                CategoryStorageRow(scanSnapshot, CoreR.string.category_photos, Icons.Outlined.Photo,
                    formatBytes, StorageCategory.PHOTOS, storage.totalBytes, onNavigate)
                CategoryStorageRow(scanSnapshot, CoreR.string.category_videos, Icons.Outlined.VideoLibrary,
                    formatBytes, StorageCategory.VIDEOS, storage.totalBytes, onNavigate)
                CategoryStorageRow(scanSnapshot, CoreR.string.category_downloads, Icons.Outlined.Download,
                    formatBytes, StorageCategory.DOWNLOADS, storage.totalBytes, onNavigate)
                if (scanSnapshot != null) {
                    Text("Scanned files · bars show a share of total storage",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (mediaPermissions.isNotEmpty() && !mediaAccessGranted) {
                    Text("Allow access to scan more media.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onRequestMediaAccess, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.media_access_request))
                    }
                }
                scanError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("More tools", style = MaterialTheme.typography.titleMedium)
                ClearSpaceActionRow("Quick Scan", "Storage, memory and media",
                    Icons.Outlined.Radar, { onNavigate(Constants.Navigation.SMART_SCAN) })
                ClearSpaceActionRow("Device diagnostics", "Readings and hardware checks",
                    Icons.Outlined.MonitorHeart, { onNavigate(Constants.Navigation.DIAGNOSTICS) })
                ClearSpaceActionRow("Media overview",
                    scanSnapshot?.let { "${formatBytes(it.categories.sumOf { c -> c.totalBytes })} in scanned media" }
                        ?: "Photos, videos and downloads",
                    Icons.Outlined.PermMedia, { onNavigate(Constants.Navigation.MEDIA) })
            }
            if (isScanning) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CleanupShortcut(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.heightIn(min = 112.dp), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CategoryStorageRow(
    scanSnapshot: StorageScanSnapshot?,
    categoryLabelRes: Int,
    icon: ImageVector,
    formatBytes: (Long) -> String,
    category: StorageCategory,
    storageCapacity: Long,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val breakdown = scanSnapshot?.categories?.firstOrNull { it.category == category }
    ClearSpaceStorageRow(
        title = stringResource(categoryLabelRes),
        amount = breakdown?.let { formatBytes(it.totalBytes) } ?: "View files",
        icon = icon,
        fraction = breakdown?.takeIf { storageCapacity > 0L }?.let { it.totalBytes.toFloat() / storageCapacity },
        onClick = { onNavigate(category.navigationRoute()) },
        modifier = modifier,
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
            mediaPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            onOpenSettings = {},
            onNavigate = {},
            onRefreshStorage = {},
            onRequestMediaAccess = {},
        )
    }
}
