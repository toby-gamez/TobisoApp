package com.example.tobisoappnative.screens

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post  
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import java.time.LocalDate

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakAllQuestions(context: Context): Int {
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
fun AllQuestionsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val allQuestions by viewModel.allQuestions.collectAsState()
    val filteredQuestions by viewModel.filteredQuestions.collectAsState()
    val allQuestionsError by viewModel.allQuestionsError.collectAsState()
    val allQuestionsLoading by viewModel.allQuestionsLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val questionsPosts by viewModel.questionsPosts.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedPostId by viewModel.selectedPostId.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showTotalOverlay by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Auto-hide filters on scroll
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && showFilters) {
            showFilters = false
        }
    }

    // Načtení dat při startu (nyní funguje v online i offline režimu)
    LaunchedEffect(isOffline) {
        viewModel.loadCategories() // Načteme kategorie pro filtry
        viewModel.loadAllQuestions() // Načteme otázky (offline/online se řeší v ViewModelu)
    }

    // Vyčištění stavu při opuštění obrazovky
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAllQuestions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LargeTopAppBar(
                title = { 
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val fontSize = (36 - (16 * collapsedFraction)).sp
                    Text(
                        text = "Procvičování",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize),
                        maxLines = 1
                    )
                },
                actions = {
                    // Filter toggle button
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Filled.FilterList, 
                            contentDescription = "Filtry",
                            tint = if (selectedCategoryId != null || selectedPostId != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    
                    if (!isOffline) {
                        IconButton(onClick = { 
                            coroutineScope.launch {
                                isRefreshing = true
                                viewModel.loadAllQuestions()
                                isRefreshing = false
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Obnovit")
                        }
                    }
                    // Points button
                    val totalPoints by PointsManager.totalPoints.collectAsState()
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                    
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
                            text = totalPoints.toString(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Streak button
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreakAllQuestions(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { navController.navigate("streak") }
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
            
            if (showTotalOverlay) {
                val totalPoints by PointsManager.totalPoints.collectAsState()
                FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                LaunchedEffect(showTotalOverlay) {
                    kotlinx.coroutines.delay(2200)
                    showTotalOverlay = false
                }
            }
        }

        // Filter Panel
        if (showFilters) {
            FilterPanel(
                categories = categories,
                posts = questionsPosts,
                allQuestions = allQuestions,
                selectedCategoryId = selectedCategoryId,
                selectedPostId = selectedPostId,
                expandedCategories = expandedCategories,
                onCategorySelected = { categoryId ->
                    viewModel.setQuestionsFilter(categoryId = categoryId)
                },
                onPostSelected = { postId ->
                    viewModel.setQuestionsFilter(postId = postId)
                },
                onClearFilter = {
                    viewModel.clearQuestionsFilter()
                },
                onCategoryExpanded = { categoryId ->
                    expandedCategories = if (expandedCategories.contains(categoryId)) {
                        expandedCategories - categoryId
                    } else {
                        expandedCategories + categoryId
                    }
                }
            )
        }

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (!isOffline) {
                    isRefreshing = true
                    coroutineScope.launch {
                        viewModel.loadAllQuestions()
                        isRefreshing = false
                    }
                } else {
                    // V offline režimu jen resetujeme refresh state
                    isRefreshing = false
                }
            }
        ) {
            when {

                allQuestionsError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                allQuestionsError ?: "Neznámá chyba",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { 
                                viewModel.loadAllQuestions()
                            }) {
                                Text("Zkusit znovu")
                            }
                        }
                    }
                }

                allQuestionsLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Načítání otázek...")
                        }
                    }
                }

                allQuestions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.QuestionMark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Žádné otázky nejsou k dispozici",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Zpět")
                            }
                        }
                    }
                }

                else -> {
                    QuestionsContent(
                        questions = filteredQuestions,
                        allQuestionsCount = allQuestions.size,
                        posts = questionsPosts,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        selectedPostId = selectedPostId,
                        navController = navController,
                        onClearFilter = { viewModel.clearQuestionsFilter() },
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    categories: List<Category>,
    posts: List<Post>,
    allQuestions: List<com.example.tobisoappnative.model.Question>,
    selectedCategoryId: Int?,
    selectedPostId: Int?,
    expandedCategories: Set<Int>,
    onCategorySelected: (Int?) -> Unit,
    onPostSelected: (Int?) -> Unit,
    onClearFilter: () -> Unit,
    onCategoryExpanded: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (selectedCategoryId != null || selectedPostId != null) {
                    TextButton(onClick = onClearFilter) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zrušit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Root categories
                val rootCategories = categories.filter { it.parentId == null }
                items(rootCategories) { category ->
                    CategoryFilterItem(
                        category = category,
                        allCategories = categories,
                        posts = posts,
                        allQuestions = allQuestions,
                        selectedCategoryId = selectedCategoryId,
                        selectedPostId = selectedPostId,
                        expandedCategories = expandedCategories,
                        onCategorySelected = onCategorySelected,
                        onPostSelected = onPostSelected,
                        onCategoryExpanded = onCategoryExpanded,
                        level = 0
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterItem(
    category: Category,
    allCategories: List<Category>,
    posts: List<Post>,
    allQuestions: List<com.example.tobisoappnative.model.Question>,
    selectedCategoryId: Int?,
    selectedPostId: Int?,
    expandedCategories: Set<Int>,
    onCategorySelected: (Int?) -> Unit,
    onPostSelected: (Int?) -> Unit,
    onCategoryExpanded: (Int) -> Unit,
    level: Int
) {
    val hasChildren = allCategories.any { it.parentId == category.id }
    val isExpanded = expandedCategories.contains(category.id)
    val isSelected = selectedCategoryId == category.id
    val questionCount = getQuestionCountInCategory(category.id, allCategories, posts, allQuestions)
    
    // Skip categories with no questions
    if (questionCount == 0) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else Color.Transparent
                )
                .clickable { 
                    if (isSelected) {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(category.id)
                    }
                }
                .padding(
                    start = (level * 16).dp + 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = { onCategoryExpanded(category.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                category.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                "$questionCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Show children and posts if expanded or if no children
        if (isExpanded || !hasChildren) {
            // Child categories
            val childCategories = allCategories.filter { it.parentId == category.id }
            childCategories.forEach { childCategory ->
                CategoryFilterItem(
                    category = childCategory,
                    allCategories = allCategories,
                    posts = posts,
                    allQuestions = allQuestions,
                    selectedCategoryId = selectedCategoryId,
                    selectedPostId = selectedPostId,
                    expandedCategories = expandedCategories,
                    onCategorySelected = onCategorySelected,
                    onPostSelected = onPostSelected,
                    onCategoryExpanded = onCategoryExpanded,
                    level = level + 1
                )
            }

            // Posts in this category
            val postsInCategory = posts.filter { it.categoryId == category.id }
            postsInCategory.forEach { post ->
                val postQuestionCount = allQuestions.count { it.postId == post.id }
                if (postQuestionCount > 0) {
                    PostFilterItem(
                        post = post,
                        questionCount = postQuestionCount,
                        isSelected = selectedPostId == post.id,
                        onPostSelected = onPostSelected,
                        level = level + 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PostFilterItem(
    post: Post,
    questionCount: Int,
    isSelected: Boolean,
    onPostSelected: (Int?) -> Unit,
    level: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .clickable { 
                if (isSelected) {
                    onPostSelected(null)
                } else {
                    onPostSelected(post.id)
                }
            }
            .padding(
                start = (level * 16).dp + 32.dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Article,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            post.title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            "$questionCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionsContent(
    questions: List<com.example.tobisoappnative.model.Question>,
    allQuestionsCount: Int,
    posts: List<Post>,
    categories: List<Category>,
    selectedCategoryId: Int?,
    selectedPostId: Int?,
    navController: NavController,
    onClearFilter: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Statistics header with filter info
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Statistiky",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (selectedCategoryId != null || selectedPostId != null) {
                        Text(
                            "Zobrazeno: ${questions.size} z $allQuestionsCount otázek",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        // Show active filter
                        val filterPost = posts.find { it.id == selectedPostId }
                        val filterCategory = categories.find { it.id == selectedCategoryId }
                        val filterName = when {
                            filterPost != null -> "Článek: ${filterPost.title}"
                            filterCategory != null -> "Kategorie: ${filterCategory.name}"
                            selectedCategoryId != null -> "Kategorie: ID $selectedCategoryId" // fallback
                            else -> ""
                        }
                        
                        if (filterName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Filtr: $filterName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = onClearFilter,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zrušit", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        Text(
                            "Celkem otázek: $allQuestionsCount",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    val hasTextQuestions = questions.any { it.isTextQuestion }
                    val hasMultipleChoice = questions.any { !it.isTextQuestion }
                    val questionTypes = when {
                        hasTextQuestions && hasMultipleChoice -> "Výběr z možností + textové odpovědi"
                        hasTextQuestions -> "Textové odpovědi" 
                        hasMultipleChoice -> "Výběr z možností"
                        else -> "Různé typy"
                    }
                    Text(
                        "Typ otázek: $questionTypes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Practice all button
                    if (questions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                startPracticeWithQuestions(questions, navController)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Procvičovat všechny (${questions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Questions grouped by posts
        val questionsByPost = questions.groupBy { it.postId }
        items(questionsByPost.entries.toList()) { (postId, postQuestions) ->
            val post = posts.find { it.id == postId }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Post header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                post?.title ?: "Článek ID: $postId",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${postQuestions.size} otázek",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Practice button
                        TextButton(
                            onClick = { 
                                navController.navigate("questions/$postId")
                            }
                        ) {
                            Text("Vyzkoušet")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.PlayArrow, 
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Questions list
                    postQuestions.forEachIndexed { index, question ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (question.isTextQuestion) Icons.Filled.Edit else Icons.Filled.RadioButtonChecked,
                                        contentDescription = if (question.isTextQuestion) "Textová otázka" else "Výběrová otázka",
                                        tint = if (question.isTextQuestion) Color(0xFF2196F3) else Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Otázka ${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    question.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (!question.isTextQuestion && question.options.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Možnosti (${question.options.size}):",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    question.options.forEachIndexed { optionIndex, option ->
                                        Text(
                                            "${optionIndex + 1}. $option",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                            color = if (optionIndex == question.correctAnswer) {
                                                Color(0xFF4CAF50)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            }
                                        )
                                    }
                                }

                                val explanationText = question.explanation
                                if (!explanationText.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Lightbulb,
                                                contentDescription = "Vysvětlení",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                explanationText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
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

private fun getQuestionCountInCategory(
    categoryId: Int,
    allCategories: List<Category>,
    posts: List<Post>,
    allQuestions: List<com.example.tobisoappnative.model.Question>
): Int {
    // Get all subcategory IDs
    val relevantCategoryIds = getAllSubcategoryIds(categoryId, allCategories)
    
    // Get post IDs in these categories
    val postIds = posts
        .filter { it.categoryId in relevantCategoryIds }
        .map { it.id }
        .toSet()

    // Count questions in these posts
    return allQuestions.count { it.postId in postIds }
}

private fun getAllSubcategoryIds(categoryId: Int, categories: List<Category>): Set<Int> {
    val result = mutableSetOf(categoryId)
    val subcategories = categories.filter { it.parentId == categoryId }
    
    for (subcategory in subcategories) {
        result.addAll(getAllSubcategoryIds(subcategory.id, categories))
    }
    
    return result
}

private fun startPracticeWithQuestions(
    questions: List<com.example.tobisoappnative.model.Question>,
    navController: NavController
) {
    if (questions.isEmpty()) return
    
    val firstPostId = questions.first().postId
    
    // Pokud jsou všechny otázky ze stejného článku, přejdeme na klasický kvíz
    if (questions.all { it.postId == firstPostId }) {
        navController.navigate("questions/$firstPostId")
    } else {
        // Mixed quiz režim pro otázky z více článků
        val questionIds = questions.map { it.id }.joinToString(",")
        navController.navigate("mixedQuiz/$questionIds")
    }
}