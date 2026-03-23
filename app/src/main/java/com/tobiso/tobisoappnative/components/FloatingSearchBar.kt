package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.compose.ui.unit.Dp
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.utils.hasAiConsent
import com.tobiso.tobisoappnative.utils.normalizeText
import com.tobiso.tobisoappnative.utils.saveAiConsent
import com.tobiso.tobisoappnative.viewmodel.MainIntent
import com.tobiso.tobisoappnative.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// Helper funkce pro zvýraznění textu (ignoruje case i diakritiku)
@Composable
fun highlightText(text: String, query: String, isDark: Boolean): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val normText = normalizeText(text)
    val normQuery = normalizeText(query)
    val highlightBackground = MaterialTheme.colorScheme.secondaryContainer
    val highlightTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    val builder = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val idx = normText.indexOf(normQuery, i)
            if (idx == -1) {
                append(text.substring(i))
                break
            }
            val matchStart = idx
            val matchEnd = idx + normQuery.length
            append(text.substring(i, matchStart))
            withStyle(SpanStyle(background = highlightBackground, color = highlightTextColor)) {
                append(text.substring(matchStart, matchEnd))
            }
            i = matchEnd
        }
    }
    return builder
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingSearchBar(
    navController: NavHostController?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true,
    collapsedHeight: Dp = 20.dp,
    isOffline: Boolean = false,
    onAiClick: () -> Unit = {},
    onAiPostSelected: (Post) -> Unit = {},
    onAiSend: (Post, String) -> Unit = { _, _ -> }
) {
    var searchText by remember { mutableStateOf("") }
    var debouncedSearchText by remember { mutableStateOf("") }
    var aiMode by remember { mutableStateOf(false) }
    var attachedPost by remember { mutableStateOf<Post?>(null) }
    val focusRequester = remember { FocusRequester() }
    val state by viewModel.uiState.collectAsState()
    val expanded = state.searchBarExpanded
    val posts = state.posts
    val categories = state.categories
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    var showConsentDialog by remember { mutableStateOf(false) }

    // Debounce pro vyhledávání
    LaunchedEffect(searchText) {
        delay(400)
        debouncedSearchText = searchText.trim()
    }

    // Aktivovat klávesnici při zapnutí AI modu
    LaunchedEffect(aiMode) {
        if (aiMode) {
            delay(100) // počkáme na animaci
            focusRequester.requestFocus()
        }
    }

    // Refokusovat klávesnici po připojení článku
    LaunchedEffect(attachedPost) {
        if (attachedPost != null) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    // Filtrování výsledků
    val normQuery = normalizeText(debouncedSearchText.trim())
    val filteredCategories = if (aiMode || debouncedSearchText.isBlank() || attachedPost != null) emptyList() else {
        categories.filter {
            normalizeText(it.name).contains(normQuery)
        }
    }
    val filteredPosts = if (debouncedSearchText.isBlank() || attachedPost != null) emptyList() else {
        posts.filter {
            val hasValidCategory = it.categoryId != null && it.categoryId != 42
            hasValidCategory && (
                normalizeText(it.title).contains(normQuery) ||
                (!aiMode && normalizeText(it.content ?: "").contains(normQuery))
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .zIndex(10f)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        // dragAmount.y > 0 => dragging down; < 0 => dragging up
                        if (dragAmount.y > 20f && expanded) {
                            viewModel.onIntent(MainIntent.SetSearchBarExpanded(false))
                        } else if (dragAmount.y < -20f && !expanded) {
                            viewModel.onIntent(MainIntent.SetSearchBarExpanded(true))
                        }
                    }
                }
        ) {
            // Výsledky hledání (jen když je rozbaleno)
            AnimatedVisibility(
                visible = expanded && debouncedSearchText.isNotBlank() && attachedPost == null &&
                          (filteredCategories.isNotEmpty() || filteredPosts.isNotEmpty()),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredCategories.isNotEmpty()) {
                            item {
                                Text(
                                    "Kategorie:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(filteredCategories) { category ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchText = ""
                                            navController?.navigate(CategoryListRoute(categoryName = category.name))
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        highlightText(category.name, debouncedSearchText, isDark),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }
                        if (filteredPosts.isNotEmpty()) {
                            item {
                                Text(
                                    "Články:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(filteredPosts) { post ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (aiMode) {
                                                attachedPost = post
                                                searchText = ""
                                                onAiPostSelected(post)
                                            } else {
                                                searchText = ""
                                                navController?.navigate(PostDetailRoute(postId = post.id))
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            highlightText(post.title, debouncedSearchText, isDark),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            (post.content ?: "").take(80) + "...",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Žádné výsledky
            AnimatedVisibility(
                visible = expanded && debouncedSearchText.isNotBlank() && attachedPost == null &&
                          filteredCategories.isEmpty() &&
                          filteredPosts.isEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "Nenalezeny žádné výsledky",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Pokud není rozbaleno, ukážeme opravdu tenký handle (vizuálně ne vyhledávání)
            AnimatedVisibility(visible = !expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(collapsedHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Klikací kontejner přesunutý dolů tak, aby odpovídal viditelnému handle
                    Box(
                        modifier = Modifier
                            .offset(y = 15.dp)
                            .size(width = 48.dp, height = 28.dp)
                            .clickable { viewModel.onIntent(MainIntent.SetSearchBarExpanded(true)) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Vlastní vizuální tenký pruh
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column {
                            if (aiMode && attachedPost != null) {
                                val post = attachedPost
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = post?.title ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { attachedPost = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Odpojit článek",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = {
                                    Text(
                                        when {
                                            aiMode && attachedPost != null -> "Napište dotaz..."
                                            aiMode -> "Připojit článek..."
                                            else -> "Najít cokoliv..."
                                        }
                                    )
                                },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (aiMode) Icons.Filled.AutoAwesome else Icons.Default.Search,
                                    contentDescription = if (aiMode) "AI mod" else "Hledat",
                                    tint = if (aiMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            },
                            trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Smazat")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = !aiMode,
                            maxLines = if (aiMode) 5 else 1,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                        } // Column
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        SmallFloatingActionButton(
                            onClick = {
                                if (isOffline && !aiMode) return@SmallFloatingActionButton
                                when {
                                    aiMode && attachedPost != null && searchText.isNotEmpty() -> {
                                        val post = attachedPost ?: return@SmallFloatingActionButton
                                        onAiSend(post, searchText)
                                        searchText = ""
                                    }
                                    aiMode -> { aiMode = false; searchText = ""; attachedPost = null }
                                    else -> {
                                        if (hasAiConsent(context)) {
                                            aiMode = true; searchText = ""; onAiClick()
                                        } else {
                                            showConsentDialog = true
                                        }
                                    }
                                }
                            },
                            containerColor = if (isOffline && !aiMode)
                                MaterialTheme.colorScheme.surfaceVariant
                            else if (aiMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isOffline && !aiMode)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else if (aiMode) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                imageVector = when {
                                    aiMode && attachedPost != null && searchText.isNotEmpty() -> Icons.Default.Send
                                    aiMode -> Icons.Default.Search
                                    else -> Icons.Filled.AutoAwesome
                                },
                                contentDescription = when {
                                    isOffline && !aiMode -> "AI nedostupné (offline)"
                                    aiMode && attachedPost != null && searchText.isNotEmpty() -> "Odeslat"
                                    aiMode -> "Přepnout na vyhledávání"
                                    else -> "AI vyhledávání"
                                }
                            )
                        }
                        if (isOffline && !aiMode) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConsentDialog) {
        AiConsentDialog(
            navController = navController,
            onAccepted = {
                saveAiConsent(context)
                showConsentDialog = false
                aiMode = true
                searchText = ""
                onAiClick()
            },
            onDismissed = { showConsentDialog = false }
        )
    }
}
