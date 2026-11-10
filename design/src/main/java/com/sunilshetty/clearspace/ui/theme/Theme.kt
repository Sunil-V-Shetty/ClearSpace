package com.sunilshetty.clearspace.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Semantic roles are shared by every feature; no per-screen palette overrides.
internal val ClearSpaceLightColors = lightColorScheme(
    primary = Color(0xFF006DAA), onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBFF), onPrimaryContainer = Color(0xFF00344F),
    secondary = Color(0xFF4C6070), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E5F5), onSecondaryContainer = Color(0xFF132F40),
    tertiary = Color(0xFF546482), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E3FF), onTertiaryContainer = Color(0xFF172E4F),
    background = Color(0xFFF5FAFF), onBackground = Color(0xFF14212C),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF14212C),
    surfaceVariant = Color(0xFFE4EEF7), onSurfaceVariant = Color(0xFF455562),
    outline = Color(0xFF71818E), outlineVariant = Color(0xFFD0DEE9),
    error = Color(0xFFBA1A1A), onError = Color.White,
)
internal val ClearSpaceDarkColors = darkColorScheme(
    primary = Color(0xFF8DCDFF), onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C72), onPrimaryContainer = Color(0xFFCDEBFF),
    secondary = Color(0xFFB4CADB), onSecondary = Color(0xFF1D3342),
    secondaryContainer = Color(0xFF354B5A), onSecondaryContainer = Color(0xFFD0E5F5),
    tertiary = Color(0xFFBDC8E8), onTertiary = Color(0xFF263553),
    tertiaryContainer = Color(0xFF3C4C6B), onTertiaryContainer = Color(0xFFD8E3FF),
    background = Color(0xFF0D1720), onBackground = Color(0xFFDBE7F2),
    surface = Color(0xFF172530), onSurface = Color(0xFFDBE7F2),
    surfaceVariant = Color(0xFF293A48), onSurfaceVariant = Color(0xFFBBCAD7),
    outline = Color(0xFF8597A5), outlineVariant = Color(0xFF3B4D5C),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

@Composable
fun ClearSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClearSpaceDarkTheme provides darkTheme) {
        val colors = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
            darkTheme -> ClearSpaceDarkColors
            else -> ClearSpaceLightColors
        }
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp),
            ),
            content = content,
        )
    }
}
