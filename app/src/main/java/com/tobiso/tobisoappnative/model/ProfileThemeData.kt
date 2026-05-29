package com.tobiso.tobisoappnative.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ProfileThemeData {

    data class ThemeColors(
        val primary: Color,
        val secondary: Color
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
            primary = Color(0xFFE65100),
            secondary = Color(0xFFFF6F00)
        ),
        103 to ThemeColors(
            primary = Color(0xFF006064),
            secondary = Color(0xFF00838F)
        ),
        104 to ThemeColors(
            primary = Color(0xFF6A1B9A),
            secondary = Color(0xFF8E24AA)
        )
    )

    fun getThemeColors(itemId: Int): ThemeColors? = themeColorMap[itemId]

    fun getThemeGradient(itemId: Int): Brush? {
        val colors = themeColorMap[itemId] ?: return null
        return Brush.horizontalGradient(
            colors = listOf(colors.primary, colors.secondary),
            startX = 0f,
            endX = Float.POSITIVE_INFINITY
        )
    }

    fun getThemePrimaryColor(itemId: Int): Color? = themeColorMap[itemId]?.primary
}
