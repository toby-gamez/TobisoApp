package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.viewmodel.ai.AiToolsViewModel

@Composable
fun SentenceSelectPanel(
    postContent: String?,
    vm: AiToolsViewModel,
    postId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()
    val accentColor = Color(0xFF2196F3)

    val paragraphs = remember(postContent) {
        if (postContent.isNullOrBlank()) emptyList()
        else extractSentenceParagraphs(postContent)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Drag handle
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }

            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Klepni na větu k vysvětlení",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }

            // Selected sentence + result area
            if (state.explainInput.isNotBlank() || state.isLoading || state.error != null || state.explainResult.isNotEmpty()) {
                HorizontalDivider()
                Column(Modifier.padding(12.dp)) {
                    if (state.explainInput.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Vybraná věta",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = accentColor
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(state.explainInput, style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .size(38.dp)
                                            .background(
                                                if (!state.isLoading) accentColor else accentColor.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                            .clickable(enabled = !state.isLoading) { vm.submitExplain(postId) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send, null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier.size(26.dp).clickable { vm.setExplainInput("") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Clear, null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when {
                        state.error != null -> {
                            Spacer(Modifier.height(8.dp))
                            Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { vm.submitExplain(postId) }) { Text("Zkusit znovu") }
                        }
                        state.explainResult.isNotEmpty() -> {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QuestionMark, null, tint = accentColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Vysvětlení", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(state.explainResult, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Sentence list from post
            if (paragraphs.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(paragraphs) { _, sentences ->
                        SentencePickerParagraph(
                            sentences = sentences,
                            selectedSentence = state.explainInput,
                            accentColor = accentColor,
                            onSentenceSelected = { vm.setExplainInput(it) }
                        )
                    }
                }
            } else {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = state.explainInput,
                        onValueChange = vm::setExplainInput,
                        label = { Text("Napište větu k vysvětlení...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(onSend = { vm.submitExplain(postId) }),
                        trailingIcon = {
                            IconButton(
                                onClick = { vm.submitExplain(postId) },
                                enabled = state.explainInput.isNotBlank() && !state.isLoading
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SentencePickerParagraph(
    sentences: List<String>,
    selectedSentence: String,
    accentColor: Color,
    onSentenceSelected: (String) -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val annotated = buildAnnotatedString {
        sentences.forEachIndexed { i, sentence ->
            val isSelected = selectedSentence == sentence
            pushStringAnnotation("S", i.toString())
            withStyle(
                SpanStyle(
                    color = if (isSelected) accentColor else contentColor,
                    background = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent
                )
            ) {
                append(sentence)
            }
            pop()
            if (i < sentences.size - 1) append(" ")
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 22.sp),
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(sentences, selectedSentence) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        layoutResult?.let { layout ->
                            val pos = layout.getOffsetForPosition(down.position)
                            annotated.getStringAnnotations("S", pos, pos)
                                .firstOrNull()?.let { ann ->
                                    onSentenceSelected(sentences[ann.item.toInt()])
                                }
                        }
                    }
                }
            }
    )
}

internal fun extractSentenceParagraphs(rawContent: String): List<List<String>> {
    var text = rawContent
    text = text.replace(Regex("```[\\s\\S]*?```"), "")
    text = text.replace(Regex("(?is)<video[^>]*>.*?</video>"), "")
    text = text.replace(Regex("<[^>]+>"), "")
    text = text.replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
    text = text.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
    text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
    text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
    text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")
    text = text.replace(Regex("`([^`]+)`"), "$1")
    text = text.replace(Regex("(?m)^-{3,}$"), "")
    text = text.replace(Regex("\\.{3}\\s*([\\s\\S]*?)\\s*\\.{3}"), "$1")
    text = text.replace(Regex("\\(--DOD-\\d+--\\)"), "")
    return text
        .split(Regex("\\n+"))
        .map { para ->
            para.trim()
                .split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.length in 15..300 }
        }
        .filter { it.isNotEmpty() }
        .take(25)
}
