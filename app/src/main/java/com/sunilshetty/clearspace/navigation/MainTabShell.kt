package com.sunilshetty.clearspace.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.ui.components.bentoCanvasColor
import com.sunilshetty.clearspace.ui.components.bentoCardColor
import com.sunilshetty.clearspace.ui.home.HomeScreen
import com.sunilshetty.clearspace.ui.tools.ToolsScreen

@Composable
fun MainTabShell(
    rootNavController: NavHostController,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val tabBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val tabDestination = tabBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        containerColor = bentoCanvasColor(),
        bottomBar = {
            if (tabDestination != null) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NavigationBar(
                    containerColor = bentoCardColor(),
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = tabDestination.hierarchy.any {
                            it.route == Constants.Navigation.HOME
                        },
                        onClick = {
                            tabNavController.navigate(Constants.Navigation.HOME) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Dashboard,
                                contentDescription = stringResource(R.string.nav_home),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = tabDestination.hierarchy.any {
                            it.route == Constants.Navigation.TOOLS
                        },
                        onClick = {
                            tabNavController.navigate(Constants.Navigation.TOOLS) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = stringResource(R.string.nav_tools),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_tools)) },
                        colors = navItemColors,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Constants.Navigation.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            tabComposable(Constants.Navigation.HOME) {
                HomeScreen(
                    navController = rootNavController,
                    onOpenSettings = onOpenSettings,
                    onNavigate = rootNavController::navigateToDetail,
                )
            }
            tabComposable(Constants.Navigation.TOOLS) {
                ToolsScreen(
                    onOpenSettings = onOpenSettings,
                    onNavigate = rootNavController::navigateToDetail,
                )
            }
        }
    }
}
