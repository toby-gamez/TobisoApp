package com.tobiso.tobisoappnative.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    vm: AiToolsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

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
    ToolEntry(AiTool.FLASHCARDS, "Kartičky", Icons.Default.Style, Color(0xFF6C63FF), "Procvič si pojmy ve formátu otázka/odpověď"),
    ToolEntry(AiTool.PRACTICE, "Procvičování", Icons.Default.Calculate, Color(0xFF20B2AA), "Úlohy ke článku s řešením"),
    ToolEntry(AiTool.REAL_WORLD, "Reálný svět", Icons.Default.Public, Color(0xFF4CAF50), "Jak se téma využívá v reálném životě"),
    ToolEntry(AiTool.WHAT_IF, "Co kdyby?", Icons.Default.Lightbulb, Color(0xFFFF9800), "Myšlenkový experiment k tématu"),
    ToolEntry(AiTool.FEYNMAN, "Zkus to vysvětlit", Icons.Default.RecordVoiceOver, Color(0xFFE91E63), "Napiš vysvětlení – AI ohodnotí tvé porozumění"),
    ToolEntry(AiTool.EXPLAIN, "Vysvětli větu", Icons.Default.QuestionMark, Color(0xFF2196F3), "Vlož větu nebo pojem k vysvětlení"),
)

@Composable
private fun AiToolGrid(onToolSelected: (AiTool) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
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
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(msg, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Zkusit znovu") }
    }
}

// ── Flashcards ────────────────────────────────────────────────────────────────

@Composable
private fun FlashcardsContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsState()
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
                        Icon(Icons.Default.ChevronLeft, null)
                        Text("Předchozí")
                    }
                    Button(onClick = vm::nextFlashcard, enabled = index < total - 1) {
                        Text("Další")
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

// ── Real World ────────────────────────────────────────────────────────────────

@Composable
private fun RealWorldContent(vm: AiToolsViewModel, postId: Int) {
    val state by vm.state.collectAsState()
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
    val state by vm.state.collectAsState()
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
                    Icon(Icons.AutoMirrored.Filled.Send, null)
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
                            Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
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
    val state by vm.state.collectAsState()
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
                Icon(Icons.Default.RecordVoiceOver, null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
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
            Icon(Icons.Default.Check, null)
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
    val state by vm.state.collectAsState()
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
                            onToggle = { vm.toggleProblemExpanded(i) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PracticeProblemCard(index: Int, problem: PracticeProblem, isExpanded: Boolean, onToggle: () -> Unit) {
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
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
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
