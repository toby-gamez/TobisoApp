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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.border

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import com.example.tobisoappnative.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineExerciseScreen(
    exerciseId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val currentExercise by viewModel.currentExercise.collectAsState()
    val exerciseLoading by viewModel.exercisesLoading.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val validationLoading by viewModel.validationLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    val json = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    var timelineConfig by remember { mutableStateOf<TimelineConfig?>(null) }
    var orderedEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableEvents by remember { mutableStateOf<List<TimelineEvent>>(emptyList()) }
    var showResult by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    val slotYears = remember(timelineConfig) {
        timelineConfig?.events?.mapNotNull { it.year }?.sorted() ?: emptyList()
    }

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    LaunchedEffect(currentExercise) {
        currentExercise?.let { exercise ->
            timelineConfig = null
            parseError = null
            try {
                val raw = exercise.configJson
                if (raw.isBlank() || raw == "null") {
                    parseError = "Konfigurace cvičení je prázdná"
                    return@let
                }

                val config = json.decodeFromString<TimelineConfig>(raw)
                timelineConfig = config
                availableEvents = config.events
                orderedEvents = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("TimelineExercise", "Error parsing config", e)
                parseError = e.message ?: "Neznámá chyba při parsování konfigurace"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentExercise?.title ?: "Timeline cvičení") },
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
            if (exerciseLoading && currentExercise == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (isOffline) {
                Text(
                    text = "Offline režim: cvičení lze vyplnit, ale kontrola vyžaduje internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (!parseError.isNullOrBlank()) {
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
                            text = parseError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        currentExercise?.type?.let { t ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Typ: $t",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        currentExercise?.configJson?.let { raw ->
                            val preview = raw.take(220)
                            if (preview.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Config (začátek): $preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Instrukce
            currentExercise?.instructionsMarkdown?.let { instructions ->
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

            // Časová osa (vizualizace rozsahu let)
            timelineConfig?.let { config ->
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
                        // Jednoduchá vizuální linie + kolečka podle letopočtů
                        val timeRange = (config.timeRange.end - config.timeRange.start).coerceAtLeast(1)
                        val assignedSlotYears = orderedEvents.mapIndexedNotNull { index, _ ->
                            slotYears.getOrNull(index)
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
                        items(slotYears.size) { index ->
                            val year = slotYears[index]
                            val eventId = orderedEvents.getOrNull(index)
                            val event = eventId?.let { id -> availableEvents.find { it.id == id } }
                            val isAssigned = event != null
                            if (isAssigned) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            orderedEvents = orderedEvents.filter { id -> id != eventId }
                                            showResult = false
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
                                            text = event?.label ?: "(nepřiřazeno)",
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
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableEvents.filter { !orderedEvents.contains(it.id) }) { event ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Přidat do orderedEvents (přiřadí se do dalšího volného roku)
                                        if (orderedEvents.size < slotYears.size) {
                                            orderedEvents = orderedEvents + event.id
                                            showResult = false
                                        }
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

            // Tlačítko kontroly
            Button(
                onClick = {
                    if (orderedEvents.isNotEmpty()) {
                        val solution = TimelineSolution(orderedEvents)
                        val solutionJson = json.encodeToString(solution)
                        viewModel.validateExercise(
                            exerciseId = exerciseId,
                            userSolutionJson = solutionJson,
                            onSuccess = { showResult = true },
                            onError = { showResult = true }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = orderedEvents.isNotEmpty() && !validationLoading && !isOffline
            ) {
                if (validationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Zkontrolovat")
                }
            }

            // Výsledek validace
            if (showResult && validationResult != null) {
                val isCorrect = validationResult?.isCorrect == true
                val successContainer = Color(0xFFE8F5E9)
                val onSuccessContainer = Color(0xFF1B5E20)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) successContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isCorrect) "Správně!" else "Nesprávně",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect) onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Skóre: ${validationResult?.score ?: 0}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        validationResult?.feedback?.let { feedback ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        validationResult?.explanation?.let { explanation ->
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
}
