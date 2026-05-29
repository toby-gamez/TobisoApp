package com.tobiso.tobisoappnative.screens

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.viewmodel.ai.AiChatViewModel
import com.tobiso.tobisoappnative.viewmodel.ai.AiChatIntent
import com.tobiso.tobisoappnative.viewmodel.ai.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    postId: Int,
    postTitle: String,
    firstUserMessage: String,
    sessionId: Long = -1L,
    navController: NavController
) {
    val vm: AiChatViewModel = hiltViewModel<AiChatViewModel, AiChatViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(postId, postTitle, Uri.decode(firstUserMessage), sessionId)
        }
    )
    val state by vm.uiState.collectAsState()
    val messages = state.messages
    val isLoading = state.isLoading
    val error = state.error
    val remainingQuestions = state.remainingQuestions
    val limitReached = state.limitReached

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var animatedIndices by remember { mutableStateOf(emptySet<Int>()) }

    // For resumed sessions, messages from DB load asynchronously.
    // Int.MAX_VALUE means "no new messages yet" — nothing animates.
    // Once DB messages arrive, this is set to their count so only subsequent messages animate.
    var newMessagesStartIndex by remember {
        mutableStateOf(if (sessionId < 0L) 0 else Int.MAX_VALUE)
    }
    LaunchedEffect(messages.size) {
        if (newMessagesStartIndex == Int.MAX_VALUE && messages.isNotEmpty()) {
            newMessagesStartIndex = messages.size
        }
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("AI asistent", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                remainingQuestions?.let { remaining ->
                    Text(
                        text = "Zbývá: $remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        )

        // Připojený článek
        val decodedTitle = Uri.decode(postTitle)
        if (decodedTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = decodedTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(items = messages, key = { _, msg -> msg.id }) { index, message ->
                val shouldAnimate = message.role == "assistant" && index >= newMessagesStartIndex && index !in animatedIndices
                ChatBubble(
                    message = message,
                    animate = shouldAnimate,
                    onAnimationComplete = { animatedIndices = animatedIndices + index }
                )
            }

            if (isLoading) {
                item { ThinkingBubble() }
            }
        }

        // Chyba
        error?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (limitReached) {
            Button(
                onClick = { navController.navigate(ShopRoute) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("🤖 Koupit více otázek v obchodě")
            }
        }

        // Input řádek
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nějaké doplňující otázky...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI hvězdiček",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    enabled = !limitReached,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                vm.onIntent(AiChatIntent.SendMessage(inputText))
                                inputText = ""
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                SmallFloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            vm.onIntent(AiChatIntent.SendMessage(inputText))
                            inputText = ""
                        }
                    },
                    containerColor = if (inputText.isNotBlank() && !isLoading && !limitReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (inputText.isNotBlank() && !isLoading && !limitReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat"
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    var textWidth by remember { mutableFloatStateOf(0f) }
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val shimmerBrush = if (textWidth > 0f) {
        Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.3f),
                baseColor,
                baseColor.copy(alpha = 0.3f),
            ),
            start = Offset(shimmerOffset * textWidth * 2f - textWidth, 0f),
            end = Offset(shimmerOffset * textWidth * 2f, 0f)
        )
    } else {
        Brush.linearGradient(listOf(baseColor, baseColor))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Přemýšlím",
                    style = MaterialTheme.typography.bodyMedium.copy(brush = shimmerBrush),
                    modifier = Modifier.onSizeChanged { textWidth = it.width.toFloat() }
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    animate: Boolean = false,
    onAnimationComplete: () -> Unit = {}
) {
    val isUser = message.role == "user"
    var displayedText by remember(message.content) {
        mutableStateOf(if (animate) "" else message.content)
    }

    LaunchedEffect(message.content) {
        if (animate) {
            message.content.forEachIndexed { i, _ ->
                displayedText = message.content.substring(0, i + 1)
                delay(12)
            }
            onAnimationComplete()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (isUser)
                        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                    else
                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .background(
                    if (isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = displayedText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
