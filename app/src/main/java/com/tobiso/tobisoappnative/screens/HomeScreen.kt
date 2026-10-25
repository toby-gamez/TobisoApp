package com.tobiso.tobisoappnative.screens

import androidx.compose.material3.Card
import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute

import com.tobiso.tobisoappnative.R
import com.tobiso.tobisoappnative.components.*
import com.tobiso.tobisoappnative.navigation.AiChatRoute
import com.tobiso.tobisoappnative.utils.NetworkUtils
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.tobiso.tobisoappnative.components.GradeBadge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.tobiso.tobisoappnative.components.ArticleOfDayCard
import com.tobiso.tobisoappnative.components.ContinueReadingCard
import com.tobiso.tobisoappnative.components.cozyEntrance
import com.tobiso.tobisoappnative.components.cozyPress
import com.tobiso.tobisoappnative.components.CozyTabs
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.tobiso.tobisoappnative.viewmodel.home.HomeViewModel
import com.tobiso.tobisoappnative.viewmodel.home.HomeIntent
import com.tobiso.tobisoappnative.viewmodel.home.HomeEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.IconPackManager
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.formatPointsBalance
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

import androidx.compose.ui.zIndex
import com.tobiso.tobisoappnative.utils.StreakUtils
import com.tobiso.tobisoappnative.utils.parseDateToMillis
import com.tobiso.tobisoappnative.utils.formatDateDisplay
import com.tobiso.tobisoappnative.utils.formatDateOnly
import com.tobiso.tobisoappnative.utils.SortMode
import com.tobiso.tobisoappnative.utils.loadSortMode
import com.tobiso.tobisoappnative.utils.saveSortMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListItem(
    post: com.tobiso.tobisoappnative.model.Post,
    categoryName: String,
    onClick: () -> Unit = {}
) {
    val candidates = listOfNotNull(post.activeLastEdit, post.activeLastFix, post.createdAt)
    val latestMillis = candidates.mapNotNull { parseDateToMillis(it) }.maxOrNull()
    val gradeName = post.activeVersion?.gradeName
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cozyEntrance(post.id)
            .cozyPress(0.97f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (gradeName != null) {
                        GradeBadge(gradeName = gradeName)
                    }
                    Text(
                        text = formatDateOnly(latestMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class Subject(
    val name: String,
    val text: String,
    val color: Color,
    val description: String = "",
)

@Composable
fun getColumnCount(maxWidth: androidx.compose.ui.unit.Dp): Int {
    return when {
        maxWidth >= 840.dp -> 3
        maxWidth >= 600.dp -> 2
        else -> 1
    }
}

data class SubjectConfig(
    val color: Color,
    val description: String,
)

val subjectConfigMap = mapOf(
    "Český jazyk" to SubjectConfig(Color(0xFFFF9800), "Gramatika, literatura a sloh"),
    "Hudební výchova" to SubjectConfig(Color(0xFF9C27B0), "Hudební teorie, autoři, žánry, písně, díla a dějiny"),
    "Matematika" to SubjectConfig(Color(0xFF1976D2), "Algebra a geometrie"),
    "Chemie" to SubjectConfig(Color(0xFFF44336), "Tělesa, látky, zákony, prvky, sloučeniny a názvosloví"),
    "Fyzika" to SubjectConfig(Color(0xFF607D8B), "Zákony fyziky, jevy, veličiny, stroje a světlo"),
    "Přírodopis" to SubjectConfig(Color(0xFF4CAF50), "Lidské tělo, minerály, horniny a geologie"),
    "Zeměpis" to SubjectConfig(Color(0xFF795548), "Vše o ČR a globálních tématech"),
)

val subjectColorsFallback = listOf(
    Color(0xFF2196F3), Color(0xFF8B4513), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFF1976D2), Color(0xFFF44336), Color(0xFF607D8B), Color(0xFF4CAF50),
    Color(0xFF795548), Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF673AB7)
)

fun getSubjectColorByName(subjectName: String): Color = subjectConfigMap[subjectName]?.color ?: Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val vm: HomeViewModel = hiltViewModel()
    var sortMode by remember { mutableStateOf(loadSortMode(context)) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val categories = state.categories
    val subjects = remember(categories) {
        val rootCategories = categories.filter { it.parentId == null && it.name != "More" }
        rootCategories.mapIndexed { index, cat ->
            val config = subjectConfigMap[cat.name]
            Subject(cat.name, cat.description ?: config?.description ?: "", config?.color ?: subjectColorsFallback[index % subjectColorsFallback.size], config?.description ?: "")
        }
    }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { vm.onIntent(HomeIntent.Load) }

    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()
    val authUser by com.tobiso.tobisoappnative.model.AuthSession.user.collectAsStateWithLifecycle()
    val bonusAiRemaining by com.tobiso.tobisoappnative.AiCreditManager.instance.bonusRemaining.collectAsStateWithLifecycle()
    val aiCredits = authUser?.credits ?: bonusAiRemaining.takeIf { it > 0 }
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline = !remember { NetworkUtils.isOnline(context) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is HomeEffect.NavigateToPost -> navController.navigate(PostDetailRoute(postId = effect.postId))
            }
        }
    }

    val currentStreak = remember { mutableStateOf(0) }
    val availableFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.availableFreezes.collectAsStateWithLifecycle()
    val usedFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.usedFreezes.collectAsStateWithLifecycle()
    LaunchedEffect(availableFreezes, usedFreezes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentStreak.value = StreakUtils.getCurrentStreak(context)
        }
    }

    val newestPosts by vm.newestPosts.collectAsStateWithLifecycle()
    LaunchedEffect(state.posts, state.categories, selectedSubjectId, sortMode) {
        if (sortMode == SortMode.NEWEST) vm.computeNewest(selectedSubjectId)
    }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val rootWithChildren = remember(categories) {
        categories.filter { it.parentId == null }.filter { root -> categories.any { it.parentId == root.id } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            BoxWithConstraints {
                val columns = if (maxWidth >= 600.dp) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 0.dp)) {
                            LargeScreenHeader(
                                title = "Učivo",
                                modifier = Modifier.layout { m, c ->
                                    val extra = 16.dp.roundToPx()
                                    val p = m.measure(c.copy(maxWidth = c.maxWidth + extra * 2))
                                    layout(c.maxWidth, p.height) { p.place(-extra, 0) }
                                },
                                actions = {
                                    aiCredits?.let {
                                        StatChip(
                                            icon = Icons.Rounded.AutoAwesome,
                                            text = it.toString(),
                                            onClick = { navController.navigate(ShopRoute) }
                                        )
                                    }
                                    StatChip(
                                        icon = Icons.Rounded.LocalFireDepartment,
                                        text = currentStreak.value.toString(),
                                        onClick = { navController.navigate(StreakRoute) }
                                    )
                                    StatChip(
                                        icon = Icons.Rounded.Stars,
                                        text = formatPointsBalance(totalPointsFloat),
                                        onClick = { navController.navigate(ShopRoute) }
                                    )
                                },
                                titleTrailing = { GradeChip(filled = false, modifier = Modifier.padding(bottom = 6.dp)) }
                            )
                            HomeSearchBar(
                                navController = navController,
                                posts = state.posts,
                                categories = categories,
                                isOffline = isOffline,
                                horizontalPadding = 0.dp,
                                onAiSend = { post, message ->
                                    navController.navigate(
                                        AiChatRoute(postId = post.id, postTitle = post.title, firstUserMessage = message)
                                    )
                                }
                            )
                            state.continueReading?.let {
                                ContinueReadingCard(
                                    item = it,
                                    onOpen = { id -> navController.navigate(PostDetailRoute(postId = id)) },
                                    modifier = Modifier
                                )
                            }
                            CozyTabs(
                                labels = listOf("Předměty", "Nejnovější"),
                                selectedIndex = if (sortMode == SortMode.SUBJECTS) 0 else 1,
                                onSelect = { i ->
                                    sortMode = if (i == 0) SortMode.SUBJECTS else SortMode.NEWEST
                                    saveSortMode(context, sortMode)
                                }
                            )
                            if (sortMode == SortMode.NEWEST) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedSubjectId == null,
                                        onClick = { selectedSubjectId = null },
                                        label = { Text("Vše") },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    rootWithChildren.forEach { subj ->
                                        FilterChip(
                                            selected = selectedSubjectId == subj.id,
                                            onClick = { selectedSubjectId = subj.id },
                                            label = { Text(subj.name) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (sortMode == SortMode.SUBJECTS) {
                        itemsIndexed(subjects) { index, subject ->
                            SubjectCard(
                                subject = subject,
                                onClick = { navController.navigate(CategoryListRoute(categoryName = subject.name)) },
                                modifier = Modifier.cozyEntrance(subject.name, index).cozyPress(0.95f)
                            )
                        }
                    } else {
                        items(newestPosts, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { post ->
                            Box {
                                PostListItem(
                                    post = post,
                                    categoryName = categoryMap[post.categoryId]?.name ?: "Nezařazeno",
                                    onClick = { navController.navigate(PostDetailRoute(postId = post.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectCard(
    subject: Subject,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (tintBg, tintFg) = subjectTint(subjectHue(subject.name))
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val subjectIcon = IconPackManager.getSubjectIcon(subject.name)
            val isEmoji = IconPackManager.isEmojiIcon(subject.name)
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                if (isEmoji && subjectIcon is String) {
                    Text(subjectIcon, fontSize = 22.sp, maxLines = 1)
                } else {
                    Icon(
                        imageVector = subjectIcon as ImageVector,
                        contentDescription = subject.name,
                        tint = tintFg,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = subject.name,
                fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = scheme.onSurface
            )
            if (subject.description.isNotEmpty()) {
                Text(
                    text = subject.description,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
