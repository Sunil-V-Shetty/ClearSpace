package com.sunilshetty.clearspace.data.model

enum class JunkFileCategory(
    val id: String,
    val autoSelectByDefault: Boolean,
    val sortOrder: Int,
) {
    SAFE_TO_CLEAN(
        id = "safe_to_clean",
        autoSelectByDefault = true,
        sortOrder = 0,
    ),
    THUMBNAIL_CACHE(
        id = "thumbnail_cache",
        autoSelectByDefault = true,
        sortOrder = 1,
    ),
    REVIEW_INSTALLERS(
        id = "review_installers",
        autoSelectByDefault = false,
        sortOrder = 2,
    ),
    REVIEW_BACKUPS(
        id = "review_backups",
        autoSelectByDefault = false,
        sortOrder = 3,
    ),
    REVIEW_OTHER(
        id = "review_other",
        autoSelectByDefault = false,
        sortOrder = 4,
    ),
}
