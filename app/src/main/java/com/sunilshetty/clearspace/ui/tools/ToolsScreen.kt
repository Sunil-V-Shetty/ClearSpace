package com.sunilshetty.clearspace.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.ui.components.*

@Composable
fun ToolsScreen(onOpenSettings: () -> Unit, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    BentoScreenBackground(modifier.fillMaxSize()) {
        ClearSpaceScreenHeader(R.string.tools_title, onOpenSettings)
        Column(Modifier.fillMaxSize().verticalScroll(rememberBentoScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val analysis = setOf(Constants.Navigation.DIAGNOSTICS, Constants.Navigation.INSIGHTS, Constants.Navigation.RECLAIMABLE)
            listOf("Clean up" to ClearSpaceToolsCatalog.cleanupTools.filter { it.route !in analysis },
                "Device and storage" to ClearSpaceToolsCatalog.cleanupTools.filter { it.route in analysis }).forEach { (heading, tools) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(heading, style = MaterialTheme.typography.titleMedium)
                    tools.forEach { tool ->
                        ClearSpaceActionRow(stringResource(tool.labelRes), stringResource(tool.hintRes),
                            toolIcon(tool.route), { onNavigate(tool.route) })
                    }
                }
            }
        }
    }
}

private fun toolIcon(route: String): ImageVector = when (route) {
    Constants.Navigation.JUNK_FILES -> Icons.Outlined.CleaningServices
    Constants.Navigation.DUPLICATE_FILES -> Icons.Outlined.ContentCopy
    Constants.Navigation.UNUSED_APPS -> Icons.Outlined.Apps
    Constants.Navigation.LARGE_FILES -> Icons.Outlined.Folder
    Constants.Navigation.TRASH -> Icons.Outlined.RestoreFromTrash
    Constants.Navigation.DIAGNOSTICS -> Icons.Outlined.MonitorHeart
    Constants.Navigation.INSIGHTS -> Icons.Outlined.AutoAwesome
    else -> Icons.Outlined.DataUsage
}
