package com.sunilshetty.clearspace

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sunilshetty.clearspace.navigation.ClearSpaceNavHost
import com.sunilshetty.clearspace.ui.permissions.AppPermissionsHost
import com.sunilshetty.clearspace.ui.theme.ClearSpaceSystemBars
import com.sunilshetty.clearspace.ui.theme.ClearSpaceTheme
import com.sunilshetty.clearspace.ui.theme.ThemeViewModel

@Composable
fun ClearSpaceApp(modifier: Modifier = Modifier) {
    val themeViewModel: ThemeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val darkTheme by themeViewModel.darkModeEnabled.collectAsStateWithLifecycle()

    ClearSpaceTheme(darkTheme = darkTheme) {
        ClearSpaceSystemBars(darkTheme = darkTheme)
        AppPermissionsHost {
            ClearSpaceAppContent(modifier = modifier)
        }
    }
}

@Composable
private fun ClearSpaceAppContent(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    ClearSpaceNavHost(
        navController = navController,
        modifier = modifier.fillMaxSize(),
    )
}
