package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.navigation.PostDetailRoute

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext

import com.tobiso.tobisoappnative.viewmodel.favorites.FavoritesViewModel
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tobiso.tobisoappnative.model.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController
) {
    val vm: FavoritesViewModel = hiltViewModel()
    val posts by vm.fetchedPosts.collectAsState()
    val favoritePosts by vm.favoritePosts.collectAsState()
    val snippets by vm.snippets.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            vm.loadSnippets()
        }
    }



    // Stav pro dialog potvrzení mazání
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteType by remember { mutableStateOf(0) } // 0 = snippets, 1 = posts

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Oblíbené", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
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
                        if (deleteType == 0) vm.clearSnippets() else vm.clearFavoritePosts()
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Žádné útržky",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Útržky ukládáš přímo v článcích.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snippets) { snippet ->
                            val postTitleState = produceState<String?>(initialValue = null, snippet.postId, posts) {
                                value = if (snippet.postId != 0) posts.find { it.id == snippet.postId }?.title else null
                            }
                            val postTitle = postTitleState.value
                            LaunchedEffect(postTitle, snippet.postId) {
                                if (postTitle == null && snippet.postId != 0) vm.fetchAndCachePost(snippet.postId)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let { mod ->
                                        if (snippet.postId != 0 && postTitle != null) mod.clickable {
                                            navController.navigate(PostDetailRoute(postId = snippet.postId))
                                        } else mod
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                                        Text(
                                            text = when {
                                                snippet.postId == 0 -> "Neznámý soubor"
                                                postTitle == null -> "Načítám název…"
                                                else -> postTitle
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = snippet.content, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = java.text.SimpleDateFormat("dd. MM. yyyy", java.util.Locale.forLanguageTag("cs-CZ")).apply {
                                                timeZone = java.util.TimeZone.getDefault()
                                            }.format(snippet.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(onClick = { vm.removeSnippet(snippet) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Odstranit útržek",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Žádné oblíbené články",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Hvězdičku přidáš u každého článku.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(favoritePosts) { post ->
                            val locale = java.util.Locale.forLanguageTag("cs-CZ")
                            val inputFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            val outputFmt = java.text.SimpleDateFormat("dd. MM. yyyy", locale).apply { timeZone = java.util.TimeZone.getDefault() }
                            val formatted = listOfNotNull(post.lastEdit, post.lastFix, post.createdAt)
                                .mapNotNull { try { inputFmt.parse(it) } catch (_: Exception) { null } }
                                .maxOrNull()?.let { outputFmt.format(it) } ?: ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(PostDetailRoute(postId = post.id)) }
                                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = post.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (formatted.isNotBlank()) {
                                        Text(
                                            text = formatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                IconButton(onClick = { vm.unsavePost(post.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Odebrat z oblíbených",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
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
