package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.Post
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val postsState = viewModel.posts.collectAsState()
    val posts: List<Post> = postsState.value
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val otherCategoryId = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    LaunchedEffect(Unit) {
        viewModel.loadPosts(otherCategoryId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Více", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        val tertiaryColor = MaterialTheme.colorScheme.tertiary
                        val points = remember { mutableStateOf(PointsManager.getPoints()) }
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
            },
            content = { innerPadding ->
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val gridColumns = if (isLandscape) 3 else 1
                val cardModifier = Modifier
                    .padding(8.dp)
                val cardShape = RoundedCornerShape(16.dp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    item(span = { GridItemSpan(1) }) {
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("feedback") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Zpětná vazba", style = MaterialTheme.typography.titleMedium)
                                Text("Napište nám, co byste chtěli změnit nebo vylepšit.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item(span = { GridItemSpan(1) }) {
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("about") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("O aplikaci", style = MaterialTheme.typography.titleMedium)
                                Text("Všechno o aplikaci", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item(span = { GridItemSpan(1) }) {
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("changelog") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Deník změn", style = MaterialTheme.typography.titleMedium)
                                Text("Všechno důležité, co bylo změněno", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item(span = { GridItemSpan(1) }) {
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("favorites") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Oblíbené", style = MaterialTheme.typography.titleMedium)
                                Text("Tvé uložené útržky a články, které nevyuživáš. :(", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(filteredPosts) { post ->
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("postDetail/${post.id}") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                                val updated = post.updatedAt
                                val formatted = updated?.let {
                                    try {
                                        val formatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", java.util.Locale.forLanguageTag("cs-CZ"))
                                        formatter.format(it)
                                    } catch (_: Exception) {
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
                if (showTotalOverlay) {
                    FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                    LaunchedEffect(showTotalOverlay) {
                        delay(2200)
                        showTotalOverlay = false
                    }
                }
            }
        )
    }
}
