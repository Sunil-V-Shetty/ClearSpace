package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunilshetty.clearspace.R

@Composable
fun InsightsDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    CleanupOverviewScreen(R.string.card_insights, onBack, onNavigate)
}
