package com.sunilshetty.clearspace.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.design.R

@Composable
fun ClearSpaceScreenHeader(
    @StringRes titleRes: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onRefreshStorage: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClearSpaceBrandMark(Modifier.size(28.dp))
            Text(stringResource(titleRes), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRefreshStorage != null) {
                IconButton(onClick = onRefreshStorage) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.content_description_refresh_storage),
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.content_description_settings),
                )
            }
        }
    }
}
