package com.tobiso.tobisoappnative.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ProfileThemeData {

    data class ThemeColors(
        val primary: Color,
        val secondary: Color,
        /** Non-null overrides the two-stop gradient with a full multi-stop list. */
        val gradientColors: List<Color>? = null
    )

    private val themeColorMap = mapOf(
        100 to ThemeColors(
            primary = Color(0xFF1A237E),
            secondary = Color(0xFF283593)
        ),
        101 to ThemeColors(
            primary = Color(0xFF1B5E20),
            secondary = Color(0xFF2E7D32)
        ),
        102 to ThemeColors(
            primary = Color(0xFFBF360C),
            secondary = Color(0xFFFF8F00),
            gradientColors = listOf(
                Color(0xFFBF360C),
                Color(0xFFE64A19),
                Color(0xFFFF8F00)
            )
        ),
        103 to ThemeColors(
            primary = Color(0xFF006064),
            secondary = Color(0xFF00838F)
        ),
        104 to ThemeColors(
            primary = Color(0xFFE53935),
            secondary = Color(0xFF8E24AA),
            gradientColors = listOf(
                Color(0xFFE53935), // red
                Color(0xFFFF9800), // orange
                Color(0xFFFFEB3B), // yellow
                Color(0xFF43A047), // green
                Color(0xFF1E88E5), // blue
                Color(0xFF8E24AA)  // violet
            )
        )
    )

    fun getThemeColors(itemId: Int): ThemeColors? = themeColorMap[itemId]

    /** Returns the gradient color stops for the given theme (2+ colors). */
    fun getGradientColors(itemId: Int): List<Color>? {
        val colors = themeColorMap[itemId] ?: return null
        return colors.gradientColors ?: listOf(colors.primary, colors.secondary)
    }

    fun getThemeGradient(itemId: Int): Brush? {
        val gradColors = getGradientColors(itemId) ?: return null
        return Brush.horizontalGradient(colors = gradColors)
    }

    fun getThemePrimaryColor(itemId: Int): Color? = themeColorMap[itemId]?.primary
}
