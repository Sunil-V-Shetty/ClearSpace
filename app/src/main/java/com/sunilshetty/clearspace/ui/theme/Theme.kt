package com.sunilshetty.clearspace.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

@Composable
private fun clearSpaceLightColorScheme(): ColorScheme = lightColorScheme(
    primary = paletteColor(PaletteFamily.Blue, step = 7),
    onPrimary = paletteColor(PaletteFamily.Grey, step = 0),
    secondary = paletteColor(PaletteFamily.Green, step = 6),
    onSecondary = paletteColor(PaletteFamily.Grey, step = 0),
    tertiary = paletteColor(PaletteFamily.Purple, step = 6),
    background = paletteColor(PaletteFamily.Grey, step = 1),
    onBackground = paletteColor(PaletteFamily.Grey, step = 9),
    surface = paletteColor(PaletteFamily.Grey, step = 0),
    onSurface = paletteColor(PaletteFamily.Grey, step = 9),
    surfaceVariant = paletteColor(PaletteFamily.Grey, step = 2),
    onSurfaceVariant = paletteColor(PaletteFamily.Grey, step = 7),
)

@Composable
private fun clearSpaceDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = paletteColor(PaletteFamily.Blue, step = 8),
    onPrimary = paletteColor(PaletteFamily.Grey, step = 9),
    secondary = paletteColor(PaletteFamily.Green, step = 7),
    onSecondary = paletteColor(PaletteFamily.Grey, step = 9),
    tertiary = paletteColor(PaletteFamily.Purple, step = 8),
    background = paletteColor(PaletteFamily.Grey, step = 0),
    onBackground = paletteColor(PaletteFamily.Grey, step = 9),
    surface = paletteColor(PaletteFamily.Grey, step = 2),
    onSurface = paletteColor(PaletteFamily.Grey, step = 9),
    surfaceVariant = paletteColor(PaletteFamily.Grey, step = 3),
    onSurfaceVariant = paletteColor(PaletteFamily.Grey, step = 7),
)

@Composable
fun ClearSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClearSpaceDarkTheme provides darkTheme) {
        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> clearSpaceDarkColorScheme()
            else -> clearSpaceLightColorScheme()
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
