package com.example.tobisoappnative.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.compose.ui.unit.Dp
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.utils.normalizeText
import com.example.tobisoappnative.viewmodel.MainViewModel
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
    collapsedHeight: Dp = 20.dp
) {
    var searchText by remember { mutableStateOf("") }
    var debouncedSearchText by remember { mutableStateOf("") }
    val expanded by viewModel.searchBarExpanded.collectAsState(initial = initialExpanded)
    val posts by viewModel.posts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Debounce pro vyhledávání
    LaunchedEffect(searchText) {
        delay(400)
        debouncedSearchText = searchText.trim()
    }

    // Filtrování výsledků
    val normQuery = normalizeText(debouncedSearchText.trim())
    val filteredCategories = if (debouncedSearchText.isBlank()) emptyList() else {
        categories.filter {
            normalizeText(it.name).contains(normQuery)
        }
    }
    val filteredPosts = if (debouncedSearchText.isBlank()) emptyList() else {
        posts.filter {
            // Vyfiltrujeme články s categoryId 42 nebo bez kategorie
            val hasValidCategory = it.categoryId != null && it.categoryId != 42
            hasValidCategory && (
                normalizeText(it.title).contains(normQuery) ||
                normalizeText(it.content).contains(normQuery)
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
                            viewModel.setSearchBarExpanded(false)
                        } else if (dragAmount.y < -20f && !expanded) {
                            viewModel.setSearchBarExpanded(true)
                        }
                    }
                }
        ) {
            // Výsledky hledání (jen když je rozbaleno)
            AnimatedVisibility(
                visible = expanded && debouncedSearchText.isNotBlank() && 
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
                                            navController?.navigate("categoryList/${category.name}")
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
                                            searchText = ""
                                            navController?.navigate("postDetail/${post.id}")
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
                                            post.content.take(80) + "...",
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
                visible = expanded && debouncedSearchText.isNotBlank() && 
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
                            .clickable { viewModel.setSearchBarExpanded(true) },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Prohledat celý svět vědění...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Hledat")
                        },
                        trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Smazat")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
