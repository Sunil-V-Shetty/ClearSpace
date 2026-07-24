package com.sunilshetty.clearspace.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val DETAIL_TRANSITION_MS = 280
private const val TAB_TRANSITION_MS = 200
private const val MAIN_POP_ENTER_MS = 220

private val detailSlideSpec = tween<IntOffset>(DETAIL_TRANSITION_MS, easing = FastOutSlowInEasing)
private val detailFadeSpec = tween<Float>(DETAIL_TRANSITION_MS, easing = FastOutSlowInEasing)
private val tabMotionSpec = tween<Float>(TAB_TRANSITION_MS, easing = FastOutSlowInEasing)
private val mainPopEnterSpec = tween<Float>(MAIN_POP_ENTER_MS, easing = FastOutSlowInEasing)

private val detailEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = detailSlideSpec) { fullWidth -> fullWidth } +
        fadeIn(detailFadeSpec)
}

private val detailExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = detailSlideSpec) { fullWidth -> -fullWidth / 4 } +
        fadeOut(detailFadeSpec)
}

private val detailPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = detailSlideSpec) { fullWidth -> -fullWidth / 4 } +
        fadeIn(detailFadeSpec)
}

private val detailPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = detailSlideSpec) { fullWidth -> fullWidth } +
        fadeOut(detailFadeSpec)
}

private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tabMotionSpec)
}

private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tabMotionSpec)
}

/** Main tabs shell when returning from a full-screen detail route. */
val mainTabPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(mainPopEnterSpec)
}

fun NavGraphBuilder.tabComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        enterTransition = tabEnter,
        exitTransition = tabExit,
        popEnterTransition = tabEnter,
        popExitTransition = tabExit,
        content = content,
    )
}

fun NavGraphBuilder.detailComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        enterTransition = detailEnter,
        exitTransition = detailExit,
        popEnterTransition = detailPopEnter,
        popExitTransition = detailPopExit,
        content = content,
    )
}
