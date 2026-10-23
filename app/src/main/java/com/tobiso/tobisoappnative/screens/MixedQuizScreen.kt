package com.tobiso.tobisoappnative.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.components.AnswerRow
import com.tobiso.tobisoappnative.components.AnswerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.components.CustomNumericKeyboard
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.utils.normalizeText
import com.tobiso.tobisoappnative.viewmodel.mixedquiz.MixedQuizIntent
import com.tobiso.tobisoappnative.viewmodel.mixedquiz.MixedQuizViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedQuizScreen(
    questionIds: String,
    navController: NavController
) {
    val vm: MixedQuizViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(questionIds) {
        vm.onIntent(MixedQuizIntent.Load(questionIds))
    }

    val totalQuestions = state.mixedQuestions.size
    val currentQuestion = if (state.quizStarted && state.shuffledIndices.isNotEmpty() && state.currentQuestionIndex < state.shuffledIndices.size) {
        val questionIndex = state.shuffledIndices[state.currentQuestionIndex]
        if (questionIndex < state.mixedQuestions.size) state.mixedQuestions[questionIndex] else null
    } else null

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            if (!state.isOffline) {
                vm.onIntent(MixedQuizIntent.Refresh)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val quizActive = state.quizStarted && !state.showResults && state.error == null && !state.isLoading && state.mixedQuestions.isNotEmpty()
                if (quizActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Zavřít")
                        }
                        LinearProgressIndicator(
                            progress = { (state.currentQuestionIndex + 1).toFloat() / totalQuestions },
                            modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        Text(
                            "${state.currentQuestionIndex + 1} / $totalQuestions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                TopAppBar(
                    title = {
                        Text(
                            text = if (state.showResults) "Výsledky procvičování"
                            else if (state.quizStarted) "Procvičování (${state.currentQuestionIndex + 1}/$totalQuestions)"
                            else "Procvičování",
                            style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
                        MultiplierIndicator()

                        if (state.showResults) {
                            IconButton(onClick = { vm.onIntent(MixedQuizIntent.RestartQuiz) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Znovu")
                            }
                        }
                    }
                )
                }

                when {
                    state.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    state.error ?: "Neznámá chyba",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    state.isLoading -> {
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

                    state.mixedQuestions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.QuestionMark,
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
                                if (state.isOffline) {
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

                    state.showResults -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                val correctAnswers = state.shuffledIndices.mapIndexed { displayIndex, questionIndex ->
                                    if (questionIndex >= state.mixedQuestions.size) return@mapIndexed false
                                    val question = state.mixedQuestions[questionIndex]
                                    if (question.isTextQuestion) {
                                        val userText = state.textAnswers[displayIndex]?.trim() ?: ""
                                        val correctText = question.correctTextAnswer?.trim() ?: ""
                                        normalizeText(userText) == normalizeText(correctText)
                                    } else {
                                        val selectedAnswer = state.selectedAnswers[displayIndex]
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
                                        Text("$correctAnswers z $totalQuestions správně")
                                        Text(
                                            "${(correctAnswers.toFloat() / totalQuestions * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (correctAnswers.toFloat() / totalQuestions >= 0.7f) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                        )
                                    }
                                }
                            }

                            items(state.shuffledIndices.size) { displayIndex ->
                                if (displayIndex >= state.shuffledIndices.size) return@items
                                val questionIndex = state.shuffledIndices[displayIndex]
                                if (questionIndex >= state.mixedQuestions.size) return@items
                                val question = state.mixedQuestions[questionIndex]

                                val isCorrect = if (question.isTextQuestion) {
                                    val userText = state.textAnswers[displayIndex]?.trim() ?: ""
                                    val correctText = question.correctTextAnswer?.trim() ?: ""
                                    normalizeText(userText) == normalizeText(correctText)
                                } else {
                                    val selectedAnswer = state.selectedAnswers[displayIndex]
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
                                        containerColor = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
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
                                        Text(question.text, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (question.isTextQuestion) {
                                            val userText = state.textAnswers[displayIndex] ?: ""
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
                                            val selectedAnswer = state.selectedAnswers[displayIndex]
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
                                                        Icons.Rounded.Lightbulb,
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

                    !state.quizStarted -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val hasTextQuestions = state.mixedQuestions.any { it.isTextQuestion }
                            val hasMultipleChoice = state.mixedQuestions.any { !it.isTextQuestion }
                            val uniquePostIds = state.mixedQuestions.map { it.postId }.distinct()
                            val articleNames = uniquePostIds.mapNotNull { postId ->
                                state.questionsPosts.find { it.id == postId }?.title
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Počet otázek", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        Text("${state.mixedQuestions.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (hasTextQuestions && hasMultipleChoice) Icons.Rounded.EditNote
                                        else if (hasTextQuestions) Icons.Rounded.Edit
                                        else Icons.Rounded.RadioButtonChecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Typ otázek", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
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

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Články (${uniquePostIds.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                        if (articleNames.isNotEmpty()) {
                                            val displayText = if (articleNames.size > 5) "${articleNames.take(5).joinToString(", ")}..." else articleNames.joinToString(", ")
                                            Text(displayText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        } else {
                                            Text("Různé články", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }
                                    }
                                }
                            }

                            if (state.isOffline) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.WifiOff, contentDescription = "Offline režim", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Offline režim — používají se uložené otázky", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { vm.onIntent(MixedQuizIntent.StartQuiz) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Začít procvičování")
                            }
                        }
                    }

                    else -> {
                        currentQuestion?.let { question ->
                            val scheme = MaterialTheme.colorScheme
                            val selected = state.selectedAnswers[state.currentQuestionIndex]
                            Column(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    state.questionsPosts.find { it.id == question.postId }?.title?.let { topic ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(topic, maxLines = 1) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                    Text(
                                        question.text,
                                        fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 26.sp,
                                        lineHeight = 32.sp
                                    )

                                    if (question.isTextQuestion) {
                                        val isNumericAnswer = remember(question.correctTextAnswer) {
                                            val correctAnswer = question.correctTextAnswer?.trim() ?: ""
                                            correctAnswer.matches(Regex("^[^a-zA-ZáčďéěíňóřšťůúýžÁČĎÉĚÍŇÓŘŠŤŮÚÝŽ]*$")) && correctAnswer.isNotEmpty()
                                        }

                                        OutlinedTextField(
                                            value = state.textAnswers[state.currentQuestionIndex] ?: "",
                                            onValueChange = { newText ->
                                                vm.onIntent(MixedQuizIntent.SetTextAnswer(state.currentQuestionIndex, newText))
                                            },
                                            label = { Text("Zadejte vaši odpověď...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            readOnly = isNumericAnswer,
                                            shape = RoundedCornerShape(22.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                        )

                                        if (isNumericAnswer) {
                                            CustomNumericKeyboard(
                                                alternateSymbol = if ((question.correctTextAnswer ?: "").contains(":")) ":" else "%",
                                                onKeyPress = { key ->
                                                    val currentText = state.textAnswers[state.currentQuestionIndex] ?: ""
                                                    vm.onIntent(MixedQuizIntent.SetTextAnswer(state.currentQuestionIndex, currentText + key))
                                                },
                                                onBackspace = {
                                                    val currentText = state.textAnswers[state.currentQuestionIndex] ?: ""
                                                    if (currentText.isNotEmpty()) {
                                                        vm.onIntent(MixedQuizIntent.SetTextAnswer(state.currentQuestionIndex, currentText.dropLast(1)))
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            question.options.forEachIndexed { index, option ->
                                                val answered = selected != null
                                                AnswerRow(
                                                    letter = 'A' + index,
                                                    text = option,
                                                    enabled = !answered,
                                                    state = when {
                                                        !answered -> AnswerState.Idle
                                                        index == question.correctAnswer -> AnswerState.Correct
                                                        index == selected -> AnswerState.Wrong
                                                        else -> AnswerState.Idle
                                                    },
                                                    onClick = { vm.onIntent(MixedQuizIntent.SelectAnswer(state.currentQuestionIndex, index)) }
                                                )
                                            }
                                        }
                                        if (selected != null) {
                                            val ok = selected == question.correctAnswer
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(if (ok) scheme.secondaryContainer else scheme.primaryContainer)
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    if (ok) "Správně!" else "Tentokrát ne",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (ok) scheme.onSecondaryContainer else scheme.onPrimaryContainer
                                                )
                                                question.explanation?.takeIf { it.isNotBlank() }?.let {
                                                    Text(
                                                        it,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 22.sp,
                                                        color = if (ok) scheme.onSecondaryContainer else scheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (state.currentQuestionIndex > 0) {
                                        OutlinedButton(
                                            onClick = { vm.onIntent(MixedQuizIntent.PreviousQuestion) },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(50)
                                        ) { Text("Předchozí", fontWeight = FontWeight.SemiBold) }
                                    }
                                    Button(
                                        onClick = {
                                            if (state.currentQuestionIndex < totalQuestions - 1) vm.onIntent(MixedQuizIntent.NextQuestion)
                                            else vm.onIntent(MixedQuizIntent.FinishQuiz)
                                        },
                                        modifier = Modifier.weight(1.4f).height(52.dp),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            if (state.currentQuestionIndex < totalQuestions - 1) "Další →" else "Dokončit",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

    }
}
