package com.sunilshetty.clearspace.data.model

import androidx.annotation.StringRes
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.R

enum class StorageCategory(val id: String) {
    PHOTOS(Constants.StorageCategory.PHOTOS),
    VIDEOS(Constants.StorageCategory.VIDEOS),
    DOWNLOADS(Constants.StorageCategory.DOWNLOADS),
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        PHOTOS -> R.string.category_photos
        VIDEOS -> R.string.category_videos
        DOWNLOADS -> R.string.category_downloads
    }

    companion object {
        fun fromId(id: String): StorageCategory? = entries.find { it.id == id }

        fun fromNavigationRoute(route: String): StorageCategory? = when (route) {
            Constants.Navigation.CATEGORY_PHOTOS -> PHOTOS
            Constants.Navigation.CATEGORY_VIDEOS -> VIDEOS
            Constants.Navigation.CATEGORY_DOWNLOADS -> DOWNLOADS
            else -> null
        }
    }

    fun navigationRoute(): String = when (this) {
        PHOTOS -> Constants.Navigation.CATEGORY_PHOTOS
        VIDEOS -> Constants.Navigation.CATEGORY_VIDEOS
        DOWNLOADS -> Constants.Navigation.CATEGORY_DOWNLOADS
    }
}
