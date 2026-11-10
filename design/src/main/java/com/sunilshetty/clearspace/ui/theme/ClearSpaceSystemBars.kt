package com.sunilshetty.clearspace.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sunilshetty.clearspace.ui.components.bentoCanvasColor

@Composable
fun ClearSpaceSystemBars(darkTheme: Boolean) {
    val canvasColor = bentoCanvasColor()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = canvasColor.toArgb()
            window.navigationBarColor = canvasColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
