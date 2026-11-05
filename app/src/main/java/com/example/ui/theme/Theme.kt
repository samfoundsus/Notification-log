package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.repository.ThemeOption

private const val BACKGROUND_TINT_FRACTION = 0.10f
private const val CONTAINER_TINT_FRACTION = 0.10f

private val FallbackDarkColorScheme = darkColorScheme(
    primary = SeedDarkPrimary,
    onPrimary = SeedDarkOnPrimary,
    primaryContainer = SeedDarkPrimaryContainer,
    onPrimaryContainer = SeedDarkOnPrimaryContainer,
    secondary = SeedDarkSecondary,
    onSecondary = SeedDarkOnSecondary,
    secondaryContainer = SeedDarkSecondaryContainer,
    onSecondaryContainer = SeedDarkOnSecondaryContainer,
    tertiary = SeedDarkTertiary,
    onTertiary = SeedDarkOnTertiary,
    tertiaryContainer = SeedDarkTertiaryContainer,
    onTertiaryContainer = SeedDarkOnTertiaryContainer,
    background = SeedDarkBackground,
    onBackground = SeedDarkOnBackground,
    surface = SeedDarkSurface,
    onSurface = SeedDarkOnSurface,
    surfaceVariant = SeedDarkSurfaceVariant,
    onSurfaceVariant = SeedDarkOnSurfaceVariant,
    surfaceContainerLowest = SeedDarkSurfaceContainerLowest,
    surfaceContainerLow = SeedDarkSurfaceContainerLow,
    surfaceContainer = SeedDarkSurfaceContainer,
    surfaceContainerHigh = SeedDarkSurfaceContainerHigh,
    surfaceContainerHighest = SeedDarkSurfaceContainerHighest,
    outline = SeedDarkOutline,
    outlineVariant = SeedDarkOutlineVariant
)

private val FallbackLightColorScheme = lightColorScheme(
    primary = SeedLightPrimary,
    onPrimary = SeedLightOnPrimary,
    primaryContainer = SeedLightPrimaryContainer,
    onPrimaryContainer = SeedLightOnPrimaryContainer,
    secondary = SeedLightSecondary,
    onSecondary = SeedLightOnSecondary,
    secondaryContainer = SeedLightSecondaryContainer,
    onSecondaryContainer = SeedLightOnSecondaryContainer,
    tertiary = SeedLightTertiary,
    onTertiary = SeedLightOnTertiary,
    tertiaryContainer = SeedLightTertiaryContainer,
    onTertiaryContainer = SeedLightOnTertiaryContainer,
    background = SeedLightBackground,
    onBackground = SeedLightOnBackground,
    surface = SeedLightSurface,
    onSurface = SeedLightOnSurface,
    surfaceVariant = SeedLightSurfaceVariant,
    onSurfaceVariant = SeedLightOnSurfaceVariant,
    surfaceContainerLowest = SeedLightSurfaceContainerLowest,
    surfaceContainerLow = SeedLightSurfaceContainerLow,
    surfaceContainer = SeedLightSurfaceContainer,
    surfaceContainerHigh = SeedLightSurfaceContainerHigh,
    surfaceContainerHighest = SeedLightSurfaceContainerHighest,
    outline = SeedLightOutline,
    outlineVariant = SeedLightOutlineVariant
)

private fun ColorScheme.withIncreasedDynamicTint(): ColorScheme {
    val tint = surfaceTint
    return copy(
        background = lerp(background, tint, BACKGROUND_TINT_FRACTION),
        surface = lerp(surface, tint, BACKGROUND_TINT_FRACTION),
        surfaceContainerLow = lerp(surfaceContainerLow, tint, CONTAINER_TINT_FRACTION),
        surfaceContainer = lerp(surfaceContainer, tint, CONTAINER_TINT_FRACTION),
        surfaceContainerHigh = lerp(surfaceContainerHigh, tint, CONTAINER_TINT_FRACTION),
        surfaceContainerHighest = lerp(surfaceContainerHighest, tint, CONTAINER_TINT_FRACTION)
    )
}

@Composable
fun NotificationLogTheme(
    themeOption: ThemeOption = ThemeOption.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeOption) {
        ThemeOption.SYSTEM -> systemDark
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColorScheme
        else -> FallbackLightColorScheme
    }

    val colorScheme = baseColorScheme.withIncreasedDynamicTint()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
