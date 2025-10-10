package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import com.example.tobisoappnative.components.MultiplierIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedQuizScreen(
    questionIds: String,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val allQuestions by viewModel.allQuestions.collectAsState()
    val allQuestionsLoading by viewModel.allQuestionsLoading.collectAsState()
    val allQuestionsError by viewModel.allQuestionsError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val questionsPosts by viewModel.questionsPosts.collectAsState()
    
    // Parse question IDs
    val questionIdsList = remember(questionIds) {
        questionIds.split(",").mapNotNull { it.toIntOrNull() }
    }
    
    // Filter questions by IDs
    val mixedQuestions = remember(allQuestions, questionIdsList) {
        allQuestions.filter { it.id in questionIdsList }
    }
    
    // Quiz state - using rememberSaveable to survive configuration changes
    var currentQuestionIndex by rememberSaveable { mutableStateOf(0) }
    var selectedAnswers by rememberSaveable { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var textAnswers by rememberSaveable { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    var quizStarted by rememberSaveable { mutableStateOf(false) }
    var pointsAwarded by rememberSaveable { mutableStateOf(false) }
    var showPointsOverlay by rememberSaveable { mutableStateOf(false) }
    var awardedPoints by rememberSaveable { mutableStateOf(0) }
    var shuffledQuestions by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    
    val context = LocalContext.current
    val totalPoints by PointsManager.totalPoints.collectAsState()
    
    // Load questions if not available (nyní funguje v online i offline režimu)
    LaunchedEffect(isOffline) {
        if (allQuestions.isEmpty()) {
            viewModel.loadAllQuestions()
        }
    }

    // Clear state when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAllQuestions()
        }
    }

    val totalQuestions = mixedQuestions.size
    val currentQuestion = if (quizStarted && shuffledQuestions.isNotEmpty() && currentQuestionIndex < shuffledQuestions.size) {
        val questionIndex = shuffledQuestions[currentQuestionIndex]
        if (questionIndex < mixedQuestions.size) mixedQuestions[questionIndex] else null
    } else null

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (!isOffline) {
                    isRefreshing = true
                    coroutineScope.launch {
                        viewModel.loadAllQuestions()
                        isRefreshing = false
                    }
                } else {
                    // V offline režimu jen resetujeme refresh state
                    isRefreshing = false
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LargeTopAppBar(
                    title = { 
                        Text(
                            text = if (showResults) "Výsledky procvičování"
                            else if (quizStarted) "Procvičování (${currentQuestionIndex + 1}/$totalQuestions)"
                            else "Procvičování",
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
                        // Zobrazení aktivního multiplikátoru
                        MultiplierIndicator()
                        
                        if (showResults) {
                            IconButton(onClick = { 
                                // Restart quiz
                                currentQuestionIndex = 0
                                selectedAnswers = emptyMap()
                                textAnswers = emptyMap()
                                showResults = false
                                quizStarted = false
                                pointsAwarded = false
                                shuffledQuestions = emptyList()
                            }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Znovu")
                            }
                        }
                    }
                )

                when {
                    allQuestionsError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    allQuestionsError ?: "Neznámá chyba",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    allQuestionsLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Načítání procvičování...")
                            }
                        }
                    }

                    mixedQuestions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.QuestionMark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Otázky pro procvičování nejsou k dispozici",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                                if (isOffline) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "V offline režimu jsou dostupné pouze dříve stažené otázky",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Zpět")
                                }
                            }
                        }
                    }

                    showResults -> {
                        // Results screen
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Results summary
                            item {
                                val correctAnswers = shuffledQuestions.mapIndexed { displayIndex, questionIndex ->
                                    val question = mixedQuestions[questionIndex]
                                    if (question.isTextQuestion) {
                                        val userText = textAnswers[displayIndex]?.trim() ?: ""
                                        val correctText = question.correctTextAnswer?.trim() ?: ""
                                        userText.equals(correctText, ignoreCase = true)
                                    } else {
                                        val selectedAnswer = selectedAnswers[displayIndex]
                                        selectedAnswer != null && 
                                            selectedAnswer >= 0 && 
                                            selectedAnswer < question.options.size &&
                                            selectedAnswer == question.correctAnswer
                                    }
                                }.count { it }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (correctAnswers.toFloat() / totalQuestions >= 0.7f) {
                                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        } else {
                                            Color(0xFFFF5722).copy(alpha = 0.1f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Výsledek procvičování",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "$correctAnswers z $totalQuestions správně",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            "${(correctAnswers.toFloat() / totalQuestions * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (correctAnswers.toFloat() / totalQuestions >= 0.7f) {
                                                Color(0xFF4CAF50)
                                            } else {
                                                Color(0xFFFF5722)
                                            }
                                        )
                                    }
                                }
                            }

                            // Individual results
                            items(shuffledQuestions.size) { displayIndex ->
                                val questionIndex = shuffledQuestions[displayIndex]
                                val question = mixedQuestions[questionIndex]
                                
                                val isCorrect = if (question.isTextQuestion) {
                                    val userText = textAnswers[displayIndex]?.trim() ?: ""
                                    val correctText = question.correctTextAnswer?.trim() ?: ""
                                    userText.equals(correctText, ignoreCase = true)
                                } else {
                                    val selectedAnswer = selectedAnswers[displayIndex]
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
                                                "Otázka ${displayIndex + 1}",
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
                                            // Show text answers
                                            val userText = textAnswers[displayIndex] ?: ""
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
                                            // Show multiple choice answers
                                            val selectedAnswer = selectedAnswers[displayIndex]
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

                                        // Show explanation if available
                                        val explanationText = question.explanation
                                        if (!explanationText.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Lightbulb,
                                                        contentDescription = "Vysvětlení",
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        explanationText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
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
                        // Quiz start screen
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Info cards with icons
                            val hasTextQuestions = mixedQuestions.any { it.isTextQuestion }
                            val hasMultipleChoice = mixedQuestions.any { !it.isTextQuestion }
                            val uniquePostIds = mixedQuestions.map { it.postId }.distinct()
                            val articleNames = uniquePostIds.mapNotNull { postId ->
                                questionsPosts.find { it.id == postId }?.title
                            }
                            
                            // Questions count card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Quiz,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Počet otázek",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            "${mixedQuestions.size}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Question types card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (hasTextQuestions && hasMultipleChoice) Icons.Filled.EditNote
                                        else if (hasTextQuestions) Icons.Filled.Edit
                                        else Icons.Filled.RadioButtonChecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Typ otázek",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            when {
                                                hasTextQuestions && hasMultipleChoice -> "Smíšené"
                                                hasTextQuestions -> "Textové"
                                                else -> "Výběrové"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Articles card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Filled.Article,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Články (${uniquePostIds.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                        )
                                        if (articleNames.isNotEmpty()) {
                                            val displayText = if (articleNames.size > 5) {
                                                "${articleNames.take(5).joinToString(", ")}..."
                                            } else {
                                                articleNames.joinToString(", ")
                                            }
                                            Text(
                                                displayText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        } else {
                                            Text(
                                                "Různé články",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Offline mode indicator
                            if (isOffline) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.WifiOff,
                                            contentDescription = "Offline režim",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Offline režim - používáte uložené otázky",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = { 
                                    quizStarted = true
                                    shuffledQuestions = mixedQuestions.indices.shuffled()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Začít procvičování")
                            }
                        }
                    }
                    
                    else -> {
                        // Quiz in progress
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
                                
                                // Question
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
                                
                                // Answer options or text field
                                if (question.isTextQuestion) {
                                    // Detekce typu klávesnice na základě správné odpovědi
                                    val keyboardType = remember(question.correctTextAnswer) {
                                        val correctAnswer = question.correctTextAnswer?.trim() ?: ""
                                        // Pokud odpověď neobsahuje písmena (pouze číslice a jiné znaky)
                                        if (correctAnswer.matches(Regex("^[^a-zA-ZáčďéěíňóřšťůúýžÁČĎÉĚÍŇÓŘŠŤŮÚÝŽ]*$")) && correctAnswer.isNotEmpty()) {
                                            KeyboardType.Number
                                        } else {
                                            KeyboardType.Text
                                        }
                                    }
                                    
                                    // Text field for text questions
                                    OutlinedTextField(
                                        value = textAnswers[currentQuestionIndex] ?: "",
                                        onValueChange = { newText ->
                                            textAnswers = textAnswers.toMutableMap().apply {
                                                put(currentQuestionIndex, newText)
                                            }
                                        },
                                        label = { Text("Zadejte vaši odpověď...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = keyboardType
                                        )
                                    )
                                } else {
                                    // Radio buttons for multiple choice
                                    Column(
                                        modifier = Modifier.selectableGroup()
                                    ) {
                                        question.options.forEachIndexed { index, option ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .selectable(
                                                        selected = (selectedAnswers[currentQuestionIndex] == index),
                                                        onClick = {
                                                            selectedAnswers = selectedAnswers.toMutableMap().apply {
                                                                put(currentQuestionIndex, index)
                                                            }
                                                        }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = (selectedAnswers[currentQuestionIndex] == index),
                                                    onClick = null
                                                )
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.padding(start = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Navigation buttons
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
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    
                                    if (currentQuestionIndex < totalQuestions - 1) {
                                        Button(
                                            onClick = { currentQuestionIndex++ }
                                        ) {
                                            Text("Další")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                showResults = true
                                                
                                                // Award points if not already awarded
                                                if (!pointsAwarded) {
                                                    val correctAnswers = shuffledQuestions.mapIndexed { displayIndex, questionIndex ->
                                                        val q = mixedQuestions[questionIndex]
                                                        if (q.isTextQuestion) {
                                                            val userText = textAnswers[displayIndex]?.trim() ?: ""
                                                            val correctText = q.correctTextAnswer?.trim() ?: ""
                                                            userText.equals(correctText, ignoreCase = true)
                                                        } else {
                                                            val selectedAnswer = selectedAnswers[displayIndex]
                                                            selectedAnswer != null && 
                                                                selectedAnswer >= 0 && 
                                                                selectedAnswer < q.options.size &&
                                                                selectedAnswer == q.correctAnswer
                                                        }
                                                    }.count { it }
                                                    
                                                    val points = correctAnswers * 2 // 2 body za správnou odpověď
                                                    if (points > 0) {
                                                        PointsManager.addPoints(context, points)
                                                        awardedPoints = points
                                                        showPointsOverlay = true
                                                    }
                                                    pointsAwarded = true
                                                }
                                            }
                                        ) {
                                            Text("Dokončit")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Points overlay
        if (showPointsOverlay) {
            FullScreenPointsOverlay(
                points = awardedPoints,
                totalPoints = totalPoints
            )
            LaunchedEffect(showPointsOverlay) {
                kotlinx.coroutines.delay(2500)
                showPointsOverlay = false
            }
        }
    }
}