package com.sunilshetty.clearspace.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.sunilshetty.clearspace.data.model.CategoryMediaItem
import com.sunilshetty.clearspace.data.model.StorageCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CategoryMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanCategory(category: StorageCategory): List<CategoryMediaItem> {
        if (category == StorageCategory.DOWNLOADS && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return scanDownloadsLegacy()
                .sortedByDescending { it.sizeBytes }
                .take(MAX_RESULTS)
        }
        val collectionUri = collectionUriFor(category) ?: return emptyList()
        return queryCollection(collectionUri)
            .sortedByDescending { it.sizeBytes }
            .take(MAX_RESULTS)
    }

    private fun collectionUriFor(category: StorageCategory): Uri? = when (category) {
        StorageCategory.PHOTOS -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        StorageCategory.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        StorageCategory.DOWNLOADS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
            } else {
                null
            }
        }
    }

    private fun queryCollection(collectionUri: Uri): List<CategoryMediaItem> {
        val results = ArrayList<CategoryMediaItem>(256)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )

        context.contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.SIZE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            if (idIndex < 0 || nameIndex < 0) return@use

            while (cursor.moveToNext() && results.size < MAX_CANDIDATES) {
                val name = cursor.getString(nameIndex) ?: continue
                val id = cursor.getLong(idIndex)
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex).coerceAtLeast(0L) else 0L
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                val dateSec = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L
                val uri = ContentUris.withAppendedId(collectionUri, id)
                results.add(
                    CategoryMediaItem(
                        id = id,
                        uri = uri,
                        displayName = name,
                        sizeBytes = size,
                        relativePath = path,
                        dateModifiedEpochMs = dateSec * 1000L,
                    ),
                )
            }
        }
        return results
    }

    private fun scanDownloadsLegacy(): List<CategoryMediaItem> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) return emptyList()
        val results = ArrayList<CategoryMediaItem>(128)
        downloadsDir.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile }
            .forEach { file ->
                if (results.size >= MAX_CANDIDATES) return@forEach
                results.add(
                    CategoryMediaItem(
                        id = file.absolutePath.hashCode().toLong(),
                        uri = file.toUri(),
                        displayName = file.name,
                        sizeBytes = file.length().coerceAtLeast(0L),
                        relativePath = file.parentFile?.name,
                        dateModifiedEpochMs = file.lastModified(),
                    ),
                )
            }
        return results
    }

    companion object {
        private const val MAX_CANDIDATES = 5_000
        private const val MAX_RESULTS = 1_000
    }
}
