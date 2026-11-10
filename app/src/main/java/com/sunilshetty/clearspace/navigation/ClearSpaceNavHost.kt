package com.sunilshetty.clearspace.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.ui.detail.CategoryDetailScreen
import com.sunilshetty.clearspace.ui.detail.InsightsDetailScreen
import com.sunilshetty.clearspace.ui.detail.MediaDetailScreen
import com.sunilshetty.clearspace.ui.detail.ReclaimableDetailScreen
import com.sunilshetty.clearspace.ui.detail.SmartScanDetailScreen
import com.sunilshetty.clearspace.ui.detail.StorageFreeDetailScreen
import com.sunilshetty.clearspace.ui.detail.StorageUsedDetailScreen
import com.sunilshetty.clearspace.ui.junk.JunkFilesScreen
import com.sunilshetty.clearspace.ui.duplicates.DuplicatesScreen
import com.sunilshetty.clearspace.ui.largefiles.LargeFilesScreen
import com.sunilshetty.clearspace.ui.unusedapps.UnusedAppsScreen
import com.sunilshetty.clearspace.ui.settings.SettingsScreen
import com.sunilshetty.clearspace.ui.trash.TrashScreen

private fun NavGraphBuilder.categoryDetailRoute(
    navController: NavHostController,
    route: String,
) {
    detailComposable(route) {
        val category = StorageCategory.fromNavigationRoute(route) ?: return@detailComposable
        CategoryDetailScreen(
            category = category,
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
fun ClearSpaceNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Constants.Navigation.MAIN,
        modifier = modifier,
    ) {
        composable(
            route = Constants.Navigation.MAIN,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = mainTabPopEnter,
            popExitTransition = { ExitTransition.None },
        ) {
            MainTabShell(
                rootNavController = navController,
                onOpenSettings = {
                    navController.navigate(Constants.Navigation.SETTINGS) {
                        launchSingleTop = true
                    }
                },
            )
        }

        detailComposable(Constants.Navigation.SMART_SCAN) {
            SmartScanDetailScreen(
                onBack = { navController.popBackStack() },
                navController = navController,
            )
        }
        detailComposable(Constants.Navigation.STORAGE_FREE) {
            StorageFreeDetailScreen(
                onBack = { navController.popBackStack() },
                navController = navController,
            )
        }
        detailComposable(Constants.Navigation.STORAGE_USED) {
            StorageUsedDetailScreen(
                onBack = { navController.popBackStack() },
                navController = navController,
            )
        }
        detailComposable(Constants.Navigation.MEDIA) {
            MediaDetailScreen(
                onBack = { navController.popBackStack() },
                navController = navController,
            )
        }
        detailComposable(Constants.Navigation.INSIGHTS) {
            InsightsDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        detailComposable(Constants.Navigation.RECLAIMABLE) {
            ReclaimableDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        detailComposable(Constants.Navigation.JUNK_FILES) {
            JunkFilesScreen(onBack = { navController.popBackStack() })
        }
        detailComposable(Constants.Navigation.DUPLICATE_FILES) {
            DuplicatesScreen(onBack = { navController.popBackStack() })
        }
        detailComposable(Constants.Navigation.UNUSED_APPS) {
            UnusedAppsScreen(onBack = { navController.popBackStack() })
        }
        detailComposable(Constants.Navigation.LARGE_FILES) {
            LargeFilesScreen(onBack = { navController.popBackStack() })
        }
        detailComposable(Constants.Navigation.TRASH) {
            TrashScreen(onBack = { navController.popBackStack() })
        }
        detailComposable(Constants.Navigation.DIAGNOSTICS) {
            com.sunilshetty.clearspace.ui.diagnostics.DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
        categoryDetailRoute(navController, Constants.Navigation.CATEGORY_PHOTOS)
        categoryDetailRoute(navController, Constants.Navigation.CATEGORY_VIDEOS)
        categoryDetailRoute(navController, Constants.Navigation.CATEGORY_DOWNLOADS)

        detailComposable(Constants.Navigation.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
