package com.sunilshetty.clearspace.data.repository

import com.sunilshetty.clearspace.data.local.LargeFileScanner
import com.sunilshetty.clearspace.data.local.UnusedAppsScanner
import com.sunilshetty.clearspace.data.model.LargeFileItem
import com.sunilshetty.clearspace.data.model.UnusedAppItem
import javax.inject.Inject
import android.app.PendingIntent
import com.sunilshetty.clearspace.data.model.JunkFileItem

class UnusedAppsRepository @Inject constructor(
    private val unusedAppsScanner: UnusedAppsScanner,
) {
    fun loadUnusedApps(): List<UnusedAppItem> = unusedAppsScanner.scanUnusedApps()

    fun loadUninstallableUserApps(): List<UnusedAppItem> = unusedAppsScanner.scanUninstallableUserApps()
}

class LargeFilesRepository @Inject constructor(
    private val largeFileScanner: LargeFileScanner,
    private val junkFilesRepository: JunkFilesRepository,
) {
    fun loadLargeFiles(): List<LargeFileItem> = largeFileScanner.scanLargeFiles()

    fun planDelete(items: List<JunkFileItem>): JunkDeletePlan = junkFilesRepository.planDelete(items)

    fun createDeleteRequest(items: List<JunkFileItem>): PendingIntent? =
        junkFilesRepository.createDeleteRequest(items)

    fun deleteSelected(items: List<JunkFileItem>): Int = junkFilesRepository.deleteSelected(items)
}
