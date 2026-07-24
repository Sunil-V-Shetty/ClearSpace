package com.sunilshetty.clearspace.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.ui.components.ClearSpaceScreenHeader
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.components.BentoLabel
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.bentoHorizontalPadding
import com.sunilshetty.clearspace.ui.components.rememberBentoScrollState
import com.sunilshetty.clearspace.ui.theme.ClearSpaceTheme

@Composable
fun ToolsScreen(
    onOpenSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoScreenBackground(modifier = modifier.fillMaxSize()) {
        ClearSpaceScreenHeader(
            titleRes = R.string.tools_title,
            onOpenSettings = onOpenSettings,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberBentoScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolsGridSection(
                titleRes = R.string.tools_section_cleanup,
                tools = ClearSpaceToolsCatalog.cleanupTools,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun ToolsGridSection(
    titleRes: Int,
    tools: List<ClearSpaceToolEntry>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bentoHorizontalPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BentoSubtleText(text = stringResource(R.string.tools_section_cleanup_subtitle))
        tools.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { tool ->
                    BentoCard(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 108.dp),
                        onClick = { onNavigate(tool.route) },
                    ) {
                        BentoLabel(text = stringResource(tool.labelRes))
                        BentoSubtleText(text = stringResource(tool.hintRes))
                    }
                }
                if (pair.size == 1) {
                    Row(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolsScreenPreview() {
    ClearSpaceTheme {
        ToolsScreen(onOpenSettings = {}, onNavigate = {})
    }
}
