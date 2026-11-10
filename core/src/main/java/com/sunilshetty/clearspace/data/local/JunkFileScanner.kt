package com.sunilshetty.clearspace.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sunilshetty.clearspace.data.model.JunkFileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

class JunkFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scanJunkFiles(): List<JunkFileItem> {
        val results = linkedMapOf<String, JunkFileItem>()
        val mediaStoreLookup = HashMap<String, Uri>(512)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) scanMediaStoreDownloads(results, mediaStoreLookup)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanMediaStoreFiles(
                collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                results = results,
                mediaStoreLookup = mediaStoreLookup,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) scanMediaStoreThumbnails(results, mediaStoreLookup)

        if (canScanFilesystem()) {
            scanPublicStorageTree(results, mediaStoreLookup)
        }

        return results.values.sortedByDescending { it.sizeBytes }
    }

    private fun mediaStoreKey(relativePath: String?, displayName: String): String {
        return "${relativePath.orEmpty()}|$displayName"
    }

    private fun recordMediaStoreRow(
        lookup: HashMap<String, Uri>,
        relativePath: String?,
        displayName: String,
        uri: Uri,
    ) {
        lookup[mediaStoreKey(relativePath, displayName)] = uri
    }

    private fun scanMediaStoreDownloads(
        results: LinkedHashMap<String, JunkFileItem>,
        mediaStoreLookup: HashMap<String, Uri>,
    ) {
        scanMediaStoreCollection(
            collectionUri = downloadsCollectionUri(),
            results = results,
            locationLabel = LOCATION_DOWNLOADS,
            mediaStoreLookup = mediaStoreLookup,
        )
    }

    private fun scanMediaStoreFiles(
        collectionUri: Uri,
        results: LinkedHashMap<String, JunkFileItem>,
        mediaStoreLookup: HashMap<String, Uri>,
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
        ).filter { Build.VERSION.SDK_INT >= 29 || it != MediaStore.MediaColumns.RELATIVE_PATH }.toTypedArray()
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())

        context.contentResolver.queryVisible(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            ingestMediaStoreCursor(
                cursor = cursor,
                collectionUri = collectionUri,
                results = results,
                locationLabel = LOCATION_FILES,
                mediaStoreLookup = mediaStoreLookup,
            )
        }
    }

    private fun scanMediaStoreThumbnails(
        results: LinkedHashMap<String, JunkFileItem>,
        mediaStoreLookup: HashMap<String, Uri>,
    ) {
        val pathColumn = MediaStore.MediaColumns.RELATIVE_PATH
        val thumbSelection = buildString {
            append("($pathColumn LIKE ? OR $pathColumn LIKE ? OR ")
            append("${MediaStore.MediaColumns.DISPLAY_NAME} IN (?, ?, ?))")
        }
        val thumbArgs = arrayOf(
            "%/.thumbnails/%",
            "%/thumbnails/%",
            "thumbs.db",
            ".ds_store",
            "desktop.ini",
        )
        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to LOCATION_PHOTOS,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to LOCATION_VIDEOS,
        ).forEach { (collectionUri, label) ->
            scanMediaStoreCollection(
                collectionUri = collectionUri,
                results = results,
                locationLabel = label,
                selection = thumbSelection,
                selectionArgs = thumbArgs,
                mediaStoreLookup = mediaStoreLookup,
            )
        }
    }

    private fun scanMediaStoreCollection(
        collectionUri: Uri,
        results: LinkedHashMap<String, JunkFileItem>,
        locationLabel: String,
        mediaStoreLookup: HashMap<String, Uri>,
        pathMustLookLikeJunk: Boolean = false,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
        ).filter { Build.VERSION.SDK_INT >= 29 || it != MediaStore.MediaColumns.RELATIVE_PATH }.toTypedArray()
        context.contentResolver.queryVisible(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            ingestMediaStoreCursor(
                cursor = cursor,
                collectionUri = collectionUri,
                results = results,
                locationLabel = locationLabel,
                mediaStoreLookup = mediaStoreLookup,
                pathMustLookLikeJunk = pathMustLookLikeJunk,
            )
        }
    }

    private fun ingestMediaStoreCursor(
        cursor: android.database.Cursor,
        collectionUri: Uri,
        results: LinkedHashMap<String, JunkFileItem>,
        locationLabel: String,
        mediaStoreLookup: HashMap<String, Uri>,
        pathMustLookLikeJunk: Boolean = false,
    ) {
        val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
        val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        if (idIndex < 0 || nameIndex < 0) return

        while (cursor.moveToNext()) {
            val displayName = cursor.getString(nameIndex) ?: continue
            val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
            val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex).coerceAtLeast(0L) else 0L

            val id = cursor.getLong(idIndex)
            val itemUri = ContentUris.withAppendedId(collectionUri, id)
            recordMediaStoreRow(mediaStoreLookup, path, displayName, itemUri)

            val matches = when {
                pathMustLookLikeJunk -> isJunkPath(path) || isJunkFileName(displayName)
                else -> isJunkCandidate(displayName, path, size)
            }
            if (!matches) continue

            val key = itemUri.toString()
            if (results.containsKey(key)) continue

            results[key] = JunkFileItem(
                id = id,
                uri = itemUri,
                displayName = displayName,
                sizeBytes = size,
                relativePath = path,
                locationLabel = locationLabel,
            )
        }
    }

    private fun scanPublicStorageTree(
        results: LinkedHashMap<String, JunkFileItem>,
        mediaStoreLookup: Map<String, Uri>,
    ) {
        val storageRoot = Environment.getExternalStorageDirectory()
        val scanRoots = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        )

        var scannedFiles = 0
        scanRoots.forEach { root ->
            if (!root.exists()) return@forEach
            root.walkTopDown()
                .maxDepth(MAX_WALK_DEPTH)
                .forEach { file ->
                    if (!file.isFile) return@forEach
                    if (scannedFiles >= MAX_WALK_FILES) return@forEach
                    scannedFiles++

                    val relativePath = mediaStoreRelativePath(file, storageRoot) ?: return@forEach
                    if (!isJunkCandidate(file.name, relativePath, file.length())) return@forEach

                    val lookupKey = mediaStoreKey(relativePath, file.name)
                    val uri = mediaStoreLookup[lookupKey]
                    val key = uri?.toString() ?: file.absolutePath
                    if (results.containsKey(key)) return@forEach

                    results[key] = JunkFileItem(
                        id = uri?.let { ContentUris.parseId(it) } ?: key.hashCode().toLong(),
                        uri = uri ?: Uri.fromFile(file),
                        displayName = file.name,
                        sizeBytes = file.length().coerceAtLeast(0L),
                        relativePath = relativePath,
                        locationLabel = LOCATION_ON_DEVICE,
                        absolutePath = file.absolutePath,
                    )
                }
        }
    }

    private fun canScanFilesystem(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun downloadsCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }
    }

    private fun mediaStoreRelativePath(file: File, storageRoot: File): String? {
        val parent = file.parentFile?.absolutePath ?: return null
        val rootPath = storageRoot.absolutePath
        if (!parent.startsWith(rootPath)) return null
        val relativeDir = parent.removePrefix(rootPath).trimStart('/')
        return if (relativeDir.isEmpty()) {
            file.name
        } else {
            "$relativeDir/"
        }
    }

    private fun isJunkCandidate(name: String, relativePath: String?, sizeBytes: Long): Boolean {
        if (isJunkFileName(name)) return true
        if (isJunkPath(relativePath)) return true
        if (sizeBytes == 0L && (isJunkPath(relativePath) || hasJunkExtension(name))) return true
        if (isIncompleteDownload(name)) return true
        return false
    }

    private fun isIncompleteDownload(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.endsWith(".crdownload") ||
            lower.endsWith(".part") ||
            lower.endsWith(".download") ||
            lower.endsWith(".partial")
    }

    private fun isJunkPath(relativePath: String?): Boolean {
        if (relativePath.isNullOrBlank()) return false
        val normalized = relativePath.lowercase(Locale.US).replace('\\', '/')
        return JUNK_PATH_SEGMENTS.any { segment -> normalized.contains(segment) }
    }

    private fun isJunkFileName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        if (JUNK_EXACT_NAMES.contains(lower)) return true
        return hasJunkExtension(lower)
    }

    private fun hasJunkExtension(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return JUNK_EXTENSIONS.any { lower.endsWith(it) }
    }

    companion object {
        private const val MAX_WALK_DEPTH = 6
        private const val MAX_WALK_FILES = 1_500

        private const val LOCATION_DOWNLOADS = "Downloads"
        private const val LOCATION_FILES = "Files"
        private const val LOCATION_PHOTOS = "Photos"
        private const val LOCATION_VIDEOS = "Videos"
        private const val LOCATION_ON_DEVICE = "On device"

        private val JUNK_PATH_SEGMENTS = listOf(
            "/cache/",
            "/temp/",
            "/tmp/",
            "/.thumbnails/",
            "/thumbnails/",
            "/.trash/",
            "/trash/",
            "/logs/",
            "/log/",
        )

        private val JUNK_EXTENSIONS = listOf(
            ".tmp",
            ".temp",
            ".log",
            ".logs",
            ".cache",
            ".crdownload",
            ".part",
            ".download",
            ".partial",
            ".old",
            ".bak",
            ".thumbdata",
            ".gid",
            ".chk",
            ".dmp",
            ".trace",
            ".hprof",
            ".swp",
            ".db-journal",
            ".lck",
        )

        private val JUNK_EXACT_NAMES = setOf(
            ".ds_store",
            "thumbs.db",
            "desktop.ini",
        )
    }
}
