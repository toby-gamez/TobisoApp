package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.material.icons.filled.Stars
import androidx.compose.ui.text.font.FontWeight
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.MultiplierIndicator
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.tobisoappnative.utils.StreakUtils
import com.example.tobisoappnative.utils.normalizeText

// Helper funkce pro získání aktuální řady (nyní s freeze podporou)
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakSearch(context: Context): Int {
    return StreakUtils.getCurrentStreak(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, searchRequestFocus: androidx.compose.runtime.MutableState<Boolean>, viewModel: MainViewModel = viewModel()) {
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Používáme data z ViewModelu místo lokálního stavu
    val posts by viewModel.posts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val postError by viewModel.postError.collectAsState()
    val categoryError by viewModel.categoryError.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var debouncedSearchText by remember { mutableStateOf("") }
    var showTotalOverlay by remember { mutableStateOf(false) }
    val totalPoints by PointsManager.totalPoints.collectAsState()

    // Funce pro zvýraznění textu (ignoruje case i diakritiku)
    @Composable
    fun highlightText(text: String, query: String): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)
    val normText = normalizeText(text)
    val normQuery = normalizeText(query)
        val isDark = isSystemInDarkTheme()
        val highlightBackground = if (isDark) MaterialTheme.colorScheme.secondaryContainer else Color.Yellow
        val highlightTextColor = if (isDark) MaterialTheme.colorScheme.onSecondaryContainer else Color.Black
        val builder = buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                val idx = normText.indexOf(normQuery, i)
                if (idx == -1) {
                    append(text.substring(i))
                    break
                }
                // idx is location in normalized text; map to original by using same index positions
                val matchStart = idx
                val matchEnd = idx + normQuery.length
                append(text.substring(i, matchStart))
                withStyle(SpanStyle(background = highlightBackground, color = highlightTextColor)) {
                    // Use original substring for visual fidelity
                    append(text.substring(matchStart, matchEnd))
                }
                i = matchEnd
            }
        }
        return builder
    }

    // Vrátí úryvek z content s kontextem kolem hledaného výrazu a zvýrazněním
    @Composable
    fun getSnippetWithHighlight(content: String, query: String, contextLen: Int = 40, fallbackLen: Int = 80): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(content.take(fallbackLen))
    val normContent = normalizeText(content)
    val normQuery = normalizeText(query)
        val idx = normContent.indexOf(normQuery)
        if (idx == -1) {
            // Pokud výraz není v content, zobraz začátek content
            return AnnotatedString(content.take(fallbackLen))
        }
        val start = maxOf(0, idx - contextLen)
        val end = minOf(content.length, idx + normQuery.length + contextLen)
        val snippet = content.substring(start, end)
        // Zvýraznění v rámci snippet
        return highlightText(snippet, query)
    }

    // Načítání dat z ViewModelu (funguje i v offline režimu)
    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            isLoading = true
            viewModel.loadPosts()
        }
        if (categories.isEmpty()) {
            viewModel.loadCategories()
        }
        isLoading = false
    }

    // Debounce pro vyhledávání
    LaunchedEffect(searchText) {
        delay(400)
        debouncedSearchText = searchText
    }

    // Filtrování postů realtime podle debouncedSearchText
    val filteredPosts = if (debouncedSearchText.isBlank()) emptyList() else {
        val normQuery = normalizeText(debouncedSearchText)
        posts.filter {
            normalizeText(it.title).contains(normQuery) || normalizeText(it.content).contains(normQuery)
        }
    }

    // Filtrování kategorií podle debouncedSearchText
    val filteredCategories = if (debouncedSearchText.isBlank()) emptyList() else {
        val normQuery = normalizeText(debouncedSearchText)
        categories.filter {
            normalizeText(it.name).contains(normQuery)
        }
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
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Zobrazení bodů s novým designem
                    val totalPoints by PointsManager.totalPoints.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
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
                    
                    // Streak button s počtem dní (s freeze podporou)
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    // Sledování změn v freeze
                    val availableFreezes by com.example.tobisoappnative.StreakFreezeManager.availableFreezes.collectAsState()
                    val usedFreezes by com.example.tobisoappnative.StreakFreezeManager.usedFreezes.collectAsState()
                    
                    LaunchedEffect(availableFreezes, usedFreezes) {
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
                        postError != null && postError.orEmpty().isNotBlank() -> Text(postError.orEmpty(), color = MaterialTheme.colorScheme.error)
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
