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

    val fullAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { /* user returned from system settings */ }

    fun launchFullStorageAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri(),
            )
            runCatching { fullAccessLauncher.launch(intent) }.onFailure {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()))
            }
        } else {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* Permission-dependent screens refresh when resumed. */ }

    fun requestRuntimeThenFullAccessIfNeeded() {
        when {
            runtimePermissions.isNotEmpty() && !hasAppRuntimePermissions(context) -> {
                permissionLauncher.launch(runtimePermissions)
            }
        }
    }

    val actions = AppPermissionActions(
        requestAllPermissions = ::requestRuntimeThenFullAccessIfNeeded,
        openFullStorageAccessSettings = ::launchFullStorageAccessSettings,
    )

    CompositionLocalProvider(LocalAppPermissionActions provides actions) {
        content()
    }
}
