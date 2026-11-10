package com.sunilshetty.clearspace.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun semanticTextRolesMeetNormalTextContrast() {
        listOf(ClearSpaceLightColors, ClearSpaceDarkColors).forEach { c ->
            listOf(c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                c.onSurface to c.surface, c.onSurfaceVariant to c.surface,
                c.onBackground to c.background, c.onSecondaryContainer to c.secondaryContainer,
                c.onError to c.error).forEach { (text, background) ->
                assertTrue("Insufficient contrast for $text on $background", ratio(text, background) >= 4.5)
            }
        }
    }
    @Test fun cleanupHeroTextMeetsNormalTextContrast() {
        listOf(ClearSpaceLightColors, ClearSpaceDarkColors).forEach { c ->
            assertTrue(ratio(c.onPrimaryContainer, c.primaryContainer) >= 4.5)
            assertTrue(ratio(c.onSurfaceVariant, c.background) >= 4.5)
        }
    }
    private fun ratio(a: Color, b: Color): Double =
        (maxOf(a.luminance(), b.luminance()) + 0.05) / (minOf(a.luminance(), b.luminance()) + 0.05)
}
