package com.tobiso.tobisoappnative.components

import androidx.compose.animation.core.*
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
                    text = "Celkem: $totalPoints bodů",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FullScreenMilestoneOverlay(points: Int, totalPoints: Int, milestoneDay: Int) {
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
                    text = "Celkem: $totalPoints bodů",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
                text = totalPoints.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FullScreenAchievementOverlay(points: Int, totalPoints: Int, achievementPoints: Int) {
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
                    text = "Celkem: $totalPoints bodů",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}