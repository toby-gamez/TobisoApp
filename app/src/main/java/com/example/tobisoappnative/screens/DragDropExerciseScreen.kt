package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.dragdrop.DragDropExerciseIntent
import com.example.tobisoappnative.viewmodel.dragdrop.DragDropExerciseViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.tobisoappnative.model.*
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragDropExerciseScreen(
    exerciseId: Int,
    navController: NavController
) {
    val vm: DragDropExerciseViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()

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
                PointsManager.addPoints(points)
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

    LaunchedEffect(exerciseId) {
        vm.onIntent(DragDropExerciseIntent.Load(exerciseId))
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.exerciseTitle.ifEmpty { "Drag & Drop cvičení" }) },
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

            // Kategorie (koše)
            state.config?.let { config ->
                // Kategorie
                Text(
                    "Kategorie:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(config.categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.onIntent(DragDropExerciseIntent.PlaceInCategory(category.id))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.selectedItem != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Položky v této kategorii
                                val itemsInCategory = state.placements.filter { it.value == category.id }
                                if (itemsInCategory.isEmpty()) {
                                    Text(
                                        "Prázdné",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    itemsInCategory.forEach { (itemId, _) ->
                                        val item = config.items.find { it.id == itemId }
                                        item?.let {
                                            val itemResult = if (state.showResult) state.validationResult?.detailedResults?.get(itemId) else null
                                            val cardColor = when (itemResult) {
                                                true -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                                false -> Color(0xFFFF5722).copy(alpha = 0.1f)
                                                null -> MaterialTheme.colorScheme.surface
                                            }
                                            OutlinedCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        vm.onIntent(DragDropExerciseIntent.RemoveFromCategory(itemId))
                                                    },
                                                colors = CardDefaults.outlinedCardColors(
                                                    containerColor = cardColor
                                                )
                                            ) {
                                                Text(
                                                    text = it.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dostupné položky
                Text(
                    "Dostupné položky (kliknutím vyberte, pak klikněte na kategorii):",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(config.items.filter { !state.placements.containsKey(it.id) }) { item ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.onIntent(DragDropExerciseIntent.SelectItem(item.id))
                                },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (state.selectedItem == item.id)
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

            Spacer(modifier = Modifier.height(8.dp))

            // Tlačítko kontroly
            Button(
                onClick = { vm.onIntent(DragDropExerciseIntent.Validate(exerciseId)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = state.placements.isNotEmpty() && !state.isValidating && !state.isOffline
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
                        containerColor = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f)
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

    if (showPointsOverlay && awardedPoints > 0) {
        FullScreenPointsOverlay(
            points = awardedPoints,
            totalPoints = totalPoints
        )
    }
    } // Box
}
