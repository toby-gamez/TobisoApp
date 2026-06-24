package com.tobiso.tobisoappnative.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FullScreenPointsOverlay(points: Int, totalPointsFloat: Float) {
    // Stavy pro animace
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    // Spuštění animací při zobrazení komponenty
    LaunchedEffect(Unit) {
        startAnimations = true
        delay(1800) // Nechá animace dokončit
        startFadeOut = true
    }

    // Animace pro fade-in/out efekt celého overlay
    val alpha by animateFloatAsState(
        targetValue = when {
            startFadeOut -> 0f
            startAnimations -> 1f
            else -> 0f
        },
        animationSpec = if (startFadeOut) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "alpha"
    )

    // Animace pro scale efekt hlavního textu
    val textScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "textScale"
    )

    // Animace pro sekundární text s delay
    val secondaryAlpha by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "secondaryAlpha"
    )

    // Animace pro kruhový efekt ze středu
    val circleScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "circleScale"
    )

    // primaryContainer barvy pro kruh - stejné jako nový design
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Tlustý prstenec otevírající se ze středu na pozadí
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(circleScale)
                .border(
                    width = 40.dp,
                    color = primaryContainerColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Hlavní text s animací
            Text(
                text = "+$points bodů!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(textScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sekundární text s delayed animací a hvězdičkou
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(secondaryAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Body",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Celkem: ${formatPointsBalance(totalPointsFloat)} bodů",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FullScreenMilestoneOverlay(points: Int, totalPointsFloat: Float, milestoneDay: Int) {
    // Stavy pro animace
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    // Spuštění animací při zobrazení komponenty
    LaunchedEffect(Unit) {
        startAnimations = true
        delay(2200) // Delší delay pro milník
        startFadeOut = true
    }

    // Animace pro fade-in/out efekt celého overlay
    val alpha by animateFloatAsState(
        targetValue = when {
            startFadeOut -> 0f
            startAnimations -> 1f
            else -> 0f
        },
        animationSpec = if (startFadeOut) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "alpha"
    )

    // Animace pro scale efekt hlavního textu
    val textScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "textScale"
    )

    // Animace pro sekundární text s delay
    val secondaryAlpha by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "secondaryAlpha"
    )

    // Animace pro kruhový efekt ze středu
    val circleScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "circleScale"
    )

    // Speciální barva pro milníky - primaryContainer jako pozadí
    val milestoneColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Speciální kruh pro milníky
        Box(
            modifier = Modifier
                .size(450.dp)
                .scale(circleScale)
                .border(
                    width = 50.dp,
                    color = primaryContainerColor.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Emoji nebo ikona milníku
            Text(
                text = "🎉",
                fontSize = 64.sp,
                modifier = Modifier
                    .scale(textScale)
                    .alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hlavní text milníku
            Text(
                text = "Milník dosažen!",
                color = milestoneColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(textScale)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Text s počtem dní
            Text(
                text = "$milestoneDay dní v řadě",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body získané za milník
            Text(
                text = "+$points bodů!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Celkový počet bodů s hvězdičkou
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(secondaryAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Body",
                    tint = milestoneColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Celkem: ${formatPointsBalance(totalPointsFloat)} bodů",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FullScreenTotalPointsOverlay(totalPointsFloat: Float) {
    // Stejná animace a vzhled jako FullScreenPointsOverlay, ale pouze číslo bodů
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimations = true
        delay(1800)
        startFadeOut = true
    }

    val alpha by animateFloatAsState(
        targetValue = when {
            startFadeOut -> 0f
            startAnimations -> 1f
            else -> 0f
        },
        animationSpec = if (startFadeOut) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "alpha"
    )

    val textScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "textScale"
    )

    val circleScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "circleScale"
    )

    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(circleScale)
                .border(
                    width = 40.dp,
                    color = primaryContainerColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
        )
        
        // Číslo s hvězdičkou
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.scale(textScale)
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Body",
                tint = primaryColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatPointsBalance(totalPointsFloat),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FullScreenPrestigeTierOverlay(tier: PrestigeTier, totalEarnedPoints: Int) {
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimations = true
        delay(4000)
        startFadeOut = true
    }

    val alpha by animateFloatAsState(
        targetValue = when { startFadeOut -> 0f; startAnimations -> 1f; else -> 0f },
        animationSpec = if (startFadeOut) tween(600, easing = FastOutSlowInEasing)
                        else tween(350, easing = FastOutSlowInEasing),
        label = "prestige_alpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0.25f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "prestige_scale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(500, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "prestige_label_alpha"
    )

    val colors = tierColors(tier)

    val infiniteTransition = rememberInfiniteTransition(label = "prestige_overlay")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "overlay_rot"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "overlay_pulse"
    )
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "overlay_particles"
    )

    val particleCount = when {
        tier >= PrestigeTier.MYTHIC -> 20
        tier >= PrestigeTier.LEGEND -> 15
        tier >= PrestigeTier.DIAMOND -> 10
        else -> 6
    }
    val particleXFracs = remember(particleCount) {
        FloatArray(particleCount) { i -> 0.04f + (i.toFloat() / particleCount) * 0.92f }
    }
    val particleSizes = remember(particleCount) {
        FloatArray(particleCount) { i -> 2.5f + (i % 4) * 1.5f }
    }

    Box(
        modifier = Modifier.fillMaxSize().alpha(alpha).background(Color(0xFF070708)),
        contentAlignment = Alignment.Center
    ) {
        // Background radial glow
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to colors.first().copy(alpha = 0.22f),
                    0.55f to colors.last().copy(alpha = 0.08f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.85f
                ),
                radius = size.minDimension * 0.85f,
                center = Offset(cx, cy)
            )
        }

        // Floating particles
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until particleCount) {
                val phase = i.toFloat() / particleCount
                val t = (particleTime + phase) % 1f
                val x = size.width * particleXFracs[i]
                val y = size.height * (1f - t)
                val a = when {
                    t < 0.12f -> t / 0.12f
                    t > 0.82f -> (1f - t) / 0.18f
                    else -> 1f
                }.coerceIn(0f, 1f) * 0.8f
                drawCircle(colors[i % colors.size].copy(alpha = a), particleSizes[i].dp.toPx(), Offset(x, y))
                if (tier >= PrestigeTier.GOLD && i % 2 == 0) {
                    drawCircle(Color.White.copy(alpha = a * 0.7f), particleSizes[i].dp.toPx() * 0.38f, Offset(x, y))
                }
            }
        }

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(contentScale).padding(horizontal = 32.dp)
        ) {
            // Large animated ring with tier emoji inside
            Box(modifier = Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val ringRadius = size.minDimension / 2f - 10.dp.toPx()

                    // Glow rings
                    for (i in 0 until 4) {
                        drawCircle(
                            color = colors.first().copy(alpha = pulse * (0.30f - i * 0.06f).coerceAtLeast(0.01f)),
                            radius = ringRadius + (i + 1) * 4.5.dp.toPx(),
                            center = center,
                            style = Stroke((3f - i * 0.5f).coerceAtLeast(0.5f).dp.toPx())
                        )
                    }
                    // Primary ring
                    rotate(rotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(colors, Offset(size.width / 2f, size.height / 2f)),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(10.dp.toPx())
                        )
                    }
                    // Counter-rotating inner ring
                    rotate(-(rotation * 0.65f), pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(colors.reversed(), Offset(size.width / 2f, size.height / 2f)),
                            radius = ringRadius - 14.dp.toPx(),
                            center = center,
                            style = Stroke(2.5.dp.toPx())
                        )
                    }
                }
                Text(tierEmoji(tier), fontSize = 56.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "NOVÁ PRESTIŽ",
                color = colors.first(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(labelAlpha)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = tier.label,
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = tierDescription(tier),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(labelAlpha)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(labelAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = colors.first(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Celkem: $totalEarnedPoints bodů",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Shown when the user's point balance crosses 100 000 and is reset to 0.
 * [newDeflationDivisor] is the divisor now in effect (10, 100, …).
 */
@Composable
fun FullScreenPointsResetOverlay(newDeflationDivisor: Int) {
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimations = true
        delay(4200)
        startFadeOut = true
    }

    val alpha by animateFloatAsState(
        targetValue = when { startFadeOut -> 0f; startAnimations -> 1f; else -> 0f },
        animationSpec = if (startFadeOut) tween(600, easing = FastOutSlowInEasing)
                        else tween(350, easing = FastOutSlowInEasing),
        label = "reset_alpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "reset_scale"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(500, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "reset_detail_alpha"
    )

    // Animated ring pulse
    val infiniteTransition = rememberInfiniteTransition(label = "reset_ring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reset_pulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "reset_rot"
    )
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "reset_particles"
    )

    // Fiery orange/red palette for the reset overlay
    val fireColors = listOf(
        Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFEB3B),
        Color(0xFFFF9800), Color(0xFFFF5722)
    )
    val particleCount = 12
    val particleXFracs = remember { FloatArray(particleCount) { i -> 0.04f + i.toFloat() / particleCount * 0.92f } }
    val particleSizes = remember { FloatArray(particleCount) { i -> 2.5f + (i % 4) * 1.3f } }

    Box(
        modifier = Modifier.fillMaxSize().alpha(alpha).background(Color(0xFF0D0500)),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFFF5722).copy(alpha = 0.28f),
                    0.5f to Color(0xFFFF9800).copy(alpha = 0.10f),
                    1f to Color.Transparent,
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.80f
                ),
                radius = size.minDimension * 0.80f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        // Falling embers
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until particleCount) {
                val phase = i.toFloat() / particleCount
                val t = (particleTime + phase) % 1f
                val x = size.width * particleXFracs[i]
                // embers fall downward (reversed from floating particles)
                val y = size.height * t
                val a = when {
                    t < 0.12f -> t / 0.12f
                    t > 0.82f -> (1f - t) / 0.18f
                    else -> 1f
                }.coerceIn(0f, 1f) * 0.85f
                drawCircle(fireColors[i % fireColors.size].copy(alpha = a), particleSizes[i].dp.toPx(), Offset(x, y))
                if (i % 3 == 0) {
                    drawCircle(Color.White.copy(alpha = a * 0.6f), particleSizes[i].dp.toPx() * 0.35f, Offset(x, y))
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(contentScale).padding(horizontal = 32.dp)
        ) {
            // Pulsing ring with icon
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val ringRadius = size.minDimension / 2f - 8.dp.toPx()
                    for (i in 0 until 3) {
                        drawCircle(
                            color = Color(0xFFFF5722).copy(alpha = pulse * (0.35f - i * 0.08f).coerceAtLeast(0.02f)),
                            radius = ringRadius + (i + 1) * 5.dp.toPx(),
                            center = center,
                            style = Stroke((3f - i * 0.7f).coerceAtLeast(0.5f).dp.toPx())
                        )
                    }
                    rotate(ringRotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(fireColors, Offset(size.width / 2f, size.height / 2f)),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(8.dp.toPx())
                        )
                    }
                }
                Text("⚡", fontSize = 52.sp)
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "RESET BODŮ",
                color = Color(0xFFFF9800),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Překročil jsi 100 000!",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Tvůj zůstatek byl resetován na 0.\nBody nyní vydělávají méně — budeš muset pracovat tvrději!",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.alpha(detailAlpha)
            )

            Spacer(Modifier.height(20.dp))

            // Deflation rate badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF5722).copy(alpha = 0.20f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .alpha(detailAlpha)
            ) {
                Text(
                    text = "Míra deflace: 1 : $newDeflationDivisor",
                    color = Color(0xFFFF9800),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FullScreenAchievementOverlay(points: Int, totalPointsFloat: Float, achievementPoints: Int) {
    // Stavy pro animace
    var startAnimations by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }

    // Spuštění animací při zobrazení komponenty
    LaunchedEffect(Unit) {
        startAnimations = true
        delay(2200) // Delší delay pro achievement
        startFadeOut = true
    }

    // Animace pro fade-in/out efekt celého overlay
    val alpha by animateFloatAsState(
        targetValue = when {
            startFadeOut -> 0f
            startAnimations -> 1f
            else -> 0f
        },
        animationSpec = if (startFadeOut) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "alpha"
    )

    // Animace pro text - postupné objevení s bounce efektem
    val textScale by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "textScale"
    )

    // Animace pro sekundární text - mírně zpožděná
    val secondaryAlpha by animateFloatAsState(
        targetValue = if (startAnimations && !startFadeOut) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "secondaryAlpha"
    )

    val achievementColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Kruh na pozadí s animací
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(textScale)
                .border(
                    width = 40.dp,
                    color = achievementColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Emoji nebo ikona achievementu
            Text(
                text = "🏆",
                fontSize = 64.sp,
                modifier = Modifier
                    .scale(textScale)
                    .alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hlavní text achievementu
            Text(
                text = "Úspěch odemčen!",
                color = achievementColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(textScale)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Text s počtem bodů pro achievement
            Text(
                text = "$achievementPoints celkových bodů",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body získané za achievement
            Text(
                text = "+$points bodů!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(secondaryAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Celkový počet bodů s hvězdičkou
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(secondaryAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Body",
                    tint = achievementColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Celkem: ${formatPointsBalance(totalPointsFloat)} bodů",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}