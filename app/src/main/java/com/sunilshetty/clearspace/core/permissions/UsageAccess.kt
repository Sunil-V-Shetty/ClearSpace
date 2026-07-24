package com.sunilshetty.clearspace.core.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.net.toUri

@Suppress("DEPRECATION")
fun hasUsageStatsAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun usageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

fun uninstallPackageIntent(packageName: String): Intent =
    Intent(Intent.ACTION_DELETE).apply {
        data = "package:$packageName".toUri()
    }
