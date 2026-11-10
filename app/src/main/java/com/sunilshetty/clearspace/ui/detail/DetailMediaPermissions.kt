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
internal fun appRuntimePermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> junkReadPermissions() + "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    Build.VERSION.SDK_INT <= 28 -> junkReadPermissions() + Manifest.permission.WRITE_EXTERNAL_STORAGE
    else -> junkReadPermissions()
}

internal fun hasAppRuntimePermissions(context: Context): Boolean {
    return hasAllPermissions(context, appRuntimePermissions())
}

internal fun hasMediaAccess(context: Context): Boolean {
    return canScanJunkFilesystem(context) || mediaReadPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
    } || (Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(context,
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PermissionChecker.PERMISSION_GRANTED)
}

internal fun hasJunkReadAccess(context: Context): Boolean {
    if (canScanJunkFilesystem(context)) return true
    return hasMediaAccess(context) || (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Api33Permissions.READ_MEDIA_AUDIO) == PermissionChecker.PERMISSION_GRANTED)
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
