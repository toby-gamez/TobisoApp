package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.components.ContentRenderer
import com.tobiso.tobisoappnative.components.ExerciseLoadingContent
import com.tobiso.tobisoappnative.components.parseContentToElements
import com.tobiso.tobisoappnative.viewmodel.timeline.TimelineExerciseEffect
import com.tobiso.tobisoappnative.viewmodel.timeline.TimelineExerciseIntent
import com.tobiso.tobisoappnative.viewmodel.timeline.TimelineExerciseViewModel
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineExerciseScreen(
    exerciseId: Int,
    navController: NavController,
    ttsViewModel: TtsViewModel
) {
    val vm: TimelineExerciseViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var pointsAwarded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.showResult) {
        if (state.showResult && !pointsAwarded) {
            val score = state.validationResult?.score ?: 0
            if (score > 0) {
                val points = score / 10
                PointsManager.instance.addPoints(points)
                pointsAwarded = true
            }
        }
    }

    LaunchedEffect(exerciseId) {
        vm.onIntent(TimelineExerciseIntent.Load(exerciseId))
    }

    LaunchedEffect(Unit) {
        vm.effect.collectLatest { effect ->
            when (effect) {
                is TimelineExerciseEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                TimelineExerciseEffect.NavigateBack -> navController.popBackStack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.exerciseTitle.ifEmpty { "Timeline cvičení" }, style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (!state.instructionsMarkdown.isNullOrEmpty()) {
                        IconButton(onClick = { ttsViewModel.speak(state.instructionsMarkdown ?: "") }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Číst nahlas")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ExerciseLoadingContent(
                isLoading = state.isLoading,
                isOffline = state.isOffline,
                error = state.error
            )

            // Instrukce
            state.instructionsMarkdown?.let { instructions ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val contentElements = parseContentToElements(instructions, isOffline = state.isOffline, posts = emptyList())
                        ContentRenderer(
                            contentElements = contentElements,
                            isOffline = state.isOffline,
                            posts = emptyList(),
                            addendums = emptyList(),
                            navController = navController,
                            onAddendumSelected = {}
                        )
                    }
                }
            }

            // Časová osa (vizualizace rozsahu let)
            state.config?.let { config ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Časová osa: ${config.timeRange.start} - ${config.timeRange.end}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val timeRange = (config.timeRange.end - config.timeRange.start).coerceAtLeast(1)
                        val assignedSlotYears = state.orderedEvents.mapIndexedNotNull { index, _ ->
                            state.slotYears.getOrNull(index)
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        ) {
                            val dotSize = 10.dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )

                            assignedSlotYears.forEach { year ->
                                val fraction = ((year - config.timeRange.start).toFloat() / timeRange)
                                    .coerceIn(0f, 1f)
                                val xOffset = (maxWidth - dotSize) * fraction
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .offset(x = xOffset)
                                        .align(Alignment.CenterStart)
                                        .background(
                                            MaterialTheme.colorScheme.secondary,
                                            RoundedCornerShape(50)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = config.timeRange.start.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = config.timeRange.end.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Seřazené události
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Přiřazené události k rokům (kliknutím zrušíte):",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.slotYears.size) { index ->
                            val year = state.slotYears[index]
                            val eventId = state.orderedEvents.getOrNull(index)
                            val event = eventId?.let { id -> state.config?.events?.find { it.id == id } }
                            val isAssigned = event != null
                            if (isAssigned) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.onIntent(TimelineExerciseIntent.RemoveEvent(eventId))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$year:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = event.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$year:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dostupné události
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dostupné události (kliknutím přidáte):",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val shuffledAvailableEvents = remember(state.availableEvents) {
                        state.availableEvents.shuffled()
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shuffledAvailableEvents.filter { !state.orderedEvents.contains(it.id) }) { event ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.onIntent(TimelineExerciseIntent.AddEvent(event.id))
                                    }
                            ) {
                                Text(
                                    text = event.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { vm.onIntent(TimelineExerciseIntent.Reset) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resetovat")
            }

            // Tlačítko kontroly
            Button(
                onClick = { vm.onIntent(TimelineExerciseIntent.Validate(exerciseId)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = state.orderedEvents.isNotEmpty() && !state.isValidating && !state.isOffline
            ) {
                if (state.isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Zkontrolovat")
                }
            }

            // Výsledek validace
            if (state.showResult && state.validationResult != null) {
                val isCorrect = state.validationResult?.isCorrect == true
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isCorrect) "Správně!" else "Nesprávně",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Skóre: ${state.validationResult?.score ?: 0}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        state.validationResult?.feedback?.let { feedback ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        state.validationResult?.explanation?.let { explanation ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    } // Box
}
