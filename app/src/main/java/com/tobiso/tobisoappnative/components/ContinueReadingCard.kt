package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.repository.ContinueReading

/** "Pokračovat ve čtení" — the last article that was started but not finished. */
@Composable
fun ContinueReadingCard(
    item: ContinueReading,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(600, easing = FastOutSlowInEasing)) +
            expandVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), expandFrom = Alignment.Top) +
            slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) { -it / 4 } +
            scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), initialScale = 0.92f)
    ) {
        ContinueReadingCardContent(item, onOpen, modifier)
    }
}

@Composable
private fun ContinueReadingCardContent(
    item: ContinueReading,
    onOpen: (Int) -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var filled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); filled = true }
    val progress by animateFloatAsState(
        targetValue = if (filled) item.percent / 100f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "continueProgress"
    )
    Card(
        onClick = { onOpen(item.postId) },
        modifier = modifier.fillMaxWidth().cozyPress(0.97f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("POKRAČOVAT VE ČTENÍ", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.title,
                    fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Text(
                listOfNotNull(item.categoryPath, "${item.percent} %").joinToString(" · "),
                fontSize = 13.sp,
                modifier = Modifier.alpha(0.85f)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = scheme.secondary,
                trackColor = scheme.onSecondaryContainer.copy(alpha = 0.16f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}
