package com.sunilshetty.clearspace.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.model.DuplicateFileItem
import com.sunilshetty.clearspace.data.model.DuplicateGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject

class DuplicateScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanDuplicateGroups(): List<DuplicateGroup> {
        val candidates = mutableListOf<DuplicateFileItem>()
        scanCollection(
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            locationLabel = LABEL_PHOTOS,
            results = candidates,
        )
        scanCollection(
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            locationLabel = LABEL_VIDEOS,
            results = candidates,
        )
        scanDownloads(candidates)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanCollection(
                collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                locationLabel = LABEL_FILES,
                results = candidates,
                mediaTypeNoneOnly = true,
            )
        }

        if (candidates.isEmpty()) return emptyList()

        val bySize = candidates.groupBy { it.sizeBytes }
        val hashToFiles = linkedMapOf<String, MutableList<DuplicateFileItem>>()
        var hashedCount = 0

        bySize.values
            .filter { it.size > 1 }
            .forEach { sameSizeFiles ->
                sameSizeFiles.forEach { file ->
                    if (hashedCount >= MAX_HASH_OPERATIONS) return@forEach
                    hashedCount++
                    val hash = computeContentHash(file.uri, file.sizeBytes) ?: return@forEach
                    val key = "$hash|${file.sizeBytes}"
                    hashToFiles.getOrPut(key) { mutableListOf() }.add(
                        file.copy(contentHash = key),
                    )
                }
            }

        return hashToFiles.values
            .filter { it.size > 1 }
            .map { files ->
                val sorted = files.sortedByDescending { it.dateModifiedSec }
                DuplicateGroup(
                    groupKey = sorted.first().contentHash,
                    files = sorted,
                )
            }
            .sortedByDescending { it.totalBytes }
    }

    private fun scanDownloads(results: MutableList<DuplicateFileItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        scanCollection(collectionUri = uri, locationLabel = LABEL_DOWNLOADS, results = results)
    }

    private fun scanCollection(
        collectionUri: Uri,
        locationLabel: String,
        results: MutableList<DuplicateFileItem>,
        mediaTypeNoneOnly: Boolean = false,
    ) {
        if (results.size >= MAX_CANDIDATES) return

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
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

        context.contentResolver.query(
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
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            if (idIndex < 0 || nameIndex < 0) return@use

            while (cursor.moveToNext() && results.size < MAX_CANDIDATES) {
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                if (size <= 0L) continue

                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: continue
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                val modified = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L
                val itemUri = ContentUris.withAppendedId(collectionUri, id)

                results.add(
                    DuplicateFileItem(
                        id = id,
                        uri = itemUri,
                        displayName = name,
                        sizeBytes = size,
                        relativePath = path,
                        locationLabel = locationLabel,
                        dateModifiedSec = modified,
                        contentHash = "",
                    ),
                )
            }
        }
    }

    private fun computeContentHash(uri: Uri, sizeBytes: Long): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesToRead = minOf(sizeBytes, PARTIAL_HASH_BYTES)
                while (bytesToRead > 0) {
                    val read = input.read(
                        buffer,
                        0,
                        minOf(buffer.size.toLong(), bytesToRead).toInt(),
                    )
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    bytesToRead -= read
                }
                digest.digest().joinToString(separator = "") { byte ->
                    "%02x".format(byte)
                }
            }
        }.getOrNull()
    }

    companion object {
        private const val MAX_CANDIDATES = 6_000
        private const val MAX_HASH_OPERATIONS = 2_500
        private const val PARTIAL_HASH_BYTES = 512L * 1024L
        private const val BUFFER_SIZE = 8192

        private const val LABEL_PHOTOS = "Photos"
        private const val LABEL_VIDEOS = "Videos"
        private const val LABEL_DOWNLOADS = "Downloads"
        private const val LABEL_FILES = "Files"
    }
}
