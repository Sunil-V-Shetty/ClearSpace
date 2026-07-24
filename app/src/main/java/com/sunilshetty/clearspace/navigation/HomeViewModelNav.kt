package com.sunilshetty.clearspace.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.ui.home.HomeViewModel

/**
 * Shared across the main nav graph (home dashboard + detail screens), not only HomeScreen.
 */
@Composable
fun rememberHomeViewModel(navController: NavHostController): HomeViewModel {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val mainEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry(Constants.Navigation.MAIN)
    }
    return hiltViewModel(mainEntry)
}

fun NavHostController.navigateToDetail(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}
