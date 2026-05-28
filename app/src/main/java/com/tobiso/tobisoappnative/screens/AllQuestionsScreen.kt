package com.tobiso.tobisoappnative.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.IconPackManager
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.QuestionProgressManager
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseDragDropRoute
import com.tobiso.tobisoappnative.navigation.ExerciseMatchingRoute
import com.tobiso.tobisoappnative.navigation.ExerciseTimelineRoute
import com.tobiso.tobisoappnative.navigation.MixedQuizRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute
import com.tobiso.tobisoappnative.utils.StreakUtils
import com.tobiso.tobisoappnative.viewmodel.allquestions.AllQuestionsViewModel
import com.tobiso.tobisoappnative.viewmodel.allquestions.WeakCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllQuestionsScreen(navController: NavController) {
    val vm: AllQuestionsViewModel = hiltViewModel()
    val allQuestions by vm.allQuestions.collectAsState()
    val allQuestionsError by vm.allQuestionsError.collectAsState()
    val allQuestionsLoading by vm.allQuestionsLoading.collectAsState()
    val isOffline by vm.isOffline.collectAsState()
    val categories by vm.categories.collectAsState()
    val weakCategories by vm.weakCategories.collectAsState()
    val categoryQuestionIdsMap by vm.categoryQuestionIdsMap.collectAsState()
    val categoryExercisesMap by vm.categoryExercisesMap.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var showTotalOverlay by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(isOffline) {
        vm.loadCategories()
        vm.loadAllQuestions()
    }

    DisposableEffect(Unit) {
        onDispose { vm.clearAllQuestions() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                LargeTopAppBar(
                    title = {
                        val fraction = scrollBehavior.state.collapsedFraction
                        Text(
                            text = "Procvičování",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = (36 - 16 * fraction).sp
                            ),
                            maxLines = 1
                        )
                    },
                    actions = {
                        MultiplierIndicator()

                        val totalPoints by PointsManager.instance.totalPoints.collectAsState()
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

                        val currentStreak = remember { mutableStateOf(0) }
                        val availableFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.availableFreezes.collectAsState()
                        val usedFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.usedFreezes.collectAsState()
                        LaunchedEffect(availableFreezes, usedFreezes) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                currentStreak.value = StreakUtils.getCurrentStreak(context)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { navController.navigate(StreakRoute) }
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

                        val offlineDownloading by vm.offlineDownloading.collectAsState()
                        val offlineProgress by vm.offlineDownloadProgress.collectAsState()
                        if (offlineDownloading) {
                            Box(modifier = Modifier.padding(end = 8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = offlineProgress.coerceIn(0f, 1f),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )

                if (showTotalOverlay) {
                    val totalPoints by PointsManager.instance.totalPoints.collectAsState()
                    FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                    LaunchedEffect(showTotalOverlay) {
                        kotlinx.coroutines.delay(2200)
                        showTotalOverlay = false
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (!isOffline) {
                        isRefreshing = true
                        coroutineScope.launch {
                            vm.loadAllQuestions()
                            isRefreshing = false
                        }
                    }
                }
            ) {
                when {
                    allQuestionsError != null -> ErrorState(
                        message = allQuestionsError ?: "Neznámá chyba",
                        onRetry = { vm.loadAllQuestions() }
                    )

                    allQuestionsLoading -> LoadingState()

                    allQuestions.isEmpty() -> EmptyState()

                    else -> PracticeHubContent(
                        vm = vm,
                        allQuestionsCount = allQuestions.size,
                        categories = categories,
                        weakCategories = weakCategories,
                        categoryQuestionIdsMap = categoryQuestionIdsMap,
                        categoryExercisesMap = categoryExercisesMap,
                        navController = navController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeHubContent(
    vm: AllQuestionsViewModel,
    allQuestionsCount: Int,
    categories: List<com.tobiso.tobisoappnative.model.Category>,
    weakCategories: List<WeakCategory>,
    categoryQuestionIdsMap: Map<Int, List<Int>>,
    categoryExercisesMap: Map<Int, List<InteractiveExerciseResponse>>,
    navController: NavController
) {
    val rootCategories = remember(categories, categoryQuestionIdsMap) {
        categories.filter { cat ->
            cat.parentId == null &&
                    cat.name != "More" &&
                    (categoryQuestionIdsMap[cat.id]?.isNotEmpty() == true)
        }
    }

    var sheetCategoryId by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (sheetCategoryId != null) {
        val cat = categories.find { it.id == sheetCategoryId }
        val allIds = categoryQuestionIdsMap[sheetCategoryId!!] ?: emptyList()
        if (cat != null && allIds.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { sheetCategoryId = null },
                sheetState = sheetState
            ) {
                TopicSheet(
                    category = cat,
                    allQuestionIds = allIds,
                    subcategories = categories.filter { it.parentId == cat.id },
                    categoryQuestionIdsMap = categoryQuestionIdsMap,
                    categoryExercisesMap = categoryExercisesMap,
                    allCategories = categories,
                    onLaunch = { ids ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            sheetCategoryId = null
                            navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                        }
                    },
                    onNavigateExercise = { exercise ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            sheetCategoryId = null
                            navigateToExercise(navController, exercise)
                        }
                    }
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            QuickActionsSection(
                totalCount = allQuestionsCount,
                onQuickDrill = {
                    val ids = vm.getRandomQuestionIds(10)
                    if (ids.isNotEmpty()) navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                },
                onDailyChallenge = {
                    val ids = vm.getDailyQuestionIds(20)
                    if (ids.isNotEmpty()) navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                },
                onAll = {
                    val ids = vm.getRandomQuestionIds(allQuestionsCount)
                    if (ids.isNotEmpty()) navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                }
            )
        }

        if (weakCategories.isNotEmpty()) {
            item {
                WeakSpotsSection(
                    weakCategories = weakCategories,
                    onCategoryClick = { catId ->
                        val ids = categoryQuestionIdsMap[catId] ?: emptyList()
                        if (ids.isNotEmpty()) navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                    }
                )
            }
        }

        item {
            Text(
                text = "Procvičovat dle tématu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        items(rootCategories.chunked(2)) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { category ->
                    val questionIds = categoryQuestionIdsMap[category.id] ?: emptyList()
                    val progress = if (questionIds.isNotEmpty())
                        QuestionProgressManager.instance.getProgressForQuestions(questionIds.toSet())
                    else -1f
                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        name = category.name,
                        questionCount = questionIds.size,
                        progress = if (progress < 0f) null else progress,
                        onClick = { sheetCategoryId = category.id }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TopicSheet(
    category: com.tobiso.tobisoappnative.model.Category,
    allQuestionIds: List<Int>,
    subcategories: List<com.tobiso.tobisoappnative.model.Category>,
    categoryQuestionIdsMap: Map<Int, List<Int>>,
    categoryExercisesMap: Map<Int, List<InteractiveExerciseResponse>>,
    allCategories: List<com.tobiso.tobisoappnative.model.Category>,
    onLaunch: (List<Int>) -> Unit,
    onNavigateExercise: (InteractiveExerciseResponse) -> Unit
) {
    val subjectIcon = IconPackManager.getSubjectIcon(category.name)
    val isEmoji = IconPackManager.isEmojiIcon(category.name)
    val total = allQuestionIds.size
    val counts = listOf(10, 25, 50).filter { it < total }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEmoji && subjectIcon is String) {
                Text(text = subjectIcon, fontSize = 28.sp)
            } else {
                Icon(
                    imageVector = subjectIcon as? ImageVector ?: Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$total otázek celkem",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick count chips
        Text(
            text = "Rychlý dril",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.forEach { n ->
                ElevatedButton(
                    onClick = { onLaunch(allQuestionIds.shuffled().take(n)) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("$n", style = MaterialTheme.typography.labelLarge)
                }
            }
            Button(
                onClick = { onLaunch(allQuestionIds.shuffled()) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Vše ($total)", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Subcategories
        val subcatsWithQuestions = subcategories
            .mapNotNull { sub -> (categoryQuestionIdsMap[sub.id]?.takeIf { it.isNotEmpty() })?.let { sub to it } }

        if (subcatsWithQuestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Podkategorie",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            subcatsWithQuestions.forEach { (sub, ids) ->
                val progress = QuestionProgressManager.instance.getProgressForQuestions(ids.toSet())
                SubcategoryRow(
                    category = sub,
                    ids = ids,
                    progress = if (progress < 0f) null else progress,
                    allCategories = allCategories,
                    categoryQuestionIdsMap = categoryQuestionIdsMap,
                    onLaunch = onLaunch
                )
            }
        }

        // Exercises for this category (and its direct subcategories)
        val catIds = (listOf(category.id) + subcategories.map { it.id }).toSet()
        val exercises = catIds
            .flatMap { categoryExercisesMap[it] ?: emptyList() }
            .distinctBy { it.id }

        if (exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Interaktivní cvičení",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            exercises.forEach { exercise ->
                ExerciseRow(exercise = exercise, onClick = { onNavigateExercise(exercise) })
            }
        }
    }
}

@Composable
private fun SubcategoryRow(
    category: com.tobiso.tobisoappnative.model.Category,
    ids: List<Int>,
    progress: Float?,
    allCategories: List<com.tobiso.tobisoappnative.model.Category>,
    categoryQuestionIdsMap: Map<Int, List<Int>>,
    onLaunch: (List<Int>) -> Unit,
    indent: Int = 0
) {
    val children = allCategories
        .filter { it.parentId == category.id }
        .mapNotNull { child -> (categoryQuestionIdsMap[child.id]?.takeIf { it.isNotEmpty() })?.let { child to it } }

    val hasChildren = children.isNotEmpty()
    val expandable = ids.size > 15
    var expanded by remember(category.id) { mutableStateOf(false) }

    val startPad = (indent * 16 + 4).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (expandable || hasChildren) expanded = !expanded
                else onLaunch(ids.shuffled())
            }
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = startPad, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (indent == 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (progress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = when {
                                progress >= 0.8f -> Color(0xFF4CAF50)
                                progress >= 0.5f -> Color(0xFFFFC107)
                                else -> MaterialTheme.colorScheme.error
                            },
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${ids.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = when {
                    hasChildren || expandable -> if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                    else -> Icons.Default.ChevronRight
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (expanded) {
            if (hasChildren) {
                // Show sub-subcategories as another list
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    children.forEach { (child, childIds) ->
                        val childProgress = QuestionProgressManager.instance
                            .getProgressForQuestions(childIds.toSet())
                        SubcategoryRow(
                            category = child,
                            ids = childIds,
                            progress = if (childProgress < 0f) null else childProgress,
                            allCategories = allCategories,
                            categoryQuestionIdsMap = categoryQuestionIdsMap,
                            onLaunch = onLaunch,
                            indent = indent + 1
                        )
                    }
                }
            } else {
                // Leaf node — show count chips
                val counts = listOf(10, 15).filter { it < ids.size }
                Row(
                    modifier = Modifier.padding(start = startPad, end = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    counts.forEach { n ->
                        ElevatedButton(
                            onClick = { onLaunch(ids.shuffled().take(n)) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("$n", style = MaterialTheme.typography.labelMedium) }
                    }
                    Button(
                        onClick = { onLaunch(ids.shuffled()) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("Vše (${ids.size})", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    totalCount: Int,
    onQuickDrill: () -> Unit,
    onDailyChallenge: () -> Unit,
    onAll: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        Text(
            text = "Rychlý start",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                title = "Rychlý dril",
                subtitle = "10 náhodných otázek",
                icon = Icons.Default.FlashOn,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onQuickDrill
            )
            QuickActionCard(
                title = "Denní výzva",
                subtitle = "20 otázek dne",
                icon = Icons.Default.Today,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onDailyChallenge
            )
            QuickActionCard(
                title = "Vše",
                subtitle = "$totalCount otázek",
                icon = Icons.Default.LibraryBooks,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onAll
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeakSpotsSection(
    weakCategories: List<WeakCategory>,
    onCategoryClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Slabá místa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weakCategories.forEach { weak ->
                WeakCategoryChip(weak = weak, onClick = { onCategoryClick(weak.categoryId) })
            }
        }
    }
}

@Composable
private fun WeakCategoryChip(weak: WeakCategory, onClick: () -> Unit) {
    val pct = (weak.percentage * 100).toInt()
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = weak.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$pct% správně",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CategoryCard(
    modifier: Modifier = Modifier,
    name: String,
    questionCount: Int,
    progress: Float?,
    onClick: () -> Unit
) {
    val subjectIcon = IconPackManager.getSubjectIcon(name)
    val isEmoji = IconPackManager.isEmojiIcon(name)

    Card(
        modifier = modifier.clickable(enabled = questionCount > 0, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isEmoji && subjectIcon is String) {
                Text(
                    text = subjectIcon,
                    fontSize = 24.sp,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = subjectIcon as? ImageVector ?: Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (questionCount > 0) "$questionCount otázek" else "Žádné otázky",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (progress != null && questionCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        progress >= 0.8f -> Color(0xFF4CAF50)
                        progress >= 0.5f -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun navigateToExercise(navController: NavController, exercise: InteractiveExerciseResponse) {
    when (exercise.type) {
        "timeline" -> navController.navigate(ExerciseTimelineRoute(exercise.id))
        "drag-drop" -> navController.navigate(ExerciseDragDropRoute(exercise.id))
        "matching" -> navController.navigate(ExerciseMatchingRoute(exercise.id))
        "circuit" -> navController.navigate(ExerciseCircuitRoute(exercise.id))
    }
}

@Composable
private fun ExerciseRow(exercise: InteractiveExerciseResponse, onClick: () -> Unit) {
    val (icon, label) = when (exercise.type) {
        "timeline" -> Icons.Default.Timeline to "Časová osa"
        "drag-drop" -> Icons.Default.DragIndicator to "Přetahování"
        "matching" -> Icons.Default.CompareArrows to "Párování"
        "circuit" -> Icons.Default.AccountTree to "Obvod"
        else -> Icons.Default.Extension to "Cvičení"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.title ?: "Cvičení",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Zkusit znovu") }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Načítání otázek...")
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
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
        }
    }
}
