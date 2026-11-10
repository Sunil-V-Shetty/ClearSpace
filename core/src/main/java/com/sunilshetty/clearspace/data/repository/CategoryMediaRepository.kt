package com.sunilshetty.clearspace.data.repository

import android.app.PendingIntent
import com.sunilshetty.clearspace.data.local.CategoryMediaScanner
import com.sunilshetty.clearspace.data.model.CategoryMediaItem
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.model.StorageCategory
import javax.inject.Inject

class CategoryMediaRepository @Inject constructor(
    private val categoryMediaScanner: CategoryMediaScanner,
    private val junkFilesRepository: JunkFilesRepository,
) {
    fun loadCategoryMedia(category: StorageCategory): List<CategoryMediaItem> =
        categoryMediaScanner.scanCategory(category)

    fun planDelete(items: List<JunkFileItem>): JunkDeletePlan = junkFilesRepository.planDelete(items)

    fun createDeleteRequest(items: List<JunkFileItem>): PendingIntent? =
        junkFilesRepository.createDeleteRequest(items)

    fun deleteSelected(items: List<JunkFileItem>): Int = junkFilesRepository.deleteSelected(items)
}
