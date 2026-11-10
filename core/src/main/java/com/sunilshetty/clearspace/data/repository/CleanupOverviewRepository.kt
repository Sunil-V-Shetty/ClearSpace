package com.sunilshetty.clearspace.data.repository

import android.os.Environment
import android.os.Build
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sunilshetty.clearspace.data.local.DuplicateScanner
import com.sunilshetty.clearspace.data.local.LargeFileScanner
import com.sunilshetty.clearspace.data.model.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupOverviewRepository @Inject constructor(
    private val duplicates: DuplicateScanner,
    private val large: LargeFileScanner,
    private val junk: JunkFilesRepository,
    @ApplicationContext private val context: Context,
) {
    suspend fun scan(): CleanupOverview = withContext(Dispatchers.IO) {
        val groups = duplicates.scanDuplicateGroups()
        val duplicateItems = groups.flatMap { group -> group.files.filter { it.selectionKey != group.keeperKey }.map { it.toJunkFileItem() } }
        val largeItems = large.scanLargeFiles().map { it.toJunkFileItem() }
        val junkItems = junk.loadJunkFiles().filter { it.category.autoSelectByDefault }
        val sections = linkedMapOf("duplicates" to duplicateItems, "large" to largeItems, "junk" to junkItems)
        CleanupOverview(
            sections.map { (kind, files) -> CleanupOpportunity(kind, files.size, uniqueCandidateBytes(files.map(::candidate))) },
            uniqueCandidateBytes(sections.values.flatten().map(::candidate)), System.currentTimeMillis(),
        )
    }

    private fun candidate(item: JunkFileItem): CleanupCandidate {
        val relative = item.relativePath?.trim('/')?.let { "$it/${item.displayName}" }
        val absolute = item.absolutePath ?: item.uri.takeIf { it.scheme == "file" }?.path
        @Suppress("DEPRECATION")
        val root = Environment.getExternalStorageDirectory().absolutePath + "/"
        val volume = if (item.uri.scheme == "content" && Build.VERSION.SDK_INT >= 29) {
            runCatching {
                context.contentResolver.query(item.uri, arrayOf(MediaStore.MediaColumns.VOLUME_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull() ?: "external_primary"
        } else if (absolute != null && !absolute.startsWith(root)) {
            absolute.removePrefix("/storage/").substringBefore('/').lowercase()
        } else "external_primary"
        val key = "$volume:${relative ?: absolute?.removePrefix(root) ?: "media:${item.id}"}"
        return CleanupCandidate(key, item.sizeBytes)
    }
}
