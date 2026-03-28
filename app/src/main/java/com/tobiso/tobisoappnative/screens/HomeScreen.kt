package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute

import com.tobiso.tobisoappnative.R
import com.tobiso.tobisoappnative.components.FloatingSearchBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.tobiso.tobisoappnative.viewmodel.home.HomeViewModel
import com.tobiso.tobisoappnative.viewmodel.home.HomeIntent
import com.tobiso.tobisoappnative.viewmodel.home.HomeEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
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
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.draw.shadow
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
    val candidates = listOfNotNull(post.lastEdit, post.lastFix, post.createdAt)
    val latestMillis = candidates.mapNotNull { parseDateToMillis(it) }.maxOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
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
                Text(
                    text = formatDateOnly(latestMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class Subject(
    val name: String,
    val icon: ImageVector,
    val colorType: SubjectColorType,
    val text: String,
)

enum class SubjectColorType {
    PRIMARY, SECONDARY, TERTIARY, ERROR, OUTLINE,
    PRIMARY_CONTAINER, SECONDARY_CONTAINER, TERTIARY_CONTAINER,
    SURFACE_VARIANT
}

@Composable
fun getColumnCount(): Int {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> 3  // Tablet landscape
        configuration.screenWidthDp >= 600 -> 2  // Tablet portrait / velký mobil
        else -> 1  // Mobil
    }
}

// Funkce pro získání barvy podle názvu předmětu
@Composable 
fun getSubjectColorByName(subjectName: String): Color {
    val colorType = when (subjectName) {
        "Mluvnice" -> SubjectColorType.PRIMARY
        "Literatura" -> SubjectColorType.SECONDARY  
        "Sloh" -> SubjectColorType.TERTIARY
        "Hudební výchova" -> SubjectColorType.PRIMARY_CONTAINER
        "Matematika" -> SubjectColorType.SECONDARY_CONTAINER
        "Chemie" -> SubjectColorType.ERROR
        "Fyzika" -> SubjectColorType.TERTIARY_CONTAINER
        "Přírodopis" -> SubjectColorType.OUTLINE
        "Zeměpis" -> SubjectColorType.SURFACE_VARIANT
        else -> SubjectColorType.PRIMARY // Výchozí barva
    }
    return getSubjectColor(colorType)
}

@Composable
fun getSubjectColor(colorType: SubjectColorType): Color {
    val isDarkTheme = isSystemInDarkTheme()

    return if (isDarkTheme) {
        // Šedější barvy pro tmavý režim
        when (colorType) {
            SubjectColorType.PRIMARY -> Color(0xFF9E9E9E)
            SubjectColorType.SECONDARY -> Color(0xFF757575)
            SubjectColorType.TERTIARY -> Color(0xFF616161)
            SubjectColorType.ERROR -> Color(0xFF8C8C8C)
            SubjectColorType.OUTLINE -> Color(0xFF666666)
            SubjectColorType.PRIMARY_CONTAINER -> Color(0xFF7C7C7C)
            SubjectColorType.SECONDARY_CONTAINER -> Color(0xFF696969)
            SubjectColorType.TERTIARY_CONTAINER -> Color(0xFF545454)
            SubjectColorType.SURFACE_VARIANT -> Color(0xFF424242)
        }
    } else {
        // Barvy podle názvu předmětů pro světlý režim
        when (colorType) {
            SubjectColorType.PRIMARY -> Color(0xFF2196F3)        // Mluvnice - Modrá
            SubjectColorType.SECONDARY -> Color(0xFF8B4513)      // Literatura - Hnědá
            SubjectColorType.TERTIARY -> Color(0xFFFF9800)       // Sloh - Oranžová
            SubjectColorType.PRIMARY_CONTAINER -> Color(0xFF9C27B0)     // Hudební výchova - Fialová
            SubjectColorType.SECONDARY_CONTAINER -> Color(0xFF1976D2)   // Matematika - Tmavě modrá
            SubjectColorType.ERROR -> Color(0xFFF44336)          // Chemie - Červená
            SubjectColorType.TERTIARY_CONTAINER -> Color(0xFF607D8B)    // Fyzika - Modro-šedá
            SubjectColorType.OUTLINE -> Color(0xFF4CAF50)        // Přírodopis - Zelená
            SubjectColorType.SURFACE_VARIANT -> Color(0xFF795548)       // Zeměpis - Hnědá
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light
    val subjects = listOf(
        Subject("Mluvnice", Icons.Default.Spellcheck, SubjectColorType.PRIMARY, "Gramatika a pravopis českého jazyka"),
        Subject("Literatura", Icons.Default.MenuBook, SubjectColorType.SECONDARY, "Česká a světová literatura"),
        Subject("Sloh", Icons.Default.Description, SubjectColorType.TERTIARY, "Tvorba textů a slohové útvary"),
        Subject("Hudební výchova", Icons.Default.LibraryMusic, SubjectColorType.PRIMARY_CONTAINER, "Hudební teorie, autoři, žánry, písně, díla a dějiny"),
        Subject("Matematika", Icons.Default.Calculate, SubjectColorType.SECONDARY_CONTAINER, "Algebra a geometrie"),
        Subject("Chemie", Icons.Default.Science, SubjectColorType.ERROR, "Tělesa, látky, zákony, prvky a sloučeniny"),
        Subject("Fyzika", Icons.Default.PrecisionManufacturing, SubjectColorType.TERTIARY_CONTAINER, "Zákony fyziky, elektřina, radioaktivita, veličiny, stroje a světlo"),
        Subject("Přírodopis", Icons.Default.Eco, SubjectColorType.OUTLINE, "Lidské tělo, minerály a horniny"),
        Subject("Zeměpis", Icons.Default.Public, SubjectColorType.SURFACE_VARIANT, "Vše o povrchu, obyvatelstvu, hospodářství a ochraně přírody ČR"),
    )
    val context = LocalContext.current
    val columnCount = getColumnCount()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()
    val vm: HomeViewModel = hiltViewModel()
    var sortMode by remember { mutableStateOf(loadSortMode(context)) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val state by vm.uiState.collectAsState()
    val posts = state.posts
    val categories = state.categories
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(Unit) { 
        vm.onIntent(HomeIntent.Load)
    }
    
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val offlineDownloading = state.offlineDownloading
    val offlineProgress = state.offlineDownloadProgress
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(

                title = {
                    Text("Učivo",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                    )
                },
                actions = {
                    // Dropdown pro výběr zobrazení (předměty / nejnovější)
                    IconButton(onClick = { dropdownExpanded = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Řazení")
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Podle předmětů") },
                            onClick = {
                                sortMode = SortMode.SUBJECTS
                                saveSortMode(context, SortMode.SUBJECTS)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nejnovější") },
                            onClick = {
                                sortMode = SortMode.NEWEST
                                saveSortMode(context, SortMode.NEWEST)
                                dropdownExpanded = false
                            }
                        )
                    }
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Zobrazení bodů s novým designem
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
                    // Offline download indicator / tlačítko
                    if (offlineDownloading) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            CircularProgressIndicator(
                                progress = offlineProgress.coerceIn(0f, 1f),
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Streak button s počtem dní (s freeze podporou)
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    // Sledování změn v freeze
                    val availableFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.availableFreezes.collectAsState()
                    val usedFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.usedFreezes.collectAsState()
                    
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
                },
                scrollBehavior = scrollBehavior
            )
            
            // Obsah podle režimu (předměty / nejnovější)
            when (sortMode) {
                SortMode.SUBJECTS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(subjects) { subject ->
                            SubjectCard(
                                subject = subject,
                                onClick = { navController.navigate(CategoryListRoute(categoryName = subject.name)) },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                SortMode.NEWEST -> {
                    // Tlačítka pro výběr předmětu (root kategorie se skutečnými potomky)
                    val rootSubjects = remember(categories) { categories.filter { it.parentId == null } }
                    val rootWithChildren = remember(categories) { rootSubjects.filter { root -> categories.any { it.parentId == root.id } } }

                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    var pendingScrollToTop by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = {
                                // If we're switching from a subject to "Vše", always set a pending scroll
                                // so newly inserted items above the current first become visible.
                                // If we're already on "Vše", only scroll now if not at the top.
                                if (selectedSubjectId != null) {
                                    selectedSubjectId = null
                                    pendingScrollToTop = true
                                } else {
                                    val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                                    if (!atTop) coroutineScope.launch { listState.animateScrollToItem(0) }
                                }
                            },
                            label = { Text("Vše") }
                        )

                        rootWithChildren.forEach { subj ->
                            FilterChip(
                                selected = selectedSubjectId == subj.id,
                                onClick = { selectedSubjectId = subj.id },
                                label = { Text(subj.name) }
                            )
                        }
                    }

                    val categoryMap = remember(categories) { categories.associateBy { it.id } }
                    val newestPosts by vm.newestPosts.collectAsState()

                    LaunchedEffect(state.posts, state.categories, selectedSubjectId) {
                        vm.computeNewest(selectedSubjectId)
                    }

                    LaunchedEffect(newestPosts) {
                        if (pendingScrollToTop) {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                            pendingScrollToTop = false
                        }
                    }

                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp)) {
                        items(items = newestPosts, key = { it.id }) { post ->
                            PostListItem(
                                post = post,
                                categoryName = categoryMap[post.categoryId]?.name ?: "Nezařazeno",
                                onClick = { navController.navigate(PostDetailRoute(postId = post.id)) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        
        // Overlay pro celkové body
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }

        // Snackbar host (moved higher)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
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
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subjectIcon = IconPackManager.getSubjectIcon(subject.name)
            val isEmoji = IconPackManager.isEmojiIcon(subject.name)
            
            // Ikona nebo emoji
            if (isEmoji && subjectIcon is String) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subjectIcon,
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (subjectIcon is ImageVector) subjectIcon else subject.icon,
                        contentDescription = subject.name,
                        tint = getSubjectColor(subject.colorType),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subject.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
