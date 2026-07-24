package com.sunilshetty.clearspace.ui.permissions

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sunilshetty.clearspace.ui.detail.appRuntimePermissions
import com.sunilshetty.clearspace.ui.detail.canScanJunkFilesystem
import com.sunilshetty.clearspace.ui.detail.hasAppRuntimePermissions

data class AppPermissionActions(
    val requestAllPermissions: () -> Unit,
    val openFullStorageAccessSettings: () -> Unit,
)

val LocalAppPermissionActions = compositionLocalOf<AppPermissionActions?> { null }

@Composable
fun AppPermissionsHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val runtimePermissions = appRuntimePermissions()
    var autoFlowStarted by rememberSaveable { mutableStateOf(false) }

    val fullAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { /* user returned from system settings */ }

    fun launchFullStorageAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri(),
            )
            fullAccessLauncher.launch(intent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !canScanJunkFilesystem(context)) {
            launchFullStorageAccessSettings()
        }
    }

    fun requestRuntimeThenFullAccessIfNeeded() {
        when {
            runtimePermissions.isNotEmpty() && !hasAppRuntimePermissions(context) -> {
                permissionLauncher.launch(runtimePermissions)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !canScanJunkFilesystem(context) -> {
                launchFullStorageAccessSettings()
            }
        }
    }

    val actions = AppPermissionActions(
        requestAllPermissions = ::requestRuntimeThenFullAccessIfNeeded,
        openFullStorageAccessSettings = ::launchFullStorageAccessSettings,
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (autoFlowStarted) return@LaunchedEffect
        autoFlowStarted = true
        requestRuntimeThenFullAccessIfNeeded()
    }

    CompositionLocalProvider(LocalAppPermissionActions provides actions) {
        content()
    }
}
