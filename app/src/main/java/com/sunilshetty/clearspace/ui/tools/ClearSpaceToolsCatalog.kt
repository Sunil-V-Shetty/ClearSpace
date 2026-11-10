package com.sunilshetty.clearspace.ui.tools

import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.constants.Constants

data class ClearSpaceToolEntry(
    val labelRes: Int,
    val route: String,
    val hintRes: Int,
)

/** Single catalog for the Tools tab — each destination appears here only. */
object ClearSpaceToolsCatalog {
    val cleanupTools: List<ClearSpaceToolEntry> = listOf(
        ClearSpaceToolEntry(R.string.diagnostics_title, Constants.Navigation.DIAGNOSTICS, R.string.diagnostics_hint),
        ClearSpaceToolEntry(R.string.card_insights, Constants.Navigation.INSIGHTS, R.string.card_insights_empty),
        ClearSpaceToolEntry(R.string.card_reclaimable, Constants.Navigation.RECLAIMABLE, R.string.card_reclaimable_empty),
        ClearSpaceToolEntry(
            labelRes = R.string.home_bento_junk_files,
            route = Constants.Navigation.JUNK_FILES,
            hintRes = R.string.home_bento_junk_hint,
        ),
        ClearSpaceToolEntry(
            labelRes = R.string.home_bento_duplicates,
            route = Constants.Navigation.DUPLICATE_FILES,
            hintRes = R.string.home_bento_duplicates_hint,
        ),
        ClearSpaceToolEntry(
            labelRes = R.string.home_bento_unused_apps,
            route = Constants.Navigation.UNUSED_APPS,
            hintRes = R.string.home_bento_unused_apps_hint,
        ),
        ClearSpaceToolEntry(
            labelRes = R.string.home_bento_large_files,
            route = Constants.Navigation.LARGE_FILES,
            hintRes = R.string.home_bento_large_files_hint,
        ),
        ClearSpaceToolEntry(
            labelRes = R.string.trash_title,
            route = Constants.Navigation.TRASH,
            hintRes = R.string.tools_trash_hint,
        ),
    )
}
