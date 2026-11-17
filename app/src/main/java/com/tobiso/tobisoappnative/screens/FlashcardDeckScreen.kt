package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.components.ChatMarkdownText
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.viewmodel.flashcards.FlashcardDeckIntent
import com.tobiso.tobisoappnative.viewmodel.flashcards.FlashcardDeckViewModel

/** What the back of a card shows: the correct answer(s) and the first explanation, if there is one. */
fun flashcardAnswerText(question: Question): String {
    val answers = question.answers.filter { it.correct == 1 }.joinToString("\n") { "• ${it.answerText}" }
    val single = question.correctTextAnswer
    return single ?: answers
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDeckScreen(categoryId: Int?, categoryName: String?, navController: NavController, vm: FlashcardDeckViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(categoryId) { vm.onIntent(FlashcardDeckIntent.Load(categoryId)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName?.let { "Kartičky · $it" } ?: "Kartičky") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(state.error!!, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { vm.onIntent(FlashcardDeckIntent.Load(categoryId)) }) { Text("Zkusit znovu") }
                }
                state.cards.isEmpty() -> Text(
                    "Pro tuto část zatím nejsou žádné kartičky.",
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.finished -> FinishedDeck(
                    known = state.known.size,
                    total = state.cards.size,
                    canRepeat = state.unknown.isNotEmpty(),
                    onRestart = { vm.onIntent(FlashcardDeckIntent.Load(categoryId)) },
                    onRepeatUnknown = { vm.onIntent(FlashcardDeckIntent.RepeatUnknown) },
                    onBack = { navController.popBackStack() }
                )
                else -> state.current?.let { card ->
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Kartička ${state.index + 1} z ${state.cards.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f).clickable { vm.onIntent(FlashcardDeckIntent.Flip) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.flipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!state.flipped) {
                                    ChatMarkdownText(text = card.questionText, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Klepni pro otočení", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Text("Odpověď", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.height(8.dp))
                                    ChatMarkdownText(text = flashcardAnswerText(card), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.titleMedium)
                                    card.explanation?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider()
                                        Spacer(Modifier.height(12.dp))
                                        ChatMarkdownText(text = it, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                        if (state.flipped) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { vm.onIntent(FlashcardDeckIntent.Answer(false)) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                                    Icon(Icons.Rounded.Replay, null)
                                    Spacer(Modifier.height(0.dp))
                                    Text(" Musím zopakovat")
                                }
                                Button(onClick = { vm.onIntent(FlashcardDeckIntent.Answer(true)) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                                    Icon(Icons.Rounded.Check, null)
                                    Text(" Umím")
                                }
                            }
                        } else {
                            Button(onClick = { vm.onIntent(FlashcardDeckIntent.Flip) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Ukázat odpověď") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedDeck(known: Int, total: Int, canRepeat: Boolean, onRestart: () -> Unit, onRepeatUnknown: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hotovo!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Uměl/a jsi $known z $total kartiček.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (canRepeat) Button(onClick = onRepeatUnknown, modifier = Modifier.fillMaxWidth()) { Text("Zopakovat ty, které neumím") }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Projít znovu (nové kartičky)") }
        TextButton(onClick = onBack) { Text("Zpět na procvičování") }
    }
}
