package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunilshetty.clearspace.R

@Composable
fun InsightsDetailScreen(onBack: () -> Unit) {
    DetailScreenScaffold(titleRes = R.string.card_insights, onBack = onBack) {
        DetailBodyText(text = stringResource(R.string.card_insights_empty))
    }
}
