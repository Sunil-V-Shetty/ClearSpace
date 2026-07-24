package com.sunilshetty.clearspace.data.repository

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.local.JunkFileClassifier
import com.sunilshetty.clearspace.data.local.JunkFileScanner
import com.sunilshetty.clearspace.data.model.JunkFileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class JunkFilesRepository @Inject constructor(
    private val junkFileScanner: JunkFileScanner,
    private val junkFileClassifier: JunkFileClassifier,
    private val trashRepository: TrashRepository,
    @ApplicationContext private val context: Context,
) {

    fun loadJunkFiles(): List<JunkFileItem> {
        return junkFileScanner.scanJunkFiles().map { item ->
            item.copy(category = junkFileClassifier.classify(item))
        }
    }

    fun deleteSelected(items: List<JunkFileItem>): Int {
        if (items.isEmpty()) return 0
        trashRepository.archiveFromJunkItems(items)
        var deleted = 0
        val canDeleteFilesDirectly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()

        items.chunked(DIRECT_DELETE_CHUNK_SIZE).forEach { chunk ->
            chunk.forEach { item ->
                val removed = when {
                    item.absolutePath != null && canDeleteFilesDirectly -> {
                        if (File(item.absolutePath).delete()) 1 else 0
                    }
                    item.uri.scheme == "file" && canDeleteFilesDirectly -> {
                        if (File(item.uri.path.orEmpty()).delete()) 1 else 0
                    }
                    item.uri.scheme == "content" -> {
                        runCatching { context.contentResolver.delete(item.uri, null, null) }
                            .getOrDefault(0)
                    }
                    else -> 0
                }
                if (removed > 0) deleted++
            }
        }
        return deleted
    }

    /**
     * Builds delete plan: [MediaStore.createDeleteRequest] requires a homogeneous batch
     * (e.g. all images, or all downloads — not mixed).
     */
    fun planDelete(items: List<JunkFileItem>): JunkDeletePlan {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return JunkDeletePlan(systemPromptBatches = emptyList(), directDelete = items)
        }

        val systemBatches = linkedMapOf<String, MutableList<JunkFileItem>>()
        val directDelete = mutableListOf<JunkFileItem>()

        items.forEach { item ->
            val bucket = createDeleteRequestBucket(item.uri)
            if (bucket != null) {
                systemBatches.getOrPut(bucket) { mutableListOf() }.add(item)
            } else {
                directDelete.add(item)
            }
        }

        return JunkDeletePlan(
            systemPromptBatches = systemBatches.values
                .flatMap { bucketItems -> bucketItems.chunked(MAX_SYSTEM_DELETE_BATCH_SIZE) }
                .filter { it.isNotEmpty() },
            directDelete = directDelete,
        )
    }

    fun createDeleteRequest(items: List<JunkFileItem>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || items.isEmpty()) return null
        val uris = items.map { it.uri }
        if (uris.any { createDeleteRequestBucket(it) == null }) return null
        val bucket = createDeleteRequestBucket(uris.first()) ?: return null
        if (uris.any { createDeleteRequestBucket(it) != bucket }) return null

        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        }.getOrNull()
    }

    companion object {
        /**
         * MediaStore batch delete supports one collection type per request.
         * Paths like …/external/images/media/…, …/downloads/…, etc.
         */
        internal fun createDeleteRequestBucket(uri: Uri): String? {
            if (uri.scheme != "content") return null
            val path = uri.path?.lowercase().orEmpty()
            return when {
                path.contains("/images/") -> BUCKET_IMAGES
                path.contains("/video/") -> BUCKET_VIDEO
                path.contains("/audio/") -> BUCKET_AUDIO
                path.contains("/downloads/") -> BUCKET_DOWNLOADS
                else -> null
            }
        }

        internal const val BUCKET_IMAGES = "images"
        internal const val BUCKET_VIDEO = "video"
        internal const val BUCKET_AUDIO = "audio"
        internal const val BUCKET_DOWNLOADS = "downloads"

        /** Android slows or fails with huge [MediaStore.createDeleteRequest] lists. */
        const val MAX_SYSTEM_DELETE_BATCH_SIZE = 64
        private const val DIRECT_DELETE_CHUNK_SIZE = 50
    }
}

data class JunkDeletePlan(
    val systemPromptBatches: List<List<JunkFileItem>>,
    val directDelete: List<JunkFileItem>,
)
