package com.example.tobisoappnative.screens

import android.app.Application
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.matching.MatchingExerciseIntent
import com.example.tobisoappnative.viewmodel.matching.MatchingExerciseEffect
import com.example.tobisoappnative.viewmodel.matching.MatchingExerciseViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.tobisoappnative.model.*
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingExerciseScreen(
    exerciseId: Int,
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val vm: MatchingExerciseViewModel = viewModel(
        factory = MatchingExerciseViewModel.Factory(application)
    )
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var pointsAwarded by rememberSaveable { mutableStateOf(false) }
    var showPointsOverlay by rememberSaveable { mutableStateOf(false) }
    var awardedPoints by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(state.showResult) {
        if (state.showResult && !pointsAwarded) {
            val score = state.validationResult?.score ?: 0
            if (score > 0) {
                val points = score / 10
                PointsManager.addPoints(context, points)
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
                title = { Text(state.exerciseTitle.ifBlank { "Matching cvičení" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
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
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (state.isOffline) {
                Text(
                    text = "Offline režim: cvičení lze vyplnit, ale kontrola vyžaduje internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (!state.error.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nelze načíst konfiguraci cvičení",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Instrukce
            state.instructionsMarkdown?.let { instructions ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RichText {
                            Markdown(instructions)
                        }
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
                            true -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            false -> Color(0xFFFF5722).copy(alpha = 0.1f)
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
                                            MaterialTheme.colorScheme.secondaryContainer
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
            if (state.showResult && state.validationResult != null) {
                val result = state.validationResult!!
                val isCorrect = result.isCorrect
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color(0xFFFF5722).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isCorrect) "Správně!" else "Nesprávně",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
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
