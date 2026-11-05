package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.AppIconProvider
import com.example.util.MonogramTone

@Composable
fun AppIconMonogram(
    packageName: String,
    appName: String,
    iconProvider: AppIconProvider,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    var iconBitmap by remember(packageName, iconProvider) {
        mutableStateOf<ImageBitmap?>(iconProvider.getCachedIcon(packageName))
    }

    LaunchedEffect(packageName, iconProvider) {
        if (iconBitmap == null) {
            iconBitmap = iconProvider.loadIcon(packageName)
        }
    }

    val resolvedBitmap = iconBitmap
    if (resolvedBitmap != null) {
        Image(
            bitmap = resolvedBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .clearAndSetSemantics { }
        )
    } else {
        val spec = remember(packageName, appName, iconProvider) {
            iconProvider.getMonogram(packageName, appName)
        }

        val colorScheme = MaterialTheme.colorScheme
        val (containerColor, contentColor) = when (spec.tone) {
            MonogramTone.PRIMARY -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
            MonogramTone.SECONDARY -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
            MonogramTone.TERTIARY -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = spec.letter.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
