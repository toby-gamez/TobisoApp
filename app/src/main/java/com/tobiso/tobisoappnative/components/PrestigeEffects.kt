package com.tobiso.tobisoappnative.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PrestigeTier(val minPoints: Int, val label: String) {
    NONE(0, ""),
    BRONZE(1_000, "Bronzový"),
    SILVER(5_000, "Stříbrný"),
    GOLD(10_000, "Zlatý"),
    DIAMOND(25_000, "Diamantový"),
    LEGEND(50_000, "Legendární"),
    MYTHIC(100_000, "Mytický")
}

fun getPrestigeTier(points: Int): PrestigeTier =
    PrestigeTier.entries.lastOrNull { points >= it.minPoints } ?: PrestigeTier.NONE

private val tierRingColors = mapOf(
    PrestigeTier.BRONZE to listOf(
        Color(0xFFCD7F32), Color(0xFFE8AA5A), Color(0xFFFFF0C0),
        Color(0xFFE8AA5A), Color(0xFFCD7F32)
    ),
    PrestigeTier.SILVER to listOf(
        Color(0xFF9E9E9E), Color(0xFFE0E0E0), Color(0xFFFFFFFF),
        Color(0xFFE0E0E0), Color(0xFF9E9E9E)
    ),
    PrestigeTier.GOLD to listOf(
        Color(0xFFFFD700), Color(0xFFFFF59D), Color(0xFFFFAA00),
        Color(0xFFFFF59D), Color(0xFFFFD700)
    ),
    PrestigeTier.DIAMOND to listOf(
        Color(0xFF40C4FF), Color(0xFFCE93D8), Color(0xFFF48FB1),
        Color(0xFF80DEEA), Color(0xFF40C4FF)
    ),
    PrestigeTier.LEGEND to listOf(
        Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFFEB3B),
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935)
    ),
    PrestigeTier.MYTHIC to listOf(
        Color(0xFFFF006E), Color(0xFF8338EC), Color(0xFF3A86FF),
        Color(0xFF06FFB4), Color(0xFFFFBE0B), Color(0xFFFF5400), Color(0xFFFF006E)
    )
)

fun tierColors(tier: PrestigeTier): List<Color> =
    tierRingColors[tier] ?: listOf(Color.Transparent)

/**
 * Formats a float point balance for display:
 * - >= 1 → integer string ("42")
 * - 0 < f < 1 → two decimal places ("0.02")
 * - 0 → "0"
 */
fun formatPointsBalance(f: Float): String = when {
    f >= 1f -> f.toInt().toString()
    f > 0f -> "%.2f".format(f)
    else -> "0"
}

fun tierEmoji(tier: PrestigeTier): String = when (tier) {
    PrestigeTier.BRONZE -> "🥉"
    PrestigeTier.SILVER -> "🥈"
    PrestigeTier.GOLD -> "🥇"
    PrestigeTier.DIAMOND -> "💎"
    PrestigeTier.LEGEND -> "🏆"
    PrestigeTier.MYTHIC -> "🌌"
    PrestigeTier.NONE -> ""
}

fun tierDescription(tier: PrestigeTier): String = when (tier) {
    PrestigeTier.BRONZE -> "1 000 celkových bodů"
    PrestigeTier.SILVER -> "5 000 celkových bodů"
    PrestigeTier.GOLD -> "10 000 celkových bodů"
    PrestigeTier.DIAMOND -> "25 000 celkových bodů"
    PrestigeTier.LEGEND -> "50 000 celkových bodů"
    PrestigeTier.MYTHIC -> "100 000 celkových bodů"
    PrestigeTier.NONE -> ""
}

/**
 * Wraps avatar content (80 dp) in an animated prestige border ring matching the
 * Discord Nitro avatar-decoration look. content lambda is scoped to the 80 dp Box so
 * Alignment.BottomEnd etc. resolve relative to the avatar, not the outer ring.
 */
