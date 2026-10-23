package com.tobiso.tobisoappnative.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Semantic feedback colors drawn from the Organic palette (sage = good, terracotta = bad,
 * warm amber = in between) instead of stock Material green/red/yellow.
 */
object StatusColors {
    val success: Color @Composable get() = if (isSystemInDarkTheme()) Sage400 else Sage600
    val warning: Color @Composable get() = if (isSystemInDarkTheme()) Accent300 else Accent400
    val danger: Color @Composable get() = MaterialTheme.colorScheme.error
}
