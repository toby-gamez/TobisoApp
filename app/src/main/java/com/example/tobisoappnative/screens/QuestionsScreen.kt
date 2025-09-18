package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    postId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val questionsError by viewModel.questionsError.collectAsState()
    val questionsLoading by viewModel.questionsLoading.collectAsState()
    val postDetail by viewModel.postDetail.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()
    
    // Kvíz stav
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var textAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showResults by remember { mutableStateOf(false) }
    var quizStarted by remember { mutableStateOf(false) }
    var pointsAwarded by remember { mutableStateOf(false) }
    var showPointsOverlay by remember { mutableStateOf(false) }
    var awardedPoints by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val totalPoints by PointsManager.totalPoints.collectAsState()
    
    // Načtení dat při startu
    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
        viewModel.loadQuestions(postId)
    }
    
    // Vyčištění stavu při opuštění obrazovky
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearQuestions()
        }
    }
    
    val currentQuestion = if (questions.isNotEmpty() && currentQuestionIndex < questions.size && currentQuestionIndex >= 0) {
        questions[currentQuestionIndex]
    } else null
    
    val correctAnswersCount = questions.indices.count { index ->
        if (index < questions.size && index >= 0) {
            val question = questions[index]
            if (question.isTextQuestion) {
                // Pro textové otázky porovnáváme text case-insensitive
                val userText = textAnswers[index]?.trim() ?: ""
                val correctText = question.correctTextAnswer?.trim() ?: ""
                userText.equals(correctText, ignoreCase = true)
            } else {
                // Pro výběrové otázky porovnáváme index
                selectedAnswers[index] == question.correctAnswer
            }
        } else false
    }
    
    val totalQuestions = questions.size
    val scorePercentage = if (totalQuestions > 0) {
        (correctAnswersCount * 100) / totalQuestions
    } else 0
    
    // Přidání bodů po dokončení kvízu
    LaunchedEffect(showResults) {
        if (showResults && !pointsAwarded && scorePercentage > 0) {
            val points = scorePercentage / 10 // Procenta vydělená 10
            PointsManager.addPoints(context, points)
            awardedPoints = points
            pointsAwarded = true
            showPointsOverlay = true
        }
    }
    
    // Skrytí overlay po čase
    LaunchedEffect(showPointsOverlay) {
        if (showPointsOverlay) {
            kotlinx.coroutines.delay(2500) // Stejná doba jako v overlay komponentě
            showPointsOverlay = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.loadQuestions(postId)
                    isRefreshing = false
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LargeTopAppBar(
                    title = { 
                        Text(
                            text = if (showResults) "Vaše výsledky"
                            else if (quizStarted) "Prověrka (${currentQuestionIndex + 1}/$totalQuestions)"
                            else "Prověrka",
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
                        if (showResults) {
                            IconButton(onClick = { 
                                // Restartovat kvíz
                                currentQuestionIndex = 0
                                selectedAnswers = emptyMap()
                                textAnswers = emptyMap()
                                showResults = false
                                quizStarted = false
                                pointsAwarded = false // Reset pro další pokus
                                showPointsOverlay = false
                                awardedPoints = 0
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Restart kvízu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                
                when {
                    questionsError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Chyba při načítání otázek:",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    questionsError ?: "Neznámá chyba",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    questionsLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Načítání otázek...")
                            }
                        }
                    }
                    
                    questions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Pro tento článek nejsou k dispozici žádné otázky.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Zpět na článek")
                                }
                            }
                        }
                    }
                    
                    showResults -> {
                        // Zobrazení výsledků
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Celkový výsledek
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        scorePercentage >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        scorePercentage >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                                        else -> Color(0xFFFF5722).copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Váš výsledek",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "$correctAnswersCount / $totalQuestions",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$scorePercentage%",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            scorePercentage >= 80 -> Color(0xFF4CAF50)
                                            scorePercentage >= 60 -> Color(0xFFFFC107)
                                            else -> Color(0xFFFF5722)
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Detailní výsledky pro každou otázku
                            questions.forEachIndexed { index, question ->
                                val isCorrect = if (question.isTextQuestion) {
                                    val userText = textAnswers[index]?.trim() ?: ""
                                    val correctText = question.correctTextAnswer?.trim() ?: ""
                                    userText.equals(correctText, ignoreCase = true)
                                } else {
                                    val selectedAnswer = selectedAnswers[index]
                                    selectedAnswer != null && 
                                        selectedAnswer >= 0 && 
                                        selectedAnswer < question.options.size &&
                                        selectedAnswer == question.correctAnswer
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCorrect) {
                                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        } else {
                                            Color(0xFFFF5722).copy(alpha = 0.1f)
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                                contentDescription = if (isCorrect) "Správně" else "Špatně",
                                                tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Otázka ${index + 1}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            question.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (question.isTextQuestion) {
                                            // Zobrazení textových odpovědí
                                            val userText = textAnswers[index] ?: ""
                                            val correctText = question.correctTextAnswer ?: ""
                                            
                                            Text(
                                                "Vaše odpověď: $userText",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                            )
                                            
                                            if (!isCorrect) {
                                                Text(
                                                    "Správná odpověď: $correctText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        } else {
                                            // Zobrazení výběrových odpovědí
                                            val selectedAnswer = selectedAnswers[index]
                                            if (selectedAnswer != null && selectedAnswer >= 0 && selectedAnswer < question.options.size) {
                                                Text(
                                                    "Vaše odpověď: ${question.options[selectedAnswer]}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                                )
                                            }
                                            
                                            if (!isCorrect && question.correctAnswer >= 0 && question.correctAnswer < question.options.size) {
                                                Text(
                                                    "Správná odpověď: ${question.options[question.correctAnswer]}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        }
                                        
                                        // Zobrazení vysvětlení u špatných odpovědí
                                        if (!isCorrect && !question.explanation.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        "Vysvětlení:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        question.explanation!!,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    !quizStarted -> {
                        // Úvodní obrazovka
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            postDetail?.title?.let { title ->
                                Text(
                                    "Prověrka",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "ke článku: $title",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.Start // Zarovnání doleva
                                ) {
                                    Text(
                                        "Informace",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Počet otázek: $totalQuestions")
                                    val hasTextQuestions = questions.any { it.isTextQuestion }
                                    val hasMultipleChoice = questions.any { !it.isTextQuestion }
                                    val questionTypes = when {
                                        hasTextQuestions && hasMultipleChoice -> "Výběr z možností + textové odpovědi"
                                        hasTextQuestions -> "Textové odpovědi" 
                                        else -> "Výběr z možností"
                                    }
                                    Text("Typ: $questionTypes")
                                    Text("Výsledky se zobrazí na konci")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = { quizStarted = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Vyzkoušet se")
                            }
                        }
                    }
                    
                    else -> {
                        // Kvíz probíhá
                        currentQuestion?.let { question ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Progress bar
                                LinearProgressIndicator(
                                    progress = (currentQuestionIndex + 1).toFloat() / totalQuestions,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Otázka
                                Card {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Otázka ${currentQuestionIndex + 1} z $totalQuestions",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            question.text,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Možnosti odpovědí nebo textové pole
                                if (question.isTextQuestion) {
                                    // Textové pole pro textové otázky
                                    OutlinedTextField(
                                        value = textAnswers[currentQuestionIndex] ?: "",
                                        onValueChange = { newText ->
                                            textAnswers = textAnswers.toMutableMap().apply {
                                                put(currentQuestionIndex, newText)
                                            }
                                        },
                                        label = { Text("Zadejte vaši odpověď...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                } else {
                                    // Výběr z možností pro běžné otázky
                                    Column(
                                        modifier = Modifier.selectableGroup()
                                    ) {
                                        question.options.forEachIndexed { index, option ->
                                            val isSelected = selectedAnswers[currentQuestionIndex] == index
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .selectable(
                                                        selected = isSelected,
                                                        onClick = {
                                                            selectedAnswers = selectedAnswers.toMutableMap().apply {
                                                                put(currentQuestionIndex, index)
                                                            }
                                                        },
                                                        role = Role.RadioButton
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    }
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = null
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        option,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Navigační tlačítka
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (currentQuestionIndex > 0) {
                                        OutlinedButton(
                                            onClick = { currentQuestionIndex-- }
                                        ) {
                                            Text("Předchozí")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    
                                    val hasAnswered = if (currentQuestion.isTextQuestion) {
                                        textAnswers[currentQuestionIndex]?.isNotBlank() == true
                                    } else {
                                        selectedAnswers.containsKey(currentQuestionIndex)
                                    }
                                    
                                    if (currentQuestionIndex < totalQuestions - 1) {
                                        Button(
                                            onClick = { currentQuestionIndex++ },
                                            enabled = hasAnswered
                                        ) {
                                            Text("Další")
                                        }
                                    } else {
                                        val allAnswered = questions.indices.all { index ->
                                            val question = questions[index]
                                            if (question.isTextQuestion) {
                                                textAnswers[index]?.isNotBlank() == true
                                            } else {
                                                selectedAnswers.containsKey(index)
                                            }
                                        }
                                        Button(
                                            onClick = { showResults = true },
                                            enabled = hasAnswered && allAnswered
                                        ) {
                                            Text("Ukončit prověrku")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Zobrazení points overlay
        if (showPointsOverlay && awardedPoints > 0) {
            FullScreenPointsOverlay(
                points = awardedPoints,
                totalPoints = totalPoints
            )
        }
    }
}