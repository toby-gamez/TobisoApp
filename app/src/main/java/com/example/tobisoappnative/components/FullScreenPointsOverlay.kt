package com.example.tobisoappnative.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FullScreenPointsOverlay(points: Int, totalPoints: Int) {
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

    // Terciární barvy pro kruh
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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
                    color = tertiaryColor.copy(alpha = 0.1f),
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

            // Sekundární text s delayed animací
            Text(
                text = "Celkem: $totalPoints bodů",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(secondaryAlpha)
            )
        }
    }
}

@Composable
fun FullScreenTotalPointsOverlay(totalPoints: Int) {
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

    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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
                    color = tertiaryColor.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
        )
        Text(
            text = totalPoints.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(textScale)
        )
    }
}