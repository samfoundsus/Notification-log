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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.repository.ThemeOption

private const val DARK_SURFACE_BLEND_FRACTION = 0.08f
private const val DARK_CONTAINER_BLEND_FRACTION = 0.05f
private const val LIGHT_SURFACE_BLEND_FRACTION = 0.05f
private const val LIGHT_CONTAINER_BLEND_FRACTION = 0.04f

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

fun darkenScheme(scheme: ColorScheme, isDark: Boolean): ColorScheme {
    return if (isDark) {
        scheme.copy(
            background = lerp(scheme.background, Color.Black, DARK_SURFACE_BLEND_FRACTION),
            surface = lerp(scheme.surface, Color.Black, DARK_SURFACE_BLEND_FRACTION),
            surfaceVariant = lerp(scheme.surfaceVariant, Color.Black, DARK_SURFACE_BLEND_FRACTION),
            surfaceDim = lerp(scheme.surfaceDim, Color.Black, DARK_SURFACE_BLEND_FRACTION),
            surfaceBright = lerp(scheme.surfaceBright, Color.Black, DARK_SURFACE_BLEND_FRACTION),
            surfaceContainerLowest = lerp(
                scheme.surfaceContainerLowest,
                Color.Black,
                DARK_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerLow = lerp(
                scheme.surfaceContainerLow,
                Color.Black,
                DARK_SURFACE_BLEND_FRACTION
            ),
            surfaceContainer = lerp(
                scheme.surfaceContainer,
                Color.Black,
                DARK_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerHigh = lerp(
                scheme.surfaceContainerHigh,
                Color.Black,
                DARK_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerHighest = lerp(
                scheme.surfaceContainerHighest,
                Color.Black,
                DARK_SURFACE_BLEND_FRACTION
            ),
            primaryContainer = lerp(
                scheme.primaryContainer,
                Color.Black,
                DARK_CONTAINER_BLEND_FRACTION
            ),
            secondaryContainer = lerp(
                scheme.secondaryContainer,
                Color.Black,
                DARK_CONTAINER_BLEND_FRACTION
            ),
            tertiaryContainer = lerp(
                scheme.tertiaryContainer,
                Color.Black,
                DARK_CONTAINER_BLEND_FRACTION
            ),
            errorContainer = lerp(
                scheme.errorContainer,
                Color.Black,
                DARK_CONTAINER_BLEND_FRACTION
            )
        )
    } else {
        scheme.copy(
            background = lerp(scheme.background, Color.Black, LIGHT_SURFACE_BLEND_FRACTION),
            surface = lerp(scheme.surface, Color.Black, LIGHT_SURFACE_BLEND_FRACTION),
            surfaceVariant = lerp(scheme.surfaceVariant, Color.Black, LIGHT_SURFACE_BLEND_FRACTION),
            surfaceDim = lerp(scheme.surfaceDim, Color.Black, LIGHT_SURFACE_BLEND_FRACTION),
            surfaceBright = lerp(scheme.surfaceBright, Color.Black, LIGHT_SURFACE_BLEND_FRACTION),
            surfaceContainerLowest = lerp(
                scheme.surfaceContainerLowest,
                Color.Black,
                LIGHT_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerLow = lerp(
                scheme.surfaceContainerLow,
                Color.Black,
                LIGHT_SURFACE_BLEND_FRACTION
            ),
            surfaceContainer = lerp(
                scheme.surfaceContainer,
                Color.Black,
                LIGHT_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerHigh = lerp(
                scheme.surfaceContainerHigh,
                Color.Black,
                LIGHT_SURFACE_BLEND_FRACTION
            ),
            surfaceContainerHighest = lerp(
                scheme.surfaceContainerHighest,
                Color.Black,
                LIGHT_SURFACE_BLEND_FRACTION
            ),
            primaryContainer = lerp(
                scheme.primaryContainer,
                Color.Black,
                LIGHT_CONTAINER_BLEND_FRACTION
            ),
            secondaryContainer = lerp(
                scheme.secondaryContainer,
                Color.Black,
                LIGHT_CONTAINER_BLEND_FRACTION
            ),
            tertiaryContainer = lerp(
                scheme.tertiaryContainer,
                Color.Black,
                LIGHT_CONTAINER_BLEND_FRACTION
            ),
            errorContainer = lerp(
                scheme.errorContainer,
                Color.Black,
                LIGHT_CONTAINER_BLEND_FRACTION
            )
        )
    }
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

    val colorScheme = darkenScheme(baseColorScheme, darkTheme)

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
