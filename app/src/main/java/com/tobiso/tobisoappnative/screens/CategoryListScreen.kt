package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Category
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import com.tobiso.tobisoappnative.components.FloatingSearchBar
import com.tobiso.tobisoappnative.viewmodel.categorylist.CategoryListViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CategoryListScreen(
    parentCategoryName: String,
    navController: NavController
) {
    val vm: CategoryListViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val favoritePosts by vm.favoritePosts.collectAsState()
    val categories = state.categories
    val categoryError = state.error
    val categoryLoading = state.isLoading
    val posts = state.posts
    val postError = state.error
    val postLoading = state.isLoading
    LaunchedEffect(Unit) { vm.loadCategories() }

    val parentCategory = categories.find { it.name == parentCategoryName }
    val filteredCategories = parentCategory?.let { parent ->
        categories.filter { it.parentId == parent.id }
    } ?: emptyList()

    val showConnectionError = parentCategoryName == "Mluvnice" && (parentCategory == null || filteredCategories.isEmpty())

    // Načtení postů při změně parentCategory
    LaunchedEffect(parentCategory?.id) {
        parentCategory?.id?.let { vm.loadPosts(it) }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 3 else 1

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("$parentCategoryName", style = MaterialTheme.typography.headlineLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        when {
            categoryLoading || postLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Načítání obsahu...")
                    }
                }
            }
            showConnectionError -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Chyba připojení nebo žádné podkategorie dostupné.", color = MaterialTheme.colorScheme.error)
                        if (categoryError != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Detail chyby:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                            Text(categoryError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            postError != null -> {
                // Zobrazení chybové hlášky na celé obrazovce při chybě načítání postů
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = "Chyba při načítání postů:",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = postError ?: "Neznámá chyba",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(CategoryListRoute(categoryName = category.name)) },
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, contentDescription = "Kategorie", modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                val filteredPosts = parentCategory?.let { parent ->
                    posts.filter { it.categoryId == parent.id }
                } ?: emptyList()
                // Zobrazení postů ke kategorii
                if (filteredCategories.isEmpty() && filteredPosts.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (postError != null) {
                                Text(
                                    text = "Chyba při načítání postů: ${postError}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (categoryError != null) {
                                Text(
                                    text = "Chyba při načítání kategorií: ${categoryError}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    items(filteredPosts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(PostDetailRoute(postId = post.id)) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Post", modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                                    // Show the most recent timestamp among `lastEdit`, `lastFix`, `createdAt`
                                    val locale = java.util.Locale("cs", "CZ")
                                    val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val outputFormatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale).apply {
                                        timeZone = TimeZone.getDefault()
                                    }

                                    val candidates = listOfNotNull(post.lastEdit, post.lastFix, post.createdAt)
                                    val latestDate = candidates.mapNotNull { ds ->
                                        try {
                                            inputFormatter.parse(ds)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }.maxOrNull()

                                    val formatted = latestDate?.let { outputFormatter.format(it) } ?: candidates.firstOrNull() ?: ""
                                    if (formatted.isNotBlank()) {
                                        Text(text = "Upraveno: $formatted", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                val isFavorite = favoritePosts.any { it.id == post.id }
                                IconButton(onClick = {
                                    if (isFavorite) vm.unsavePost(post.id) else vm.savePost(post)
                                }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
}