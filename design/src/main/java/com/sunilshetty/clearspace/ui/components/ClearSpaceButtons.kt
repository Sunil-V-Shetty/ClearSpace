package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.design.R

fun Modifier.clearSpaceBottomBarPadding(): Modifier =
    fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)

@Composable
fun ClearSpaceSelectAllCheckbox(totalCount: Int, selectedCount: Int, onSelectAll: () -> Unit,
    onClearSelection: () -> Unit, modifier: Modifier = Modifier) {
    if (totalCount <= 0) return
    val state = when {
        selectedCount <= 0 -> ToggleableState.Off
        selectedCount >= totalCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val label = stringResource(R.string.junk_select_all)
    Row(modifier.semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        TriStateCheckbox(state, onClick = { if (state == ToggleableState.Off) onSelectAll() else onClearSelection() })
    }
}

@Composable
fun ClearSpaceSelectionSummaryRow(summary: String, totalCount: Int, selectedCount: Int,
    onSelectAll: () -> Unit, onClearSelection: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(summary, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        ClearSpaceSelectAllCheckbox(totalCount, selectedCount, onSelectAll, onClearSelection)
    }
}

@Composable
fun ClearSpacePrimaryButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, icon: ImageVector? = Icons.Outlined.DeleteOutline) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
        ActionLabel(label, icon)
    }
}

@Composable
fun ClearSpaceSecondaryButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, icon: ImageVector? = null) {
    FilledTonalButton(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
        ActionLabel(label, icon)
    }
}

@Composable
private fun ActionLabel(label: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(label, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun QuickScanPrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, label: String = stringResource(R.string.quick_scan_action)) {
    ClearSpacePrimaryButton(onClick, label, modifier, enabled, Icons.Outlined.Radar)
}

@Composable
fun QuickScanSecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    label: String = stringResource(R.string.quick_scan_done_continue)) {
    ClearSpaceSecondaryButton(onClick, label, modifier)
}
