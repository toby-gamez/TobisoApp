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
import androidx.compose.ui.unit.sp
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.Post
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.MultiplierIndicator
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakMore(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
    val streakDays = sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
    
    if (streakDays.isEmpty()) return 0
    
    val sortedDates = streakDays.map { LocalDate.parse(it) }.sorted()
    if (sortedDates.size == 1) return 1
    
    var currentStreak = 0
    val today = LocalDate.now()
    val lastRecordedDay = sortedDates.last()
    
    if (lastRecordedDay == today || lastRecordedDay == today.minusDays(1)) {
        var expectedDate = lastRecordedDay
        for (i in sortedDates.indices.reversed()) {
            if (sortedDates[i] == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
    }
    
    return currentStreak
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val postsState = viewModel.posts.collectAsState()
    val posts: List<Post> = postsState.value
    val postLoading by viewModel.postLoading.collectAsState()
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val otherCategoryId = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    LaunchedEffect(Unit) {
        viewModel.loadPosts(otherCategoryId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(
                title = { Text("Více", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    // Zobrazení bodů s novým designem
                    val totalPoints by PointsManager.totalPoints.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showTotalOverlay = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Body",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = totalPoints.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Streak button s počtem dní
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreakMore(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigate("streak") }
                    ) {
                        if (currentStreak.value > 0) {
                            Text(
                                text = currentStreak.value.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val gridColumns = if (isLandscape) 3 else 1
            val cardModifier = Modifier
                .padding(8.dp)
            val cardShape = RoundedCornerShape(16.dp)

            if (postLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Načítání dalšího obsahu...")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = { navController.navigate("shop") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Obchod",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Obchod", 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Utrať své body za streak freeze, citáty, ikony a zvířátka!", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        onClick = { navController.navigate("backpack") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = "Aktovka",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Aktovka", 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Prohlédni si své koupené věci - citáty, ikony a zvířátka!", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
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

                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("updater") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Aktualizátor", style = MaterialTheme.typography.titleMedium)
                            Text("Aktualizuj si aplikaci, ať ti nic neunikne!", style = MaterialTheme.typography.bodySmall)
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
                    }
                }
            }
        }
    }

        // Overlay na nejvyšší úrovni
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }
    }
}