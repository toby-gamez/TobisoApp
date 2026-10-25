package com.tobiso.tobisoappnative.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Soft "settle in" entrance: fades, rises a little and scales up with a gentle spring. Plays once per [key]. */
fun Modifier.cozyEntrance(key: Any? = null, index: Int = 0, stepMs: Long = 45): Modifier = composed {
    val key = key ?: currentCompositeKeyHash
    var shown by rememberSaveable(key) { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "cozyEntrance"
    )
    LaunchedEffect(key) {
        if (!shown) {
            delay((index.coerceAtMost(8)) * stepMs)
            shown = true
        }
    }
    graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 36.dp.toPx()
        val s = 0.94f + 0.06f * p
        scaleX = s
        scaleY = s
    }
}

/** Gentle squish while pressed. Observes touches without consuming them, so clicks and scrolling keep working. */
fun Modifier.cozyPress(pressedScale: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "cozyPress"
    )
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(PointerEventPass.Initial)
                pressed = false
            }
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Soft icon/content swap: the old one shrinks and fades while the new one springs in. */
@androidx.compose.runtime.Composable
fun <T> CozySwap(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @androidx.compose.runtime.Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)) +
                androidx.compose.animation.scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f))
                .togetherWith(
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
                        androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(120), targetScale = 0.6f)
                )
        },
        label = "cozySwap"
    ) { content(it) }
}

/**
 * Pill-shaped tab switcher with a sliding, springy indicator. Use this instead of FilterChips when the
 * options are mutually exclusive views of the same screen.
 */
@androidx.compose.runtime.Composable
fun CozyTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val pad = 4.dp
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(scheme.surfaceContainerHigh)
            .padding(pad)
    ) {
        val tabWidth = maxWidth / labels.size.coerceAtLeast(1)
        val offset by androidx.compose.animation.core.animateDpAsState(
            tabWidth * selectedIndex,
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "cozyTabOffset"
        )
        Box(
            Modifier
                .offset(x = offset)
                .size(tabWidth, maxHeight)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(scheme.primaryContainer)
        )
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val color by androidx.compose.animation.animateColorAsState(
                    if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    androidx.compose.animation.core.tween(250), label = "cozyTabColor"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        label, color = color, fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1
                    )
                }
            }
        }
    }
}
