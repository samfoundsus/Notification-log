package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.AppGlyph
import com.example.util.AppIconProvider

@Composable
fun AppIconMonogram(
    packageName: String,
    iconProvider: AppIconProvider,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    glyphSize: Dp = 22.dp
) {
    var glyph by remember(packageName, iconProvider) {
        mutableStateOf<AppGlyph?>(iconProvider.getCachedGlyph(packageName))
    }

    LaunchedEffect(packageName, iconProvider) {
        if (glyph == null) {
            glyph = iconProvider.resolveGlyph(packageName)
        }
    }

    val activeGlyph = glyph ?: remember(packageName, iconProvider) {
        iconProvider.getCategoryFallback(packageName)
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.secondaryContainer
    val contentColor = colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        when (activeGlyph) {
            is AppGlyph.Monochrome -> {
                Image(
                    bitmap = activeGlyph.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor, BlendMode.SrcIn),
                    modifier = Modifier.size(glyphSize)
                )
            }
            is AppGlyph.CategoryVector -> {
                Icon(
                    painter = painterResource(id = activeGlyph.drawableResId),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(glyphSize)
                )
            }
        }
    }
}
