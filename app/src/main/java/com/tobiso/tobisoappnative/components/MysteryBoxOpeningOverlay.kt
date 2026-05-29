package com.tobiso.tobisoappnative.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MysteryBoxOpeningOverlay(
    rewardText: String,
    onDismiss: () -> Unit
) {
    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(200)
        phase = 1
        delay(800)
        phase = 2
        delay(2000)
        onDismiss()
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (phase > 0) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    val boxScale by animateFloatAsState(
        targetValue = when {
            phase == 0 -> 0f
            phase == 1 -> 1f
            else -> 2.5f
        },
        animationSpec = when {
            phase == 0 -> tween(300)
            phase == 1 -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
            else -> tween(350, easing = FastOutLinearInEasing)
        },
        label = "boxScale"
    )

    val boxAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 0f else 1f,
        animationSpec = tween(250),
        label = "boxAlpha"
    )

    val glowScale by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "glowScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(200),
        label = "glowAlpha"
    )

    val textScale by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "textScale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(400, delayMillis = 200),
        label = "textAlpha"
    )

    val sparkleScale by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "sparkleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overlayAlpha)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        if (phase >= 2) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(glowScale)
                    .alpha(glowAlpha)
                    .border(
                        width = 30.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(Color.Transparent, CircleShape)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Text(
                    text = "🎁",
                    fontSize = 80.sp,
                    modifier = Modifier
                        .scale(boxScale)
                        .alpha(boxAlpha)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = rewardText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(textScale)
                    .alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🎉",
                fontSize = 48.sp,
                modifier = Modifier
                    .scale(sparkleScale)
                    .alpha(textAlpha)
            )
        }
    }
}