@Composable
fun PrestigeAvatarBorder(
    tier: PrestigeTier,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (tier == PrestigeTier.NONE) {
        Box(modifier = modifier.size(80.dp), content = content)
        return
    }

    val borderWidth: Dp = when {
        tier >= PrestigeTier.MYTHIC -> 5.dp
        tier >= PrestigeTier.LEGEND -> 4.5.dp
        tier >= PrestigeTier.GOLD -> 4.dp
        else -> 3.dp
    }
    val gapWidth = 2.5.dp
    val glowExtra: Dp = when {
        tier >= PrestigeTier.MYTHIC -> 8.dp
        tier >= PrestigeTier.LEGEND -> 5.dp
        tier >= PrestigeTier.GOLD -> 3.dp
        else -> 0.dp
    }
    val outerSize = 80.dp + (borderWidth + gapWidth) * 2 + glowExtra * 2

    val colors = tierColors(tier)
    val rotationDurationMs: Int? = when (tier) {
        PrestigeTier.BRONZE -> null
        PrestigeTier.SILVER -> 6000
        PrestigeTier.GOLD -> 3500
        PrestigeTier.DIAMOND -> 2500
        PrestigeTier.LEGEND -> 2000
        PrestigeTier.MYTHIC -> 1200
        PrestigeTier.NONE -> null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "prestige_ring")

    val rotation by if (rotationDurationMs != null) {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(rotationDurationMs, easing = LinearEasing)),
            label = "ring_rot"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val pulse by if (tier >= PrestigeTier.GOLD) {
        infiniteTransition.animateFloat(
            initialValue = 0.45f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ),
            label = "ring_pulse"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val avatarRadius = 40.dp.toPx()
            val ringRadius = avatarRadius + gapWidth.toPx() + borderWidth.toPx() / 2f

            // Glow layers (GOLD+)
            if (tier >= PrestigeTier.GOLD) {
                val glowColor = colors.first()
                for (i in 0 until 3) {
                    val alpha = pulse * (0.28f - i * 0.07f).coerceAtLeast(0.02f)
                    val strokePx = (2.5f - i * 0.6f).coerceAtLeast(0.5f).dp.toPx()
                    drawCircle(
                        color = glowColor.copy(alpha = alpha),
                        radius = ringRadius + (i + 1) * 3.5.dp.toPx(),
                        center = center,
                        style = Stroke(width = strokePx)
                    )
                }
            }

            // Primary animated ring
            rotate(rotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors,
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = borderWidth.toPx())
                )
            }

            // Counter-rotating outer ring (LEGEND+)
            if (tier >= PrestigeTier.LEGEND) {
                rotate(-(rotation * 0.65f), pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors.reversed(),
                            center = Offset(size.width / 2f, size.height / 2f)
                        ),
                        radius = ringRadius + borderWidth.toPx() + 1.5.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // Surface-colored gap ring separating the animated border from the avatar
        Box(
            modifier = Modifier
                .size(80.dp + gapWidth * 2)
                .clip(CircleShape)
                .background(surfaceColor)
        )

        // 80 dp avatar area — content lambda is scoped here
        Box(modifier = Modifier.size(80.dp), content = content)
    }
}

/**
 * Overlay composable for the profile hero banner area.
 * Adds shimmer sweep (GOLD+) and floating colour particles (DIAMOND+).
 * Place this as the first child inside the hero Box so it renders behind other elements.
 */
@Composable
fun PrestigeHeroOverlay(tier: PrestigeTier, modifier: Modifier = Modifier) {
    if (tier < PrestigeTier.GOLD) return

    val colors = tierColors(tier)
    val particleCount = when {
        tier >= PrestigeTier.MYTHIC -> 14
        tier >= PrestigeTier.LEGEND -> 10
        tier >= PrestigeTier.DIAMOND -> 6
        else -> 0
    }

    val infiniteTransition = rememberInfiniteTransition(label = "prestige_hero")

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "shimmer"
    )

    val particleTime by if (particleCount > 0) {
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
            label = "particles"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val particleXFracs = remember(particleCount) {
        FloatArray(particleCount) { i -> 0.05f + (i.toFloat() / particleCount.coerceAtLeast(1)) * 0.9f }
    }
    val particleSizes = remember(particleCount) {
        FloatArray(particleCount) { i -> 2f + (i % 4) * 1.3f }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Shimmer sweep
        val shimmerX = size.width * shimmerProgress
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.45f to Color.White.copy(alpha = 0.14f),
                0.55f to Color.White.copy(alpha = 0.18f),
                1f to Color.Transparent,
                startX = shimmerX - 80.dp.toPx(),
                endX = shimmerX + 80.dp.toPx()
            )
        )

        // Floating particles
        for (i in 0 until particleCount) {
            val phase = i.toFloat() / particleCount
            val t = (particleTime + phase) % 1f
            val x = size.width * particleXFracs[i]
            val y = size.height * (1f - t)
            val alpha = when {
                t < 0.15f -> t / 0.15f
                t > 0.82f -> (1f - t) / 0.18f
                else -> 1f
            }.coerceIn(0f, 1f) * 0.75f

            drawCircle(
                color = colors[i % colors.size].copy(alpha = alpha),
                radius = particleSizes[i].dp.toPx(),
                center = Offset(x, y)
            )
            // White core sparkle (LEGEND+)
            if (tier >= PrestigeTier.LEGEND && i % 2 == 0) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.85f),
                    radius = particleSizes[i].dp.toPx() * 0.42f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
