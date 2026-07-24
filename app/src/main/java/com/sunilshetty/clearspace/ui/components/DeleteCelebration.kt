package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sunilshetty.clearspace.core.constants.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun ClearSpaceCompleteCelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        CompleteGif(size = 200.dp)
    }
}

fun showDeleteCelebration(
    scope: CoroutineScope,
    visible: MutableStateFlow<Boolean>,
) {
    scope.launch {
        visible.value = true
        delay(Constants.Ui.DELETE_CELEBRATION_MS)
        visible.value = false
    }
}
