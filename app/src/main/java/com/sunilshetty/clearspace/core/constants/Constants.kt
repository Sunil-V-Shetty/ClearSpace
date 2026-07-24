package com.sunilshetty.clearspace.core.constants

object Constants {

    object Database {
        const val FILE_NAME = "clearspace.db"
        const val STORAGE_CATEGORY_CACHE_TABLE = "storage_category_cache"
        const val TRASH_ITEM_TABLE = "trash_item"
    }

    object Trash {
        const val RETENTION_DAYS = 30
    }

    object Preferences {
        const val DATA_STORE_NAME = "clearspace_user_prefs"
    }

    object Ui {
        const val DELETE_CELEBRATION_MS = 1_800L
    }

    object StorageCategory {
        const val PHOTOS = "photos"
        const val VIDEOS = "videos"
        const val DOWNLOADS = "downloads"
    }

    object Navigation {
        const val MAIN = "main"
        const val HOME = "home"
        const val TOOLS = "tools"
        const val SETTINGS = "settings"
        const val TRASH = "trash"
        const val SMART_SCAN = "smart_scan"
        const val STORAGE_FREE = "storage_free"
        const val STORAGE_USED = "storage_used"
        const val MEDIA = "media"
        const val INSIGHTS = "insights"
        const val RECLAIMABLE = "reclaimable"
        const val JUNK_FILES = "junk_files"
        const val DUPLICATE_FILES = "duplicate_files"
        const val UNUSED_APPS = "unused_apps"
        const val LARGE_FILES = "large_files"
        const val CATEGORY_PHOTOS = "category_photos"
        const val CATEGORY_VIDEOS = "category_videos"
        const val CATEGORY_DOWNLOADS = "category_downloads"

        val tabRoutes = setOf(HOME, TOOLS)
    }
}
