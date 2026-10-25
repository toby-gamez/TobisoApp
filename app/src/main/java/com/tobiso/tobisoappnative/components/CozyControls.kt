package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

/*
 * Drop-in replacements for the Material 3 buttons and progress bar with the "cozy" defaults:
 * pill shape, a springy squish on press and smoothly animated progress. Screens opt in by importing
 * these instead of the material3 versions (same pattern as TobisoCard.kt).
 */

private val CozyButtonShape: Shape = RoundedCornerShape(50)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CozyButtonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = androidx.compose.material3.Button(
    onClick, modifier.cozyPress(0.95f), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content
)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CozyButtonShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = androidx.compose.material3.OutlinedButton(
    onClick, modifier.cozyPress(0.95f), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content
)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CozyButtonShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = androidx.compose.material3.TextButton(
    onClick, modifier.cozyPress(0.94f), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content
)

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = androidx.compose.material3.IconButton(
    onClick = onClick, modifier = modifier.cozyPress(0.82f), enabled = enabled, colors = colors,
    interactionSource = interactionSource, content = content
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
/** Determinate progress bar. Deliberately not animated, so it always shows the exact current value. */
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    drawStopIndicator: DrawScope.() -> Unit = { }
) {
    androidx.compose.material3.LinearProgressIndicator(progress, modifier, color, trackColor, strokeCap, gapSize, drawStopIndicator)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize
) = androidx.compose.material3.LinearProgressIndicator(modifier, color, trackColor, strokeCap, gapSize)
