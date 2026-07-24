package com.sunilshetty.clearspace.ui.theme

import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import com.sunilshetty.clearspace.R

enum class PaletteFamily {
    Red,
    Orange,
    Green,
    Blue,
    Purple,
    Grey,
}

object ClearSpacePalettes {
    val red: List<Int> = listOf(
        R.color.red_0,
        R.color.red_1,
        R.color.red_2,
        R.color.red_3,
        R.color.red_4,
        R.color.red_5,
        R.color.red_6,
        R.color.red_7,
        R.color.red_8,
        R.color.red_9,
    )

    val orange: List<Int> = listOf(
        R.color.orange_0,
        R.color.orange_1,
        R.color.orange_2,
        R.color.orange_3,
        R.color.orange_4,
        R.color.orange_5,
        R.color.orange_6,
        R.color.orange_7,
        R.color.orange_8,
        R.color.orange_9,
    )

    val green: List<Int> = listOf(
        R.color.green_0,
        R.color.green_1,
        R.color.green_2,
        R.color.green_3,
        R.color.green_4,
        R.color.green_5,
        R.color.green_6,
        R.color.green_7,
        R.color.green_8,
        R.color.green_9,
    )

    val blue: List<Int> = listOf(
        R.color.blue_0,
        R.color.blue_1,
        R.color.blue_2,
        R.color.blue_3,
        R.color.blue_4,
        R.color.blue_5,
        R.color.blue_6,
        R.color.blue_7,
        R.color.blue_8,
        R.color.blue_9,
    )

    val purple: List<Int> = listOf(
        R.color.purple_0,
        R.color.purple_1,
        R.color.purple_2,
        R.color.purple_3,
        R.color.purple_4,
        R.color.purple_5,
        R.color.purple_6,
        R.color.purple_7,
        R.color.purple_8,
        R.color.purple_9,
    )

    val grey: List<Int> = listOf(
        R.color.grey_0,
        R.color.grey_1,
        R.color.grey_2,
        R.color.grey_3,
        R.color.grey_4,
        R.color.grey_5,
        R.color.grey_6,
        R.color.grey_7,
        R.color.grey_8,
        R.color.grey_9,
    )

    fun steps(family: PaletteFamily): List<Int> = when (family) {
        PaletteFamily.Red -> red
        PaletteFamily.Orange -> orange
        PaletteFamily.Green -> green
        PaletteFamily.Blue -> blue
        PaletteFamily.Purple -> purple
        PaletteFamily.Grey -> grey
    }
}

@Composable
fun paletteColor(family: PaletteFamily, step: Int): Color {
    val steps = ClearSpacePalettes.steps(family)
    require(step in steps.indices)
    return resolvePaletteColor(steps[step])
}

@Composable
fun paletteColor(@ColorRes colorRes: Int): Color = resolvePaletteColor(colorRes)

@Composable
private fun resolvePaletteColor(@ColorRes colorRes: Int): Color {
    val useNightPalette = LocalClearSpaceDarkTheme.current
    if (!useNightPalette) {
        return colorResource(colorRes)
    }
    val context = LocalContext.current
    val nightContext = remember(context) {
        val configuration = Configuration(context.resources.configuration)
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_YES
        context.createConfigurationContext(configuration)
    }
    return Color(ContextCompat.getColor(nightContext, colorRes))
}
