package com.sunilshetty.clearspace.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.data.model.StorageCategoryBreakdown

@Composable
internal fun CategoryBreakdownLine(
    breakdown: StorageCategoryBreakdown,
    formatBytes: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(breakdown.category.labelRes()),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = formatBytes(breakdown.totalBytes),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
