package com.sunilshetty.clearspace.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.local.entity.TrashItemEntity
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.JunkFilesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class TrashFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun trashDirectory(): File {
        val dir = File(context.filesDir, TRASH_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun copyJunkItemToTrash(item: JunkFileItem, destFile: File): Boolean {
        return runCatching {
            when {
                item.absolutePath != null -> {
                    File(item.absolutePath).inputStream().use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                item.uri.scheme == "file" -> {
                    File(item.uri.path.orEmpty()).inputStream().use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                item.uri.scheme == "content" -> {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return false
                }
                else -> return false
            }
            destFile.exists() && destFile.length() > 0L
        }.getOrDefault(false)
    }

    fun resolveMimeType(item: JunkFileItem): String? {
        if (item.uri.scheme == "content") {
            context.contentResolver.getType(item.uri)?.let { return it }
        }
        val name = item.displayName.lowercase()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".mp4") -> "video/mp4"
            name.endsWith(".apk") -> "application/vnd.android.package-archive"
            else -> null
        }
    }

    fun mediaBucketFor(item: JunkFileItem): String {
        return JunkFilesRepository.createDeleteRequestBucket(item.uri) ?: BUCKET_OTHER
    }

    fun restoreToMediaStore(entity: TrashItemEntity): Boolean {
        val trashFile = File(trashDirectory(), entity.storedFileName)
        if (!trashFile.exists()) return false

        val collection = collectionUri(entity.mediaBucket)
        val mimeType = entity.mimeType ?: "application/octet-stream"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entity.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relative = entity.relativePath?.takeIf { it.isNotBlank() }
                    ?: defaultRelativePath(entity.mediaBucket)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            }
        }

        val targetUri = context.contentResolver.insert(collection, values) ?: return false
        val written = runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                trashFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            true
        }.getOrDefault(false)

        if (!written) {
            runCatching { context.contentResolver.delete(targetUri, null, null) }
            return false
        }
        return true
    }

    fun deleteStoredFile(storedFileName: String) {
        runCatching { File(trashDirectory(), storedFileName).delete() }
    }

    private fun collectionUri(mediaBucket: String): Uri {
        return when (mediaBucket) {
            JunkFilesRepository.BUCKET_IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            JunkFilesRepository.BUCKET_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            JunkFilesRepository.BUCKET_DOWNLOADS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Files.getContentUri("external")
                }
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Files.getContentUri("external")
                }
            }
        }
    }

    private fun defaultRelativePath(mediaBucket: String): String {
        return when (mediaBucket) {
            JunkFilesRepository.BUCKET_IMAGES -> "${android.os.Environment.DIRECTORY_PICTURES}/"
            JunkFilesRepository.BUCKET_VIDEO -> "${android.os.Environment.DIRECTORY_MOVIES}/"
            else -> "${android.os.Environment.DIRECTORY_DOWNLOADS}/"
        }
    }

    companion object {
        const val TRASH_DIR_NAME = "trash"
        const val BUCKET_OTHER = "other"
    }
}
