package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import com.example.tobisoappnative.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingExerciseScreen(
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

    var matchingConfig by remember { mutableStateOf<MatchingConfig?>(null) }
    var pairs by remember { mutableStateOf<List<MatchingPair>>(emptyList()) }
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var selectedRight by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    LaunchedEffect(currentExercise) {
        currentExercise?.let { exercise ->
            matchingConfig = null
            parseError = null
            try {
                val raw = exercise.configJson
                if (raw.isBlank() || raw == "null") {
                    parseError = "Konfigurace cvičení je prázdná"
                    return@let
                }

                val config = json.decodeFromString<MatchingConfig>(raw)
                matchingConfig = config
                pairs = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("MatchingExercise", "Error parsing config", e)
                parseError = e.message ?: "Neznámá chyba při parsování konfigurace"
            }
        }
    }

    // Pomocná funkce pro vytvoření páru
    fun tryCreatePair() {
        if (selectedLeft != null && selectedRight != null) {
            pairs = pairs + MatchingPair(selectedLeft!!, selectedRight!!)
            selectedLeft = null
            selectedRight = null
            showResult = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentExercise?.title ?: "Matching cvičení") },
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

            // Vytvořené páry
            if (pairs.isNotEmpty()) {
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
                    items(pairs) { pair ->
                        val leftItem = matchingConfig?.left?.find { it.id == pair.leftId }
                        val rightItem = matchingConfig?.right?.find { it.id == pair.rightId }
                        
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Odstranit pár
                                    pairs = pairs.filter { 
                                        it.leftId != pair.leftId || it.rightId != pair.rightId 
                                    }
                                    showResult = false
                                }
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
            matchingConfig?.let { config ->
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(config.left.filter { leftItem ->
                                pairs.none { it.leftId == leftItem.id }
                            }) { item ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLeft = if (selectedLeft == item.id) null else item.id
                                            tryCreatePair()
                                        },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (selectedLeft == item.id)
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(config.right.filter { rightItem ->
                                pairs.none { it.rightId == rightItem.id }
                            }) { item ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRight = if (selectedRight == item.id) null else item.id
                                            tryCreatePair()
                                        },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (selectedRight == item.id)
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
                onClick = {
                    if (pairs.isNotEmpty()) {
                        val solution = MatchingSolution(pairs)
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
                enabled = pairs.isNotEmpty() && !validationLoading && !isOffline
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
