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
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Category
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.navigation.NavController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.text.font.FontWeight
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakSearch(context: Context): Int {
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
fun SearchScreen(navController: NavController, searchRequestFocus: androidx.compose.runtime.MutableState<Boolean>) {
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var debouncedSearchText by remember { mutableStateOf("") }
    var showTotalOverlay by remember { mutableStateOf(false) }
    val totalPoints by PointsManager.totalPoints.collectAsState()

    // Funce pro zvýraznění textu
    @Composable
    fun highlightText(text: String, query: String): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        val isDark = isSystemInDarkTheme()
        val highlightBackground = if (isDark) MaterialTheme.colorScheme.secondaryContainer else Color.Yellow
        val highlightTextColor = if (isDark) MaterialTheme.colorScheme.onSecondaryContainer else Color.Black
        val builder = buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                val idx = lowerText.indexOf(lowerQuery, i)
                if (idx == -1) {
                    append(text.substring(i))
                    break
                }
                append(text.substring(i, idx))
                withStyle(SpanStyle(background = highlightBackground, color = highlightTextColor)) {
                    append(text.substring(idx, idx + query.length))
                }
                i = idx + query.length
            }
        }
        return builder
    }

    // Vrátí úryvek z content s kontextem kolem hledaného výrazu a zvýrazněním
    @Composable
    fun getSnippetWithHighlight(content: String, query: String, contextLen: Int = 40, fallbackLen: Int = 80): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(content.take(fallbackLen))
        val lowerContent = content.lowercase()
        val lowerQuery = query.lowercase()
        val idx = lowerContent.indexOf(lowerQuery)
        if (idx == -1) {
            // Pokud výraz není v content, zobraz začátek content
            return AnnotatedString(content.take(fallbackLen))
        }
        val start = maxOf(0, idx - contextLen)
        val end = minOf(content.length, idx + query.length + contextLen)
        val snippet = content.substring(start, end)
        // Zvýraznění v rámci snippet
        return highlightText(snippet, query)
    }

    // Načítání postů pouze jednou při spuštění přes ApiClient
    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            val loadedPosts = withContext(Dispatchers.IO) { ApiClient.apiService.getPosts() }
            posts = loadedPosts
        } catch (e: Exception) {
            error = "Chyba: ${e.message ?: "Neznámá chyba"}"
        }
        isLoading = false
    }

    // Načítání kategorií při spuštění
    LaunchedEffect(Unit) {
        try {
            val loadedCategories = withContext(Dispatchers.IO) { ApiClient.apiService.getCategories() }
            categories = loadedCategories
        } catch (_: Exception) {}
    }

    // Debounce pro vyhledávání
    LaunchedEffect(searchText) {
        delay(400)
        debouncedSearchText = searchText
    }

    // Filtrování postů realtime podle debouncedSearchText
    val filteredPosts = if (debouncedSearchText.isBlank()) emptyList() else posts.filter {
        it.title.contains(debouncedSearchText, ignoreCase = true) || it.content.contains(debouncedSearchText, ignoreCase = true)
    }

    // Filtrování kategorií podle debouncedSearchText
    val filteredCategories = if (debouncedSearchText.isBlank()) emptyList() else categories.filter {
        it.name.contains(debouncedSearchText, ignoreCase = true)
    }

    // ✅ Celý obsah obrazovky je nyní v jednom hlavním Boxu.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(
                title = { Text("Vyhledávání", style = MaterialTheme.typography.titleLarge) },
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
                    
                    // Streak button s počtem dní
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreakSearch(context)
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
                scrollBehavior = scrollBehavior,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchBar(
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onSearch = { isSearchActive = false },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("Vyhledat kategorii nebo obsah článku...") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Výsledky hledání realtime
                    when {
                        isLoading -> CircularProgressIndicator()
                        error != null && error.orEmpty().isNotBlank() -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        filteredPosts.isEmpty() && filteredCategories.isEmpty() && searchText.isNotBlank() -> Text("Nenalezeno žádné výsledky.")
                        else -> Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Výsledky kategorií
                            if (filteredCategories.isNotEmpty()) {
                                Text("Kategorie:", style = MaterialTheme.typography.titleSmall)
                                filteredCategories.forEach { category ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate("categoryList/${category.name}")
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                highlightText(category.name, searchText),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            // Výsledky postů
                            if (filteredPosts.isNotEmpty()) {
                                Text("Články:", style = MaterialTheme.typography.titleSmall)
                            }
                            filteredPosts.forEach { post ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("postDetail/${post.id}")
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            highlightText(post.title, searchText),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            getSnippetWithHighlight(post.content, searchText),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ✅ Overlay se teď zobrazí na nejvyšší úrovni, tedy přes všechno.
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }
    }
}
