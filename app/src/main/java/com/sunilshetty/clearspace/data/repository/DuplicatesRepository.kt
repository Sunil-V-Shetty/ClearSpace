package com.sunilshetty.clearspace.data.repository

import android.app.PendingIntent
import com.sunilshetty.clearspace.data.local.DuplicateScanner
import com.sunilshetty.clearspace.data.model.DuplicateGroup
import com.sunilshetty.clearspace.data.model.JunkFileItem
import javax.inject.Inject

class DuplicatesRepository @Inject constructor(
    private val duplicateScanner: DuplicateScanner,
    private val junkFilesRepository: JunkFilesRepository,
) {

    fun scanDuplicateGroups(): List<DuplicateGroup> = duplicateScanner.scanDuplicateGroups()

    fun planDelete(items: List<JunkFileItem>): JunkDeletePlan = junkFilesRepository.planDelete(items)

    fun createDeleteRequest(items: List<JunkFileItem>): PendingIntent? =
        junkFilesRepository.createDeleteRequest(items)

    fun deleteSelected(items: List<JunkFileItem>): Int = junkFilesRepository.deleteSelected(items)
}
