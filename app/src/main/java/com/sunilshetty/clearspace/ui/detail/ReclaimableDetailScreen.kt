package com.sunilshetty.clearspace.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunilshetty.clearspace.R

@Composable
fun ReclaimableDetailScreen(onBack: () -> Unit) {
    DetailScreenScaffold(titleRes = R.string.card_reclaimable, onBack = onBack) {
        DetailBodyText(text = stringResource(R.string.card_reclaimable_empty))
    }
}
