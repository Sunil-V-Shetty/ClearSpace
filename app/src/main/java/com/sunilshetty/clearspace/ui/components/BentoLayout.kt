package com.sunilshetty.clearspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunilshetty.clearspace.ui.theme.LocalClearSpaceDarkTheme
import com.sunilshetty.clearspace.ui.theme.PaletteFamily
import com.sunilshetty.clearspace.ui.theme.paletteColor

private val BentoCorner = RoundedCornerShape(22.dp)

@Composable
fun bentoCanvasColor(): Color {
    return if (LocalClearSpaceDarkTheme.current) {
        paletteColor(PaletteFamily.Grey, step = 0)
    } else {
        paletteColor(PaletteFamily.Grey, step = 1)
    }
}

@Composable
fun bentoCardColor(): Color {
    return if (LocalClearSpaceDarkTheme.current) {
        paletteColor(PaletteFamily.Grey, step = 2)
    } else {
        Color.White
    }
}

@Composable
fun BentoScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.background(bentoCanvasColor()),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = BentoCorner,
            colors = CardDefaults.cardColors(containerColor = bentoCardColor()),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = BentoCorner,
            colors = CardDefaults.cardColors(containerColor = bentoCardColor()),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun BentoLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun BentoValue(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun BentoSubtleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun BentoStorageProgress(
    usedFraction: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { usedFraction },
        modifier = modifier.fillMaxWidth(),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoQuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .padding(14.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        if (caption != null) {
            Text(
                text = caption,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

fun Modifier.bentoHorizontalPadding(): Modifier = padding(horizontal = 16.dp)

@Composable
fun rememberBentoScrollState(): ScrollState =
    rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(0)
    }
