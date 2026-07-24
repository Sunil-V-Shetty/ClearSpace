package com.sunilshetty.clearspace.data.local

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.model.StorageCategoryBreakdown
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaStorageScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanCategories(): List<StorageCategoryBreakdown> {
        return listOf(
            scanMediaStoreCollection(
                category = StorageCategory.PHOTOS,
                collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ),
            scanMediaStoreCollection(
                category = StorageCategory.VIDEOS,
                collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ),
            scanDownloads(),
        )
    }

    private fun scanMediaStoreCollection(
        category: StorageCategory,
        collectionUri: Uri,
    ): StorageCategoryBreakdown {
        val sizeColumn = MediaStore.MediaColumns.SIZE
        val projection = arrayOf(sizeColumn)
        var itemCount = 0L
        var totalBytes = 0L

        context.contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(sizeColumn)
            if (sizeIndex < 0) return@use
            while (cursor.moveToNext()) {
                itemCount++
                totalBytes += cursor.getLong(sizeIndex).coerceAtLeast(0L)
            }
        }

        return StorageCategoryBreakdown(
            category = category,
            itemCount = itemCount,
            totalBytes = totalBytes,
        )
    }

    private fun scanDownloads(): StorageCategoryBreakdown {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            return scanMediaStoreCollection(
                category = StorageCategory.DOWNLOADS,
                collectionUri = collectionUri,
            )
        }

        return scanDownloadsDirectoryLegacy()
    }

    private fun scanDownloadsDirectoryLegacy(): StorageCategoryBreakdown {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var itemCount = 0L
        var totalBytes = 0L
        if (downloadsDir.exists()) {
            downloadsDir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile }
                .forEach { file ->
                    itemCount++
                    totalBytes += file.length().coerceAtLeast(0L)
                }
        }
        return StorageCategoryBreakdown(
            category = StorageCategory.DOWNLOADS,
            itemCount = itemCount,
            totalBytes = totalBytes,
        )
    }
}
