package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val categoriesState = viewModel.categories.collectAsState()
    val postsState = viewModel.posts.collectAsState()
    val categories: List<Category> = categoriesState.value
    val posts: List<Post> = postsState.value
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }

    // ID kategorie "Other" je 42
    val otherCategoryId: Int = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    LaunchedEffect(Unit) {
        viewModel.loadPosts(otherCategoryId)
    }

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        LargeTopAppBar(
            title = { Text("Více", style = MaterialTheme.typography.titleLarge) },
            actions = {
                // Kulaté tlačítko s body vlevo od ohýnku
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val points = remember { mutableStateOf(PointsManager.getPoints()) }
                // Aktualizace bodů při změně
                LaunchedEffect(Unit) {
                    PointsManager.totalPoints.collect { total ->
                        points.value = total
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .background(
                            color = tertiaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { showTotalOverlay = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = points.value.toString(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { navController.navigate("streak") }) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Streak",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tlačítko pro zpětnou vazbu jako Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("feedback") },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Zpětná vazba", style = MaterialTheme.typography.titleMedium)
                            Text("Napište nám, co byste chtěli změnit nebo vylepšit.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("about") },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("O aplikaci", style = MaterialTheme.typography.titleMedium)
                            Text("Všechno o aplikaci", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("changelog") },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Deník změn", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Všechno důležité, co bylo změněno",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("favorites") },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Oblíbené", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tvé uložené útržky a příspěvky, které nevyuživáš. :(",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (!filteredPosts.isEmpty()) {
                items(filteredPosts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { navController.navigate("postDetail/${post.id}") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                                val updated = post.updatedAt
                                val formatted = updated?.let {
                                    try {
                                        val formatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", java.util.Locale("cs", "CZ"))
                                        formatter.format(it)
                                    } catch (e: Exception) {
                                        ""
                                    }
                                } ?: ""
                                if (formatted.isNotBlank()) {
                                    Text(text = "Upraveno: $formatted", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}