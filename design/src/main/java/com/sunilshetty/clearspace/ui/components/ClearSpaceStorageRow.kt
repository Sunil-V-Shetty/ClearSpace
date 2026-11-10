package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Category size is the accessible-file total; the decorative bar uses device capacity. */
@Composable
fun ClearSpaceStorageRow(title: String, amount: String, icon: ImageVector, fraction: Float?,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick, modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(amount, Modifier.widthIn(max = 120.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (fraction != null) {
                LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
