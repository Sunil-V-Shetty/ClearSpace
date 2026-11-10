package com.sunilshetty.clearspace.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.model.LargeFileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LargeFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanLargeFiles(): List<LargeFileItem> {
        val results = ArrayList<LargeFileItem>(512)
        addFromCollection(
            results = results,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            locationLabel = LABEL_PHOTOS,
        )
        addFromCollection(
            results = results,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            locationLabel = LABEL_VIDEOS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val downloadsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            addFromCollection(
                results = results,
                collectionUri = downloadsUri,
                locationLabel = LABEL_DOWNLOADS,
            )
            addFromCollection(
                results = results,
                collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                locationLabel = LABEL_FILES,
                mediaTypeNoneOnly = true,
            )
        }
        return results
            .distinctBy { it.id }
            .filter { it.sizeBytes >= MIN_SIZE_BYTES }
            .sortedByDescending { it.sizeBytes }
            .take(MAX_RESULTS)
    }

    private fun addFromCollection(
        results: MutableList<LargeFileItem>,
        collectionUri: Uri,
        locationLabel: String,
        mediaTypeNoneOnly: Boolean = false,
    ) {
        if (results.size >= MAX_CANDIDATES) return

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
        ).filter { Build.VERSION.SDK_INT >= 29 || it != MediaStore.MediaColumns.RELATIVE_PATH }.toTypedArray()
        val selection = if (mediaTypeNoneOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        } else {
            null
        }
        val selectionArgs = if (selection != null) {
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())
        } else {
            null
        }

        context.contentResolver.queryVisible(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            if (idIndex < 0 || nameIndex < 0) return@use

            while (cursor.moveToNext() && results.size < MAX_CANDIDATES) {
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                if (size < MIN_SIZE_BYTES) continue
                val name = cursor.getString(nameIndex) ?: continue
                val id = cursor.getLong(idIndex)
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                val uri = ContentUris.withAppendedId(collectionUri, id)
                results.add(
                    LargeFileItem(
                        id = id,
                        uri = uri,
                        displayName = name,
                        sizeBytes = size,
                        relativePath = path,
                        locationLabel = locationLabel,
                    ),
                )
            }
        }
    }

    companion object {
        private const val MIN_SIZE_BYTES = 5L * 1024L * 1024L
        private const val MAX_CANDIDATES = 4_000
        private const val MAX_RESULTS = 500

        private const val LABEL_PHOTOS = "Photos"
        private const val LABEL_VIDEOS = "Videos"
        private const val LABEL_DOWNLOADS = "Downloads"
        private const val LABEL_FILES = "Files"
    }
}
