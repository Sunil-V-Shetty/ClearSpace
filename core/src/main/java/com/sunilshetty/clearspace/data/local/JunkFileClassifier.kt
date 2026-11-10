package com.sunilshetty.clearspace.data.local

import com.sunilshetty.clearspace.data.model.JunkFileCategory
import com.sunilshetty.clearspace.data.model.JunkFileItem
import java.util.Locale
import javax.inject.Inject

class JunkFileClassifier @Inject constructor() {

    fun classify(item: JunkFileItem): JunkFileCategory {
        val name = item.displayName.lowercase(Locale.US)
        val path = item.relativePath?.lowercase(Locale.US)?.replace('\\', '/') ?: ""

        if (isIncompleteDownload(name)) return JunkFileCategory.SAFE_TO_CLEAN
        if (isThumbnailJunk(name, path)) return JunkFileCategory.THUMBNAIL_CACHE
        if (isSafeCacheOrTemp(name, path)) return JunkFileCategory.SAFE_TO_CLEAN
        if (isInstaller(name, path)) return JunkFileCategory.REVIEW_INSTALLERS
        if (isBackupOrOld(name)) return JunkFileCategory.REVIEW_BACKUPS
        return JunkFileCategory.REVIEW_OTHER
    }

    private fun isIncompleteDownload(name: String): Boolean {
        return name.endsWith(".crdownload") ||
            name.endsWith(".part") ||
            name.endsWith(".download") ||
            name.endsWith(".partial")
    }

    private fun isThumbnailJunk(name: String, path: String): Boolean {
        if (path.contains("/.thumbnails/") || path.contains("/thumbnails/")) return true
        return name.endsWith(".thumbdata") ||
            name == "thumbs.db" ||
            name == ".ds_store"
    }

    private fun isSafeCacheOrTemp(name: String, path: String): Boolean {
        if (isCacheOrTempPath(path)) return true
        if (SAFE_EXTENSIONS.any { name.endsWith(it) }) return true
        return false
    }

    private fun isCacheOrTempPath(path: String): Boolean {
        return CACHE_PATH_HINTS.any { hint -> path.contains(hint) }
    }

    private fun isInstaller(name: String, path: String): Boolean {
        if (!name.endsWith(".apk") && !name.endsWith(".apks") && !name.endsWith(".xapk")) {
            return false
        }
        return path.contains("download") || path.isEmpty()
    }

    private fun isBackupOrOld(name: String): Boolean {
        return name.endsWith(".bak") ||
            name.endsWith(".old") ||
            name.endsWith(".backup") ||
            name.endsWith("~")
    }

    companion object {
        private val CACHE_PATH_HINTS = listOf(
            "/cache/",
            "/temp/",
            "/tmp/",
            "/logs/",
            "/log/",
            "/.trash/",
            "/trash/",
        )

        private val SAFE_EXTENSIONS = listOf(
            ".tmp",
            ".temp",
            ".log",
            ".logs",
            ".cache",
            ".chk",
            ".dmp",
            ".trace",
            ".swp",
            ".db-journal",
            ".lck",
        )
    }
}
