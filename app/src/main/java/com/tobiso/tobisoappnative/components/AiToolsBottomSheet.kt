package com.tobiso.tobisoappnative.components

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.components.conceptmap.ConceptMapView
import com.tobiso.tobisoappnative.utils.FormulaEvaluator
import com.tobiso.tobisoappnative.utils.formatFormulaValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tobiso.tobisoappnative.model.FormulaEntry
import com.tobiso.tobisoappnative.model.PracticeProblem
import com.tobiso.tobisoappnative.utils.TextUtils
import com.tobiso.tobisoappnative.viewmodel.ai.AiTool
import com.tobiso.tobisoappnative.viewmodel.ai.AiToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsBottomSheet(
    postId: Int,
    onDismiss: () -> Unit,
    onEnterSentenceSelectMode: () -> Unit,
    onSignIn: () -> Unit = {},
    navController: NavController? = null,
    vm: AiToolsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        AnimatedContent(
            targetState = state.activeTool,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "ai_tool_content"
        ) { activeTool ->
            if (activeTool == null) {
                AiToolGrid(onToolSelected = { tool ->
                    if (tool == AiTool.EXPLAIN) {
                        onEnterSentenceSelectMode()
                        return@AiToolGrid
                    }
                    vm.openTool(tool)
                    when (tool) {
                        AiTool.REAL_WORLD -> vm.loadRealWorld(postId)
                        AiTool.FLASHCARDS -> vm.generateFlashcards(postId)
                        AiTool.PRACTICE -> vm.generatePracticeProblems(postId)
                        AiTool.EXAM_SUMMARY -> vm.loadExamSummary(postId)
                        AiTool.EXAM_QUESTIONS -> vm.loadExamQuestions(postId)
                        AiTool.KEY_TERMS -> vm.loadKeyTerms(postId)
                        AiTool.REWRITE -> vm.loadGrades()
                        AiTool.STEP_SOLVER -> vm.loadStepSolver(postId)
                        AiTool.CONCEPT_MAP -> vm.loadConceptMap(postId)
                        AiTool.FORMULAS -> vm.loadFormulas(postId)
                        AiTool.CROSS_CONNECTIONS -> vm.loadCrossConnections(postId)
                        AiTool.FUN_FACTS -> vm.loadFunFacts(postId)
                        AiTool.NOTES -> vm.loadNote(postId)
                        else -> Unit
                    }
                })
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AiToolHeader(tool = activeTool, onBack = vm::closeTool)
                    when (activeTool) {
                        AiTool.FLASHCARDS -> FlashcardsContent(vm, postId)
                        AiTool.REAL_WORLD -> RealWorldContent(vm, postId)
                        AiTool.WHAT_IF -> WhatIfContent(vm, postId)
                        AiTool.FEYNMAN -> FeynmanContent(vm, postId)
                        AiTool.PRACTICE -> PracticeContent(vm, postId)
                        AiTool.EXAM_SUMMARY -> ExamSummaryContent(vm, postId)
                        AiTool.EXAM_QUESTIONS -> ExamQuestionsContent(vm, postId)
                        AiTool.KEY_TERMS -> KeyTermsContent(vm, postId)
                        AiTool.COMPARE -> CompareContent(vm, postId)
                        AiTool.WHY -> WhyContent(vm, postId)
                        AiTool.STEP_SOLVER -> StepSolverContent(vm, postId)
                        AiTool.CONCEPT_MAP -> ConceptMapContent(vm, postId)
                        AiTool.REWRITE -> RewriteContent(vm, postId, navController)
                        AiTool.FORMULAS -> FormulasContent(vm, postId)
                        AiTool.CROSS_CONNECTIONS -> CrossConnectionsContent(vm, postId)
                        AiTool.FUN_FACTS -> FunFactsContent(vm, postId)
                        AiTool.NOTES -> NotesContent(vm, postId, onSignIn)
                        AiTool.POMODORO -> PomodoroContent()
                        else -> Unit
                    }
                }
            }
        }
    }
}

// ── Tool grid ─────────────────────────────────────────────────────────────────

