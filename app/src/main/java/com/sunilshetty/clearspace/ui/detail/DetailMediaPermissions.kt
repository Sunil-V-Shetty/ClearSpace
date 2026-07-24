package com.sunilshetty.clearspace.ui.detail

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

private object Api33Permissions {
    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
}

internal fun mediaReadPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Api33Permissions.READ_MEDIA_IMAGES,
            Api33Permissions.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

internal fun junkReadPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Api33Permissions.READ_MEDIA_IMAGES,
            Api33Permissions.READ_MEDIA_VIDEO,
            Api33Permissions.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

/** All runtime permissions requested once when the app opens. */
internal fun appRuntimePermissions(): Array<String> = junkReadPermissions()

internal fun hasAppRuntimePermissions(context: Context): Boolean {
    return hasAllPermissions(context, appRuntimePermissions())
}

internal fun hasMediaAccess(context: Context): Boolean {
    return hasAllPermissions(context, mediaReadPermissions())
}

internal fun hasJunkReadAccess(context: Context): Boolean {
    if (canScanJunkFilesystem(context)) return true
    return hasAllPermissions(context, junkReadPermissions())
}

internal fun canScanJunkFilesystem(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return Environment.isExternalStorageManager()
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    ) == PermissionChecker.PERMISSION_GRANTED
}

internal fun needsFullStorageAccessForJunkOnDevice(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
    if (permissions.isEmpty()) return true
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) ==
            PermissionChecker.PERMISSION_GRANTED
    }
}
