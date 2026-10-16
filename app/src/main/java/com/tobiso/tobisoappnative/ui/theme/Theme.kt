package com.tobiso.tobisoappnative.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Accent400,
    onPrimary = Color(0xFF4A2008),
    primaryContainer = Accent400Dark,
    onPrimaryContainer = Accent300,
    secondary = Sage400,
    onSecondary = Sage900,
    secondaryContainer = Sage400Dark,
    onSecondaryContainer = Sage300,
    tertiary = Neutral400OnDark,
    onTertiary = OrganicBgDark,
    background = OrganicBgDark,
    onBackground = OrganicTextDark,
    surface = OrganicBgDark,
    onSurface = OrganicTextDark,
    surfaceVariant = OrganicSurfaceDark,
    onSurfaceVariant = Neutral800Dark,
    outline = Neutral500Dark,
    outlineVariant = Neutral300Dark,
    surfaceContainerLowest = OrganicBgDark,
    surfaceContainerLow = OrganicBgDark,
    surfaceContainer = OrganicSurfaceDark,
    surfaceContainerHigh = Neutral200Dark,
    surfaceContainerHighest = Neutral300Dark
)

private val LightColorScheme = lightColorScheme(
    primary = Accent600,
    onPrimary = OnAccent,
    primaryContainer = Accent200,
    onPrimaryContainer = Accent900,
    secondary = Sage600,
    onSecondary = OnAccent2,
    secondaryContainer = Sage200,
    onSecondaryContainer = Sage900,
    tertiary = Neutral700,
    onTertiary = OnAccent,
    background = OrganicBg,
    onBackground = OrganicText,
    surface = OrganicBg,
    onSurface = OrganicText,
    surfaceVariant = OrganicSurface,
    onSurfaceVariant = Neutral800,
    outline = Neutral500,
    outlineVariant = Neutral300,
    surfaceContainerLowest = Accent100,
    surfaceContainerLow = Neutral100,
    surfaceContainer = OrganicSurface,
    surfaceContainerHigh = Neutral200,
    surfaceContainerHighest = Neutral300
)

@Composable
fun TobisoAppNativeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OrganicShapes,
        content = content
    )
}