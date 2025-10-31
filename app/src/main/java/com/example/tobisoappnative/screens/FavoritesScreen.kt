package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.ui.theme.poppins
import com.example.tobisoappnative.ui.theme.poppins_regular
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.components.MultiplierIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.tobisoappnative.model.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val favoritePosts by viewModel.favoritePosts.collectAsState()
    val snippets by viewModel.snippets.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            viewModel.loadSnippets()
        }
    }



    // Stav pro dialog potvrzení mazání
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteType by remember { mutableStateOf(0) } // 0 = snippets, 1 = posts

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Oblíbené", style = MaterialTheme.typography.headlineLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                // Zobrazení aktivního multiplikátoru
                MultiplierIndicator()
                
                when (selectedTab) {
                    0 -> {
                        if (snippets.isNotEmpty()) {
                            IconButton(onClick = { 
                                showDeleteDialog = true
                                deleteType = 0 
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Smazat všechny útržky")
                            }
                        }
                    }
                    1 -> {
                        if (favoritePosts.isNotEmpty()) {
                            IconButton(onClick = { 
                                showDeleteDialog = true
                                deleteType = 1 
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Smazat všechny články")
                            }
                        }
                    }
                }
            }
        )
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Útržky") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Články") }
            )
        }
        
        // Dialog potvrzení
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Opravdu chcete smazat vše?") },
                text = { Text(if (deleteType == 0) "Tímto smažete všechny uložené útržky." else "Tímto smažete všechny oblíbené články.") },
                confirmButton = {
                    TextButton(onClick = {
                        if (deleteType == 0) viewModel.clearSnippets() else viewModel.clearFavoritePosts()
                        showDeleteDialog = false
                    }) {
                        Text("Ano")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Ne")
                    }
                }
            )
        }
        when (selectedTab) {
            0 -> {
                if (snippets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Nemáte žádné útržky.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(snippets) { snippet ->
                            val postTitleState = produceState<String?>(initialValue = null, snippet.postId, posts) {
                                if (snippet.postId != 0) {
                                    // Najdeme post v cached datech z ViewModelu
                                    value = posts.find { it.id == snippet.postId }?.title
                                } else {
                                    value = null
                                }
                            }
                            val postTitle = postTitleState.value
                            // If the title is not yet available but we have an ID, try fetching the post
                            LaunchedEffect(postTitle, snippet.postId) {
                                if (postTitle == null && snippet.postId != 0) {
                                    viewModel.fetchAndCachePost(snippet.postId)
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .let { mod ->
                                        if (snippet.postId != 0 && postTitle != null) mod.clickable {
                                            navController.navigate("postDetail/${snippet.postId}")
                                        } else mod
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                                        when {
                                            snippet.postId == 0 -> Text("Neznámý soubor", style = MaterialTheme.typography.titleSmall)
                                            postTitle == null -> Text("Načítám název...", style = MaterialTheme.typography.titleSmall)
                                            else -> Text(postTitle, style = MaterialTheme.typography.titleSmall)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = snippet.content, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Vytvořeno: " + java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", java.util.Locale.forLanguageTag("cs-CZ")).format(snippet.createdAt),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeSnippet(snippet) },
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Odstranit útržek",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (favoritePosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Nemáte žádné oblíbené články.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(favoritePosts) { post ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { navController.navigate("postDetail/${post.id}") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Description, contentDescription = "Post", modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                                        val updated = post.updatedAt
                                        val formatted = updated?.let { dateString ->
                                            try {
                                                val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", java.util.Locale.forLanguageTag("cs-CZ"))
                                                val outputFormatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", java.util.Locale.forLanguageTag("cs-CZ"))
                                                val date = inputFormatter.parse(dateString)
                                                date?.let { outputFormatter.format(it) } ?: dateString
                                            } catch (_: Exception) {
                                                dateString
                                            }
                                        } ?: ""
                                        if (formatted.isNotBlank()) {
                                            Text(text = "Upraveno: $formatted", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.unsavePost(post.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Odebrat z oblíbených",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
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
}
