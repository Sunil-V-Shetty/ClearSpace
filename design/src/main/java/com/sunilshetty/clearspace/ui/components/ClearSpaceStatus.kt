package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.design.R

/** Native indicators follow the platform's animation-duration setting. */
@Composable
fun ClearSpaceLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 96.dp, contentDescription: String? = null) {
    Box(modifier.size(size).semantics { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(size * 0.7f), strokeWidth = 3.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        ClearSpaceBrandMark(Modifier.size(size * 0.28f))
    }
}

@Composable
fun ClearSpaceSuccessMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Rounded.Check, null, Modifier.padding(size * 0.18f).size(size * 0.4f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ClearSpaceBrandMark(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_clearspace_mark), contentDescription = null,
        modifier = modifier, tint = MaterialTheme.colorScheme.primary)
}
