package com.example.tobisoappnative.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tobisoappnative.tts.TtsManager
import com.example.tobisoappnative.tts.TtsState
import com.example.tobisoappnative.tts.TtsStatus
import com.example.tobisoappnative.utils.TextUtils
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun TtsPlayer(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val ttsStatus by ttsManager.status.collectAsState()
    
    AnimatedVisibility(
        visible = ttsStatus.state != TtsState.IDLE && ttsStatus.currentText.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Progress bar
                if (ttsStatus.progress > 0f) {
                    LinearProgressIndicator(
                        progress = ttsStatus.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Segment info
                    if (ttsStatus.totalSegments > 1) {
                        Text(
                            text = "Segment ${ttsStatus.currentSegment} z ${ttsStatus.totalSegments}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Build segment list once per currentText
                                // maximize / minimize state for the player window
                                var isMaximized by remember { mutableStateOf(true) }
                val allSegments = remember(ttsStatus.currentText) {
                    if (ttsStatus.currentText.isNotBlank()) TextUtils.splitTextForTts(ttsStatus.currentText) else emptyList()
                }

                // Use LazyColumn + LazyListState for precise auto-scrolling to the current segment
                val listState = rememberLazyListState()

                // Auto-scroll to current segment only (do not scroll gradually by words)
                LaunchedEffect(ttsStatus.currentSegment, allSegments.size) {
                    if (allSegments.isNotEmpty()) {
                        val idx = (ttsStatus.currentSegment - 1).coerceIn(0, maxOf(0, allSegments.size - 1))
                        try {
                            listState.animateScrollToItem(idx)
                        } catch (_: Exception) {
                            listState.scrollToItem(idx)
                        }
                    }
                }

                // Render the LazyColumn only when maximized
                if (isMaximized) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .heightIn(min = 64.dp, max = 200.dp)
                    ) {
                        itemsIndexed(allSegments) { index, segment ->
                            val isCurrent = index == (ttsStatus.currentSegment - 1)
                            if (isCurrent) {
                                val annotatedText = buildAnnotatedString {
                                    val words = ttsStatus.segmentWords.ifEmpty { segment.split("\\s+".toRegex()).filter { it.isNotBlank() } }
                                    for ((wIndex, word) in words.withIndex()) {
                                        if (wIndex <= ttsStatus.currentWordIndex) {
                                            withStyle(style = SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )) { append(word) }
                                        } else {
                                            withStyle(style = SpanStyle(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )) { append(word) }
                                        }
                                        if (wIndex < words.size - 1) append(" ")
                                    }
                                }

                                Text(
                                    text = annotatedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
                                        .padding(vertical = 2.dp)
                                )
                            } else {
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Controls (unchanged)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status text
                    Text(
                        text = when (ttsStatus.state) {
                            TtsState.INITIALIZING -> "Inicializuji..."
                            TtsState.SPEAKING -> "Přehrávám"
                            TtsState.PAUSED -> "Pozastaveno"
                            TtsState.ERROR -> ttsStatus.errorMessage ?: "Chyba"
                            TtsState.STOPPED -> "Zastaveno"
                            TtsState.IDLE -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ttsStatus.state == TtsState.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Previous segment button
                        if (ttsStatus.totalSegments > 1) {
                            IconButton(
                                onClick = { viewModel.skipToPreviousSegment() },
                                enabled = ttsStatus.currentSegment > 1
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "Předchozí segment",
                                    tint = if (ttsStatus.currentSegment > 1)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        when (ttsStatus.state) {
                            TtsState.SPEAKING -> {
                                IconButton(
                                    onClick = { ttsManager.pause() }
                                ) {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = "Pozastavit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TtsState.PAUSED -> {
                                IconButton(
                                    onClick = { ttsManager.resume() }
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Pokračovat",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TtsState.INITIALIZING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                IconButton(
                                    onClick = {
                                        if (ttsStatus.currentText.isNotEmpty()) {
                                            ttsManager.speak(ttsStatus.currentText)
                                        }
                                    },
                                    enabled = ttsStatus.isInitialized
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Přehrát",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Next segment button
                        if (ttsStatus.totalSegments > 1) {
                            IconButton(
                                onClick = { viewModel.skipToNextSegment() },
                                enabled = ttsStatus.currentSegment < ttsStatus.totalSegments
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Další segment",
                                    tint = if (ttsStatus.currentSegment < ttsStatus.totalSegments)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Toggle maximize/minimize
                        IconButton(
                            onClick = { isMaximized = !isMaximized }
                        ) {
                            Icon(
                                if (isMaximized) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isMaximized) "Minimalizovat" else "Maximalizovat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stop button
                        IconButton(
                            onClick = { ttsManager.stop() },
                            enabled = ttsStatus.state != TtsState.IDLE && ttsStatus.state != TtsState.INITIALIZING
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Zastavit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun TtsSpeedControl(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    var speed by remember { mutableStateOf(1.0f) }
    var showSpeedControl by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        TextButton(
            onClick = { showSpeedControl = !showSpeedControl }
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = "Rychlost řeči"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("${String.format("%.1f", speed)}x")
        }
        
        AnimatedVisibility(visible = showSpeedControl) {
            Card(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Rychlost řeči",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Slider(
                        value = speed,
                        onValueChange = { newSpeed ->
                            speed = newSpeed
                            ttsManager.setSpeed(newSpeed)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.5x", style = MaterialTheme.typography.bodySmall)
                        Text("2.0x", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
}