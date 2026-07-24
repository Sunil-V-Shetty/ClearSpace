package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.ui.theme.PaletteFamily
import com.sunilshetty.clearspace.ui.theme.paletteColor

val ClearSpaceActionButtonShape = RoundedCornerShape(14.dp)

/** Shared height for all bottom action buttons (primary + secondary). */
private val ClearSpaceBottomActionHeight = 44.dp

fun Modifier.clearSpaceBottomBarPadding(): Modifier =
    fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearSpaceSelectAllCheckbox(
    totalCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalCount <= 0) return

    val toggleState = when {
        selectedCount <= 0 -> ToggleableState.Off
        selectedCount >= totalCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val selectAllLabel = stringResource(R.string.junk_select_all)

    Row(
        modifier = modifier.semantics {
            contentDescription = selectAllLabel
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = selectAllLabel,
            style = MaterialTheme.typography.bodyMedium,
        )
        TriStateCheckbox(
            state = toggleState,
            onClick = {
                when (toggleState) {
                    ToggleableState.On,
                    ToggleableState.Indeterminate,
                    -> onClearSelection()
                    ToggleableState.Off -> onSelectAll()
                }
            },
        )
    }
}

@Composable
fun ClearSpaceSelectionSummaryRow(
    summary: String,
    totalCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        ClearSpaceSelectAllCheckbox(
            totalCount = totalCount,
            selectedCount = selectedCount,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearSpacePrimaryButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.Outlined.DeleteOutline,
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            paletteColor(PaletteFamily.Blue, step = 6),
            paletteColor(PaletteFamily.Blue, step = 8),
            paletteColor(PaletteFamily.Purple, step = 7),
        ),
    )
    ClearSpaceActionButtonContent(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentBrush = gradient,
        contentColor = Color.White,
        label = label,
        icon = icon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearSpaceSecondaryButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    ClearSpaceActionButtonContent(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentBrush = null,
        contentColor = MaterialTheme.colorScheme.onSurface,
        label = label,
        icon = icon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearSpaceActionButtonContent(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    containerColor: Color,
    contentBrush: Brush?,
    contentColor: Color,
    label: String,
    icon: ImageVector?,
) {
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(ClearSpaceBottomActionHeight),
        shape = ClearSpaceActionButtonShape,
        shadowElevation = 0.dp,
        color = containerColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ClearSpaceBottomActionHeight)
                .then(
                    if (contentBrush != null) {
                        Modifier.background(contentBrush, ClearSpaceActionButtonShape)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = label,
                    style = labelStyle,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun QuickScanPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(R.string.quick_scan_action),
) {
    ClearSpacePrimaryButton(
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        icon = Icons.Outlined.Radar,
    )
}

@Composable
fun QuickScanSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.quick_scan_done_continue),
) {
    ClearSpaceSecondaryButton(
        onClick = onClick,
        label = label,
        modifier = modifier,
    )
}
