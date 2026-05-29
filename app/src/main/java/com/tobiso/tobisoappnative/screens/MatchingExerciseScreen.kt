package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.matching.MatchingExerciseIntent
import com.tobiso.tobisoappnative.viewmodel.matching.MatchingExerciseEffect
import com.tobiso.tobisoappnative.viewmodel.matching.MatchingExerciseViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.saveable.rememberSaveable
import com.tobiso.tobisoappnative.model.*
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.components.ContentRenderer
import com.tobiso.tobisoappnative.components.ExerciseLoadingContent
import com.tobiso.tobisoappnative.components.FullScreenPointsOverlay
import com.tobiso.tobisoappnative.components.parseContentToElements
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingExerciseScreen(
    exerciseId: Int,
    navController: NavController,
    ttsViewModel: TtsViewModel
) {
    val vm: MatchingExerciseViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val totalPoints by PointsManager.instance.totalPoints.collectAsState()
    var pointsAwarded by rememberSaveable { mutableStateOf(false) }
    var showPointsOverlay by rememberSaveable { mutableStateOf(false) }
    var awardedPoints by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(state.showResult) {
        if (state.showResult && !pointsAwarded) {
            val score = state.validationResult?.score ?: 0
            if (score > 0) {
                val points = score / 10
                PointsManager.instance.addPoints(points)
                awardedPoints = points
                pointsAwarded = true
                showPointsOverlay = true
            }
        }
    }

    LaunchedEffect(showPointsOverlay) {
        if (showPointsOverlay) {
            kotlinx.coroutines.delay(2500)
            showPointsOverlay = false
        }
    }

    // Load exercise on first composition
    LaunchedEffect(exerciseId) {
        vm.onIntent(MatchingExerciseIntent.Load(exerciseId))
    }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        vm.effect.collectLatest { effect ->
            when (effect) {
                is MatchingExerciseEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
                MatchingExerciseEffect.NavigateBack ->
                    navController.popBackStack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.exerciseTitle.ifBlank { "Matching cvičení" }, style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
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

            // Vytvořené páry
            if (state.pairs.isNotEmpty()) {
                Text(
                    "Vytvořené páry (kliknutím odstraníte):",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.pairs) { pair ->
                        val leftItem = state.config?.left?.find { it.id == pair.leftId }
                        val rightItem = state.config?.right?.find { it.id == pair.rightId }
                        val pairResult = if (state.showResult)
                            state.validationResult?.detailedResults?.get(pair.leftId) else null
                        val pairColor = when (pairResult) {
                            true -> MaterialTheme.colorScheme.tertiaryContainer
                            false -> MaterialTheme.colorScheme.errorContainer
                            null -> MaterialTheme.colorScheme.surface
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.onIntent(MatchingExerciseIntent.RemovePair(pair))
                                },
                            colors = CardDefaults.outlinedCardColors(containerColor = pairColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = leftItem?.text ?: "?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = " ↔ ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = rightItem?.text ?: "?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Levé a pravé položky
            state.config?.let { config ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Levý sloupec
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            "Levá strana:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(config.left.filter { leftItem ->
                                state.pairs.none { it.leftId == leftItem.id }
                            }) { item ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.onIntent(MatchingExerciseIntent.SelectLeft(item.id))
                                        },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (state.selectedLeft == item.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Pravý sloupec
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            "Pravá strana:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(config.right.filter { rightItem ->
                                state.pairs.none { it.rightId == rightItem.id }
                            }) { item ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.onIntent(MatchingExerciseIntent.SelectRight(item.id))
                                        },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (state.selectedRight == item.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { vm.onIntent(MatchingExerciseIntent.Reset) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resetovat")
            }

            // Tlačítko kontroly
            Button(
                onClick = { vm.onIntent(MatchingExerciseIntent.Validate(exerciseId)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = state.pairs.isNotEmpty() && !state.isValidating && !state.isOffline
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
            val validationResult = state.validationResult
            if (state.showResult && validationResult != null) {
                val result = validationResult
                val isCorrect = result.isCorrect
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.errorContainer
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
                            text = "Skóre: ${result.score}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (result.feedback.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.feedback,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        result.explanation?.let { explanation ->
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

    if (showPointsOverlay && awardedPoints > 0) {
        FullScreenPointsOverlay(
            points = awardedPoints,
            totalPoints = totalPoints
        )
    }
    } // Box
}