private data class ToolEntry(
    val tool: AiTool,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

private val tools = listOf(
    ToolEntry(AiTool.FLASHCARDS, "Kartičky", Icons.Rounded.Style, Color(0xFF6C63FF), "Procvič si pojmy ve formátu otázka/odpověď"),
    ToolEntry(AiTool.PRACTICE, "Procvičování", Icons.Rounded.Calculate, Color(0xFF20B2AA), "Úlohy ke článku s řešením"),
    ToolEntry(AiTool.REAL_WORLD, "Reálný svět", Icons.Rounded.Public, Color(0xFF4CAF50), "Jak se téma využívá v reálném životě"),
    ToolEntry(AiTool.WHAT_IF, "Co kdyby?", Icons.Rounded.Lightbulb, Color(0xFFFF9800), "Myšlenkový experiment k tématu"),
    ToolEntry(AiTool.FEYNMAN, "Zkus to vysvětlit", Icons.Rounded.RecordVoiceOver, Color(0xFFE91E63), "Napiš vysvětlení – AI ohodnotí tvé porozumění"),
    ToolEntry(AiTool.EXAM_SUMMARY, "Shrnutí na test", Icons.Rounded.Checklist, Color(0xFF795548), "Co si z článku odnést před testem"),
    ToolEntry(AiTool.EXAM_QUESTIONS, "Co může přijít v testu", Icons.Rounded.Quiz, Color(0xFF8D6E63), "Pravděpodobné otázky s odpověďmi"),
    ToolEntry(AiTool.KEY_TERMS, "Klíčové pojmy", Icons.Rounded.MenuBook, Color(0xFF3F51B5), "Slovníček nejdůležitějších pojmů"),
    ToolEntry(AiTool.COMPARE, "Porovnat s…", Icons.Rounded.CompareArrows, Color(0xFF009688), "Srovnávací tabulka s jiným tématem"),
    ToolEntry(AiTool.STEP_SOLVER, "Krok za krokem", Icons.Rounded.Stairs, Color(0xFF00897B), "Řešený příklad s vysvětlením každého kroku"),
    ToolEntry(AiTool.CONCEPT_MAP, "Myšlenková mapa", Icons.Rounded.AccountTree, Color(0xFF6D4C41), "Mapa pojmů a jejich souvislostí"),
    ToolEntry(AiTool.WHY, "Proč to platí?", Icons.Rounded.Help, Color(0xFF7E57C2), "Vlož větu z článku a AI vysvětlí proč"),
    ToolEntry(AiTool.REWRITE, "Přepsat jinak", Icons.Rounded.Edit, Color(0xFF26A69A), "Jednodušeji, odborněji nebo pro jiný ročník"),
    ToolEntry(AiTool.FORMULAS, "Vzorce", Icons.Rounded.Functions, Color(0xFF5C6BC0), "Vzorce z článku a jejich proměnné"),
    ToolEntry(AiTool.CROSS_CONNECTIONS, "Souvislosti", Icons.Rounded.Hub, Color(0xFFEF6C00), "Propojení s jinými předměty"),
    ToolEntry(AiTool.FUN_FACTS, "Zajímavosti", Icons.Rounded.AutoAwesome, Color(0xFFFFB300), "Pár zajímavostí k tématu"),
    ToolEntry(AiTool.NOTES, "Moje poznámka", Icons.Rounded.EditNote, Color(0xFF607D8B), "Soukromá poznámka k článku (v účtu)"),
    ToolEntry(AiTool.POMODORO, "Pomodoro", Icons.Rounded.Timer, Color(0xFFE53935), "25 minut soustředění, 5 minut pauza"),
    ToolEntry(AiTool.EXPLAIN, "Vysvětli větu", Icons.Rounded.QuestionMark, Color(0xFF2196F3), "Vlož větu nebo pojem k vysvětlení"),
)

@Composable
private fun AiToolGrid(onToolSelected: (AiTool) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "AI Nástroje",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        tools.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { entry ->
                    ToolCard(entry = entry, onClick = { onToolSelected(entry.tool) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ToolCard(entry: ToolEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = entry.color.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, entry.color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(entry.color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(entry.icon, contentDescription = null, tint = entry.color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(entry.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(entry.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
    }
}

// ── Common header ─────────────────────────────────────────────────────────────

@Composable
private fun AiToolHeader(tool: AiTool, onBack: () -> Unit) {
    val entry = tools.first { it.tool == tool }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(entry.icon, contentDescription = null, tint = entry.color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(entry.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}

// ── Loading / Error helpers ───────────────────────────────────────────────────

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(msg: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(msg, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Zkusit znovu") }
    }
}

// ── Flashcards ────────────────────────────────────────────────────────────────

@Composable
private fun FlashcardsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.generateFlashcards(postId) }
            state.flashcards.isEmpty() -> {
                Text("Nepodařilo se načíst kartičky.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val card = state.flashcards[state.flashcardIndex]
                val total = state.flashcards.size
                val index = state.flashcardIndex

                // Progress
                Text("${index + 1} / $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                // Flip card
                val cardFlipDeg by animateFloatAsState(
                    targetValue = if (state.flashcardShowDefinition) 180f else 0f,
                    animationSpec = tween(400),
                    label = "cardFlip"
                )
                val isBack = cardFlipDeg > 90f
                val cardColor by animateColorAsState(
                    targetValue = if (state.flashcardShowDefinition) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(300),
                    label = "cardColor"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer { rotationY = cardFlipDeg; cameraDistance = 8 * density }
                        .clickable { vm.flipFlashcard() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(Modifier.fillMaxSize().graphicsLayer { if (isBack) rotationY = 180f }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = if (!isBack) "POJEM" else "DEFINICE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (!isBack) card.term else card.definition,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Klepni pro otočení", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = vm::prevFlashcard, enabled = index > 0) {
                        Icon(Icons.Rounded.ChevronLeft, null)
                        Text("Předchozí")
                    }
                    Button(onClick = vm::nextFlashcard, enabled = index < total - 1) {
                        Text("Další")
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }
        }
    }
}

// ── Real World ────────────────────────────────────────────────────────────────

@Composable
private fun RealWorldContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadRealWorld(postId) }
            state.realWorldApps.isEmpty() -> {
                Text("Žádné aplikace k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                Text(
                    "Jak se toto téma uplatňuje v reálném životě:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                state.realWorldApps.forEachIndexed { i, app ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.10f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(28.dp).background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(app, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── What-If ───────────────────────────────────────────────────────────────────

@Composable
private fun WhatIfContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Napiš otázku 'Co kdyby...' vztaženou k tématu článku a AI vymyslí myšlenkový experiment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.whatIfQuestion,
            onValueChange = vm::setWhatIfQuestion,
            label = { Text("Co kdyby...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onSend = { vm.submitWhatIf(postId) }),
            trailingIcon = {
                IconButton(onClick = { vm.submitWhatIf(postId) }, enabled = state.whatIfQuestion.isNotBlank() && !state.isLoading) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.submitWhatIf(postId) }
            state.whatIfScenario.isNotEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scénář", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(state.whatIfScenario, style = MaterialTheme.typography.bodyMedium)
                        if (state.whatIfExplanation.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text("Vysvětlení", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(state.whatIfExplanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ── Feynman ───────────────────────────────────────────────────────────────────

@Composable
private fun FeynmanContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.RecordVoiceOver, null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Feynmanova technika: vysvětli téma vlastními slovy, jako bys ho vysvětloval/a kamarádovi. AI ohodnotí tvé porozumění.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.feynmanText,
            onValueChange = vm::setFeynmanText,
            label = { Text("Moje vysvětlení...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { vm.submitFeynman(postId) },
            enabled = state.feynmanText.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Ohodnotit porozumění")
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.submitFeynman(postId) }
            state.feynmanScore >= 0 -> FeynmanResult(state.feynmanScore, state.feynmanFeedback, state.feynmanStrong, state.feynmanMissing)
        }
    }
}

@Composable
private fun FeynmanResult(score: Int, feedback: String, strong: List<String>, missing: List<String>) {
    val scoreColor = when {
        score >= 8 -> Color(0xFF4CAF50)
        score >= 5 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Column {
        // Score circle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(scoreColor.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                    Text("/10", fontSize = 10.sp, color = scoreColor)
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(feedback, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        if (strong.isNotEmpty()) {
            Text("✅ Silné stránky", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF4CAF50))
            Spacer(Modifier.height(6.dp))
            strong.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
            Spacer(Modifier.height(12.dp))
        }
        if (missing.isNotEmpty()) {
            Text("❌ Co chybí", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFF44336))
            Spacer(Modifier.height(6.dp))
            missing.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

// ── Practice Problems ─────────────────────────────────────────────────────────

@Composable
private fun PracticeContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.generatePracticeProblems(postId) }
            state.practiceProblems.isEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Žádné úlohy k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.practiceProblems) { i, problem ->
                        PracticeProblemCard(
                            index = i,
                            problem = problem,
                            isExpanded = state.expandedProblemIndex == i,
                            onToggle = { vm.toggleProblemExpanded(i) },
                            answer = state.problemAnswers[i].orEmpty(),
                            onAnswerChange = { vm.setProblemAnswer(i, it) },
                            feedback = state.problemFeedback[i],
                            checking = state.checkingProblem == i,
                            onCheck = { vm.checkProblemAnswer(postId, i) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PracticeProblemCard(
    index: Int,
    problem: PracticeProblem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    feedback: com.tobiso.tobisoappnative.model.EvaluateAnswerResponse?,
    checking: Boolean,
    onCheck: () -> Unit
) {
    val difficultyColor = when (problem.difficulty.lowercase()) {
        "easy", "lehká", "lehke" -> Color(0xFF4CAF50)
        "medium", "střední", "stredni" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).background(Color(0xFF20B2AA).copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF20B2AA))
                }
                Spacer(Modifier.width(10.dp))
                Text(problem.problemText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = { Text("Moje odpověď") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 4
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onCheck, enabled = answer.isNotBlank() && !checking) {
                if (checking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Zkontrolovat odpověď")
            }
            feedback?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    (if (it.isCorrect) "Správně. " else "Ještě ne. ") + it.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(difficultyColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(problem.difficulty, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = difficultyColor)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggle) {
                    Text(if (isExpanded) "Skrýt řešení" else "Zobrazit řešení", fontSize = 13.sp)
                    Icon(
                        if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null, modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Řešení:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF20B2AA))
                Spacer(Modifier.height(4.dp))
                Text(problem.solution, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
            }
        }
    }
}


// ── Exam summary ──────────────────────────────────────────────────────────────

@Composable
private fun ExamSummaryContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadExamSummary(postId) }
            state.examSummary.isBlank() -> Text("Shrnutí není k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> ChatMarkdownText(text = state.examSummary, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Account required ──────────────────────────────────────────────────────────

/** Shown instead of a feature that needs a signed-in account (mirrors the web's "Je potřeba účet" dialog). */
@Composable
private fun AccountRequiredPrompt(message: String, onSignIn: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Je potřeba účet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSignIn) { Text("Přihlásit se") }
    }
}

// ── Exam questions ────────────────────────────────────────────────────────────

@Composable
private fun ExamQuestionsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadExamQuestions(postId) }
            state.examQuestions.isEmpty() -> Text("Žádné otázky k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.examQuestions.forEachIndexed { index, q ->
                var revealed by remember(q) { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { revealed = !revealed },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8D6E63).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${index + 1}. ${q.question}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        if (revealed) {
                            HorizontalDivider()
                            Spacer(Modifier.height(6.dp))
                            Text(q.answer, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("Klepni pro zobrazení odpovědi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ── Key terms ─────────────────────────────────────────────────────────────────

@Composable
private fun KeyTermsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadKeyTerms(postId) }
            state.keyTerms.isEmpty() -> Text("Žádné pojmy k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.keyTerms.forEach { t ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3F51B5).copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(t.term, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(t.definition, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ── Step solver ───────────────────────────────────────────────────────────────

/** Calculation steps come as LaTeX, sometimes wrapped in `$…$`; render the Unicode form either way. */
private fun stepToText(step: String): String =
    if ('$' in step) convertMathSpans(step) else latexToUnicode(step)

@Composable
private fun StepSolverContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadStepSolver(postId) }
            state.solverSteps.isEmpty() -> Text("K článku nebyl nalezen žádný řešený příklad.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.solverSteps.forEachIndexed { index, s ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00897B).copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Krok ${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer {
                            Text(stepToText(s.step), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (s.explanation.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            ChatMarkdownText(text = s.explanation, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ── Concept map ───────────────────────────────────────────────────────────────

@Composable
private fun ConceptMapContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadConceptMap(postId) }
            state.conceptNodes.isEmpty() -> Text("Mapu se nepodařilo vytvořit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> ConceptMapView(state.conceptNodes, state.conceptEdges)
        }
    }
}

// ── Why ───────────────────────────────────────────────────────────────────────

@Composable
private fun WhyContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        Text(
            "Vlož větu z článku a AI vysvětlí, proč to tak je.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.whySentence,
            onValueChange = vm::setWhySentence,
            label = { Text("Věta nebo tvrzení") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onSend = { vm.submitWhy(postId) }),
            trailingIcon = {
                IconButton(onClick = { vm.submitWhy(postId) }, enabled = state.whySentence.isNotBlank() && !state.isLoading) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.submitWhy(postId) }
            state.whyResult.isNotBlank() -> ChatMarkdownText(text = state.whyResult, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Rewrite ───────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RewriteContent(vm: AiToolsViewModel, postId: Int, navController: NavController?) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        Text("Styl", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)) {
            listOf("simple" to "Jednodušeji", "student" to "Pro studenty", "expert" to "Odborně").forEach { (value, label) ->
                val key = "register:$value"
                FilterChip(selected = state.rewriteSelected == key, onClick = { vm.rewrite(postId, key) }, label = { Text(label) })
            }
        }
        if (state.grades.isNotEmpty()) {
            Text("Ročník", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)) {
                state.grades.forEach { grade ->
                    val key = "grade:${grade.level}"
                    FilterChip(selected = state.rewriteSelected == key, onClick = { vm.rewrite(postId, key) }, label = { Text(grade.name) })
                }
            }
        }
        when {
            state.isLoading -> LoadingBox()
            state.error != null && state.rewriteSelected.isNotEmpty() -> ErrorBox(state.error!!) { vm.rewrite(postId, state.rewriteSelected) }
            state.rewriteContent.isNotBlank() -> if (navController != null) {
                // Same renderer as the article, so tables, graphs and math in the rewritten text look right.
                val elements = remember(state.rewriteContent) {
                    parseContentToElements(state.rewriteContent, isOffline = false, posts = emptyList())
                }
                ContentRenderer(
                    contentElements = elements,
                    isOffline = false,
                    posts = emptyList(),
                    addendums = emptyList(),
                    navController = navController,
                    onAddendumSelected = {}
                )
            } else {
                ChatMarkdownText(text = state.rewriteContent, color = MaterialTheme.colorScheme.onSurface)
            }
            else -> Text("Vyber styl nebo ročník a AI článek přepíše.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Formulas ──────────────────────────────────────────────────────────────────

@Composable
private fun FormulasContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadFormulas(postId) }
            state.formulas.isEmpty() -> Text("V článku nebyly nalezeny žádné vzorce.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.formulas.forEach { FormulaCard(it) }
        }
    }
}

/** One formula with a slider per variable and the live result (when the formula can be evaluated). */
@Composable
private fun FormulaCard(f: FormulaEntry) {
    val names = remember(f) { f.variables.map { it.name } }
    val compiled = remember(f) { FormulaEvaluator.compile(f.expression.ifBlank { f.formula }, names) }
    val values = remember(f) { mutableStateMapOf<String, Double>().apply { f.variables.forEach { put(it.name, it.defaultVal) } } }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5C6BC0).copy(alpha = 0.10f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(f.formula.ifBlank { f.expression }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            f.variables.forEach { v ->
                val min = v.min
                val max = if (v.max > v.min) v.max else v.min + 1
                val value = (values[v.name] ?: v.defaultVal).coerceIn(min, max)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${v.name} – ${v.label}: ${formatFormulaValue(value)}" + if (v.unit.isNotBlank()) " ${v.unit}" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                val steps = if (v.step > 0) (((max - min) / v.step).toInt() - 1).coerceIn(0, 1000) else 0
                Slider(
                    value = value.toFloat(),
                    onValueChange = { values[v.name] = it.toDouble() },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps
                )
            }
            when (compiled) {
                is FormulaEvaluator.Compiled.Ok -> {
                    val result = compiled.evaluate(values.toMap())
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${f.resultVar.ifBlank { "Výsledek" }} = ${formatFormulaValue(result)}" + if (f.resultUnit.isNotBlank()) " ${f.resultUnit}" else "",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is FormulaEvaluator.Compiled.Error -> Text(
                    "Výsledek nelze spočítat (${compiled.message}).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Cross connections ─────────────────────────────────────────────────────────

@Composable
private fun CrossConnectionsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadCrossConnections(postId) }
            state.crossConnections.isEmpty() -> Text("Žádné souvislosti k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.crossConnections.forEach { c ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF6C00).copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (c.subject.isNotBlank()) Text(c.subject, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(c.targetPostTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(c.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ── Compare ───────────────────────────────────────────────────────────────────

@Composable
private fun CompareContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        Text(
            "Napiš téma, se kterým chceš článek porovnat, a AI vytvoří srovnávací tabulku.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.compareTo,
            onValueChange = vm::setCompareTo,
            label = { Text("Porovnat s…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onSend = { vm.submitCompare(postId) }),
            trailingIcon = {
                IconButton(onClick = { vm.submitCompare(postId) }, enabled = state.compareTo.isNotBlank() && !state.isLoading) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.submitCompare(postId) }
            state.compareTable.isNotBlank() -> MarkdownTableView(state.compareTable)
        }
    }
}

/** Renders a markdown pipe table (header, separator, rows); any text outside the table is shown as markdown. */
@Composable
private fun MarkdownTableView(markdown: String) {
    val parts = remember(markdown) { parseMarkdownTable(markdown) }
    val rows = parts.rows
    val outside = parts.outside
    if (rows.isNotEmpty()) {
        val columns = rows.maxOf { it.size }
        val outline = MaterialTheme.colorScheme.outlineVariant
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, outline, RoundedCornerShape(12.dp))
                .horizontalScroll(rememberScrollState())
        ) {
            rows.forEachIndexed { r, cells ->
                Row(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .background(if (r == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                ) {
                    for (c in 0 until columns) {
                        Text(
                            text = cells.getOrElse(c) { "" }.replace("**", ""),
                            fontWeight = if (r == 0 || c == 0) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight()
                                .border(0.5.dp, outline)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
    if (outside.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        ChatMarkdownText(text = outside, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── Fun facts ─────────────────────────────────────────────────────────────────

@Composable
private fun FunFactsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!) { vm.loadFunFacts(postId) }
            state.funFacts.isEmpty() -> Text("Žádné zajímavosti k dispozici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.funFacts.forEach { fact ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(fact, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Notes ─────────────────────────────────────────────────────────────────────

@Composable
private fun NotesContent(vm: AiToolsViewModel, postId: Int, onSignIn: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val signedIn by com.tobiso.tobisoappnative.model.AuthSession.user.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
        if (signedIn == null) {
            AccountRequiredPrompt("Pro poznámky se musíš přihlásit. Ukládají se do účtu a jsou vidět i na webu.", onSignIn)
            return@Column
        }
        if (state.isLoading) { LoadingBox(); return@Column }
        OutlinedTextField(
            value = state.noteText,
            onValueChange = vm::setNoteText,
            label = { Text("Poznámky k tomuto článku…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            maxLines = 12,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.saveNote(postId) }, enabled = !state.noteSaving, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Check, null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.noteSaved) "Uloženo" else "Uložit poznámku")
        }
    }
}

// ── Pomodoro ──────────────────────────────────────────────────────────────────

@Composable
private fun PomodoroContent() {
    val s by com.tobiso.tobisoappnative.manager.PomodoroTimer.state.collectAsStateWithLifecycle()
    val timer = com.tobiso.tobisoappnative.manager.PomodoroTimer
    Column(
        Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (s.breakPhase) "Pauza" else "Soustředění", style = MaterialTheme.typography.titleMedium)
        Text(
            "%02d:%02d".format(s.remainingSeconds / 60, s.remainingSeconds % 60),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.running) {
                Button(onClick = { timer.pause() }) { Text("Pozastavit") }
            } else {
                Button(onClick = { timer.start() }) { Text("Spustit") }
            }
            OutlinedButton(onClick = { timer.reset() }) { Text("Znovu") }
        }
        if (s.completed > 0) Text("Dokončeno kol: ${s.completed}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Časovač běží dál, i když sheet zavřeš.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
