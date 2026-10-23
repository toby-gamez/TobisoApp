package com.tobiso.tobisoappnative.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.tobiso.tobisoappnative.components.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.IconPackManager
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.QuestionProgressManager
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.formatPointsBalance
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseDragDropRoute
import com.tobiso.tobisoappnative.navigation.ExerciseMatchingRoute
import com.tobiso.tobisoappnative.navigation.ExerciseTimelineRoute
import com.tobiso.tobisoappnative.navigation.MixedQuizRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute
import com.tobiso.tobisoappnative.utils.StreakUtils
import com.tobiso.tobisoappnative.viewmodel.allquestions.AllQuestionsViewModel
import com.tobiso.tobisoappnative.viewmodel.allquestions.WeakCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllQuestionsScreen(navController: NavController) {
    val vm: AllQuestionsViewModel = hiltViewModel()
    val allQuestions by vm.allQuestions.collectAsStateWithLifecycle()
    val allQuestionsError by vm.allQuestionsError.collectAsStateWithLifecycle()
    val allQuestionsLoading by vm.allQuestionsLoading.collectAsStateWithLifecycle()
    val isOffline by vm.isOffline.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val weakCategories by vm.weakCategories.collectAsStateWithLifecycle()
    val categoryQuestionIdsMap by vm.categoryQuestionIdsMap.collectAsStateWithLifecycle()
    val categoryExercisesMap by vm.categoryExercisesMap.collectAsStateWithLifecycle()
    val allExercises by vm.allExercises.collectAsStateWithLifecycle()
    val questionsPosts by vm.questionsPosts.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
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
        ) {
            val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()
            val header: @Composable () -> Unit = {
                LargeScreenHeader(
                    title = "Procvičování",
                    actions = {
                        StatChip(
                            icon = Icons.Rounded.Stars,
                            text = formatPointsBalance(totalPointsFloat),
                            onClick = { navController.navigate(ShopRoute) }
                        )
                    }
                )
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
                    allQuestionsError != null -> Column { header(); ErrorState(
                        message = allQuestionsError ?: "Neznámá chyba",
                        onRetry = { vm.loadAllQuestions() }
                    ) }

                    allQuestionsLoading -> Column { header(); LoadingState() }

                    allQuestions.isEmpty() -> Column { header(); EmptyState() }

                    else -> PracticeHubContent(
                        vm = vm,
                        allQuestionsCount = allQuestions.size,
                        categories = categories,
                        weakCategories = weakCategories,
                        categoryQuestionIdsMap = categoryQuestionIdsMap,
                        categoryExercisesMap = categoryExercisesMap,
                        allExercises = allExercises,
                        posts = questionsPosts,
                        navController = navController,
                        header = header
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
    allExercises: List<InteractiveExerciseResponse>,
    posts: List<com.tobiso.tobisoappnative.model.Post>,
    navController: NavController,
    header: @Composable () -> Unit = {}
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

    val visibleExercises = remember(allExercises) { allExercises.filter { it.type != "circuit" } }
    var exercisesExpanded by remember { mutableStateOf(false) }
    val displayedExercises = if (exercisesExpanded) visibleExercises else visibleExercises.take(3)

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
        item { header() }
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
                },
                onRandomExercise = if (visibleExercises.isNotEmpty()) {
                    { vm.getRandomExercise()?.let { navigateToExercise(navController, it) } }
                } else null
            )
        }

        if (weakCategories.isNotEmpty()) {
            item {
                WeakSpotsSection(
                    count = weakCategories.size,
                    onClick = {
                        val ids = weakCategories.flatMap { categoryQuestionIdsMap[it.categoryId] ?: emptyList() }.distinct().shuffled().take(10)
                        if (ids.isNotEmpty()) navController.navigate(MixedQuizRoute(ids.joinToString(",")))
                    }
                )
            }
        }

        item { SectionLabel("Procvičovat dle tématu") }
        item {
            GroupedList(Modifier.padding(horizontal = 16.dp)) {
                rootCategories.forEachIndexed { index, category ->
                    val questionIds = categoryQuestionIdsMap[category.id] ?: emptyList()
                    val progress = if (questionIds.isNotEmpty())
                        QuestionProgressManager.instance.getProgressForQuestions(questionIds.toSet())
                    else -1f
                    if (index > 0) GroupedListDivider()
                    TopicRow(
                        name = category.name,
                        questionCount = questionIds.size,
                        progress = if (progress < 0f) null else progress,
                        onClick = { sheetCategoryId = category.id }
                    )
                }
            }
        }

        if (allExercises.isNotEmpty()) {
            item { SectionLabel("Interaktivní cvičení") }
            val types = listOf(
                Triple("drag-drop", "Přetahování", Icons.Rounded.DragIndicator),
                Triple("matching", "Přiřazování", Icons.Rounded.Link),
                Triple("timeline", "Časová osa", Icons.Rounded.Timeline),
                Triple("circuit", "Obvody", Icons.Rounded.ElectricalServices)
            ).filter { (type, _, _) -> allExercises.any { it.type == type } }
            items(types.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (type, label, icon) ->
                        val ofType = allExercises.filter { it.type == type }
                        Card(
                            onClick = { navigateToExercise(navController, ofType.random()) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "${ofType.size} ${com.tobiso.tobisoappnative.utils.pluralCs(ofType.size, "cvičení", "cvičení", "cvičení")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
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
                    imageVector = subjectIcon as? ImageVector ?: Icons.AutoMirrored.Rounded.MenuBook,
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
                                progress >= 0.8f -> com.tobiso.tobisoappnative.ui.theme.StatusColors.success
                                progress >= 0.5f -> com.tobiso.tobisoappnative.ui.theme.StatusColors.warning
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
                    hasChildren || expandable -> if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore
                    else -> Icons.Rounded.ChevronRight
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
    onAll: () -> Unit,
    onRandomExercise: (() -> Unit)?
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeroCard(containerColor = scheme.primary) {
            Text("RYCHLÝ START", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary.copy(alpha = 0.8f), letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "10 otázek napříč tvým ročníkem",
                fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                color = scheme.onPrimary
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onQuickDrill,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.onPrimary, contentColor = scheme.primary),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Začít", fontWeight = FontWeight.Bold)
                }
                Text("~5 min · až 50 bodů", fontSize = 13.sp, color = scheme.onPrimary.copy(alpha = 0.85f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = onDailyChallenge, label = { Text("Denní výzva · 20") }, shape = RoundedCornerShape(10.dp))
            AssistChip(onClick = onAll, label = { Text("Vše · $totalCount") }, shape = RoundedCornerShape(10.dp))
            if (onRandomExercise != null) {
                AssistChip(onClick = onRandomExercise, label = { Text("Náhodné cvičení") }, shape = RoundedCornerShape(10.dp))
            }
        }
    }
}

@Composable
private fun WeakSpotsSection(count: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        HeroCard(containerColor = scheme.secondaryContainer, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconCircle(
                    icon = Icons.Rounded.TrackChanges,
                    size = 48.dp,
                    background = scheme.secondary.copy(alpha = 0.18f),
                    tint = scheme.secondary
                )
                Column(Modifier.weight(1f)) {
                    Text("Slabá místa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onSecondaryContainer)
                    Text(
                        "$count ${com.tobiso.tobisoappnative.utils.pluralCs(count, "téma", "témata", "témat")}, ve kterých chybuješ",
                        fontSize = 13.sp,
                        color = scheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun TopicRow(name: String, questionCount: Int, progress: Float?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subjectIcon = IconPackManager.getSubjectIcon(name)
    val isEmoji = IconPackManager.isEmojiIcon(name)
    val (tintBg, tintFg) = subjectTint(subjectHue(name))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = questionCount > 0, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tintBg), contentAlignment = Alignment.Center) {
            if (isEmoji && subjectIcon is String) {
                Text(subjectIcon, fontSize = 20.sp)
            } else {
                Icon(subjectIcon as? ImageVector ?: Icons.AutoMirrored.Rounded.MenuBook, null, tint = tintFg, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (questionCount > 0) "$questionCount otázek" else "Žádné otázky",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            if (progress != null && questionCount > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = scheme.secondary,
                    trackColor = scheme.surfaceContainerHigh,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: InteractiveExerciseResponse, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (icon, typeLabel) = when (exercise.type) {
        "timeline" -> Icons.Rounded.Timeline to "Časová osa"
        "drag-drop" -> Icons.Rounded.DragIndicator to "Přetahování"
        "matching" -> Icons.Rounded.Link to "Přiřazování"
        "circuit" -> Icons.Rounded.ElectricalServices to "Obvod"
        else -> Icons.Rounded.Extension to "Cvičení"
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconCircle(icon = icon, size = 40.dp, background = scheme.secondaryContainer, tint = scheme.onSecondaryContainer)
            Text(typeLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                exercise.title ?: "Cvičení",
                fontSize = 12.5.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
private fun ExerciseRow(
    exercise: InteractiveExerciseResponse,
    contextLabel: String? = null,
    onClick: () -> Unit
) {
    val (icon, typeLabel) = when (exercise.type) {
        "timeline" -> Icons.Rounded.Timeline to "Časová osa"
        "drag-drop" -> Icons.Rounded.DragIndicator to "Přetahování"
        "matching" -> Icons.AutoMirrored.Rounded.CompareArrows to "Párování"
        "circuit" -> Icons.Rounded.AccountTree to "Obvod"
        else -> Icons.Rounded.Extension to "Cvičení"
    }
    val subtitle = if (contextLabel != null) "$typeLabel · $contextLabel" else typeLabel
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
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
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
                Icons.Rounded.Error,
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
                Icons.Rounded.QuestionMark,
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
