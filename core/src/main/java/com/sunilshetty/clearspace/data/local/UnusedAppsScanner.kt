package com.sunilshetty.clearspace.data.local

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import com.sunilshetty.clearspace.data.model.UnusedAppItem
import com.sunilshetty.clearspace.core.permissions.hasUsageStatsAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UnusedAppsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanUnusedApps(): List<UnusedAppItem> {
        if (!hasUsageStatsAccess(context)) return emptyList()

        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val start = now - TimeUnit.DAYS.toMillis(LOOKBACK_DAYS)
        val statsMap = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            start,
            now,
        ).associateBy { it.packageName }

        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
        val selfPackage = context.packageName
        val cutoff = now - TimeUnit.DAYS.toMillis(UNUSED_THRESHOLD_DAYS)

        return installed
            .asSequence()
            .filter { info -> isUserUninstallable(info, selfPackage) }
            .mapNotNull { info -> toUnusedItem(info, statsMap, cutoff, pm) }
            .sortedBy { it.lastUsedEpochMs }
            .take(MAX_RESULTS)
            .toList()
    }

    /** User apps that can be uninstalled when usage access is not granted yet. */
    fun scanUninstallableUserApps(): List<UnusedAppItem> {
        val pm = context.packageManager
        val selfPackage = context.packageName
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { info -> isUserUninstallable(info, selfPackage) }
            .map { info ->
                UnusedAppItem(
                    packageName = info.packageName,
                    appLabel = pm.getApplicationLabel(info).toString(),
                    lastUsedEpochMs = 0L,
                    isNeverUsed = false,
                    usageUnknown = true,
                )
            }
            .sortedBy { it.appLabel.lowercase() }
            .take(MAX_RESULTS)
            .toList()
    }

    private fun isUserUninstallable(info: ApplicationInfo, selfPackage: String): Boolean {
        if (info.packageName == selfPackage) return false
        if (info.packageName == "android") return false
        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystem || isUpdatedSystem
    }

    private fun toUnusedItem(
        info: ApplicationInfo,
        statsMap: Map<String, UsageStats>,
        cutoffMs: Long,
        pm: android.content.pm.PackageManager,
    ): UnusedAppItem? {
        val stat = statsMap[info.packageName]
        val lastUsed = stat?.lastTimeUsed ?: 0L
        val neverUsed = lastUsed <= 0L
        val unusedLongEnough = neverUsed || lastUsed < cutoffMs
        if (!unusedLongEnough) return null

        val label = pm.getApplicationLabel(info).toString()
        return UnusedAppItem(
            packageName = info.packageName,
            appLabel = label,
            lastUsedEpochMs = lastUsed,
            isNeverUsed = neverUsed,
        )
    }

    companion object {
        private const val LOOKBACK_DAYS = 90L
        private const val UNUSED_THRESHOLD_DAYS = 30L
        private const val MAX_RESULTS = 200
    }
}
