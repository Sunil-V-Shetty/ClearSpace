package com.sunilshetty.clearspace.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.ui.components.*
import com.sunilshetty.clearspace.ui.permissions.LocalAppPermissionActions
import com.sunilshetty.clearspace.util.StorageFormatter

@Composable
fun CleanupOverviewScreen(titleRes: Int, onBack: () -> Unit, onNavigate: (String) -> Unit,
    viewModel: CleanupOverviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val actions = LocalAppPermissionActions.current
    var access by remember { mutableStateOf(hasJunkReadAccess(context)) }
    val formatBytes = rememberStorageFormatBytes()
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            val granted = hasJunkReadAccess(context)
            if (granted && (!access || state.result == null)) viewModel.scan()
            access = granted
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { if (access && state.result == null) viewModel.scan() }
    DetailScreenScaffold(titleRes, onBack) {
        Text("Review your cleanup", style = MaterialTheme.typography.headlineMedium)
        if (!access) {
            BentoCard {
                Text("Scan for cleanup", style = MaterialTheme.typography.titleMedium)
                Text("Allow access to find files to review.")
                Button(onClick = { actions?.requestAllPermissions() }) { Text("Allow media access") }
            }
        } else {
            OutlinedButton(onClick = viewModel::scan, enabled = !state.loading) { Text("Refresh") }
            if (state.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Comparing accessible files…") }
            if (state.error) Text("Scan failed. Check access and retry.", color = MaterialTheme.colorScheme.error)
            state.result?.let { result ->
                BentoCard(modifier = Modifier.padding(vertical = 12.dp)) {
                    BentoLabel("Space to review")
                    BentoValue(formatBytes(result.uniqueBytes))
                    Text("Estimated from accessible files; overlaps counted once.", style = MaterialTheme.typography.bodyMedium)
                    Text("Empty Trash to free recovery space.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Updated ${StorageFormatter.formatTimestamp(result.scannedAt)}", style = MaterialTheme.typography.bodySmall)
                }
                if (result.uniqueBytes == 0L) {
                    BentoCard(Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text("No cleanup suggestions", style = MaterialTheme.typography.titleMedium)
                        Text("No matches in this scan. Browse categories below.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                result.opportunities.forEach { opportunity ->
                    val title = when (opportunity.kind) { "duplicates" -> "Duplicate copies"; "large" -> "Large files"; else -> "Temporary files" }
                    val route = when (opportunity.kind) { "duplicates" -> Constants.Navigation.DUPLICATE_FILES; "large" -> Constants.Navigation.LARGE_FILES; else -> Constants.Navigation.JUNK_FILES }
                    ClearSpaceActionRow(title, "${opportunity.count} files · ${formatBytes(opportunity.bytes)} to review",
                        when (opportunity.kind) {
                            "duplicates" -> Icons.Outlined.ContentCopy
                            "large" -> Icons.Outlined.Folder
                            else -> Icons.Outlined.CleaningServices
                        }, { onNavigate(route) }, Modifier.padding(vertical = 6.dp))
                }
                Text("Results include accessible files only.",
                    Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = { onNavigate(Constants.Navigation.UNUSED_APPS) }) { Text("Unused apps") }
        TextButton(onClick = { onNavigate(Constants.Navigation.TRASH) }) { Text("Trash") }
        TextButton(onClick = { onNavigate(Constants.Navigation.DIAGNOSTICS) }) { Text("Device diagnostics") }
    }
}
