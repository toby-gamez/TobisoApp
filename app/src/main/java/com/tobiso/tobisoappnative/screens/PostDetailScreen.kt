package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.LinearProgressIndicator

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.TextButton

import com.tobiso.tobisoappnative.components.Button
import timber.log.Timber

import com.tobiso.tobisoappnative.navigation.AiChatRoute
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseDragDropRoute
import com.tobiso.tobisoappnative.navigation.ExerciseMatchingRoute
import com.tobiso.tobisoappnative.navigation.ExerciseTimelineRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.navigation.QuestionsRoute
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.postdetail.PostDetailViewModel
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.TimeZone
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.ClickableText
// selection removed from this screen — selection moved to PlainTextScreen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.text.AnnotatedString
import com.tobiso.tobisoappnative.model.Addendum
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// combinedClickable not used here
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.TtsPlayer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.tobiso.tobisoappnative.utils.TextUtils
import com.tobiso.tobisoappnative.utils.hasAiConsent
import com.tobiso.tobisoappnative.utils.saveAiConsent
import com.tobiso.tobisoappnative.components.AiConsentDialog
import java.io.File
import java.io.FileOutputStream
import android.os.Build
import androidx.compose.runtime.saveable.rememberSaveable
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Environment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Clear
import kotlin.getOrElse
import coil.compose.AsyncImage
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.tobiso.tobisoappnative.components.InlineFraction
// removed all halilibo.richtext imports
import com.tobiso.tobisoappnative.components.AddendumDialog
import com.tobiso.tobisoappnative.components.AiInputBar
import com.tobiso.tobisoappnative.components.SentenceSelectPanel
import com.tobiso.tobisoappnative.viewmodel.ai.AiToolsViewModel
import kotlin.math.max
import com.tobiso.tobisoappnative.components.parseContentToElements
import com.tobiso.tobisoappnative.components.ContentElement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.EditCalendar
import com.tobiso.tobisoappnative.components.GradeBadge
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tobiso.tobisoappnative.components.QuizCtaCard
import com.tobiso.tobisoappnative.components.GradePickerSheet
import com.tobiso.tobisoappnative.components.ContentRenderer
import com.tobiso.tobisoappnative.components.ElementRenderer
import com.tobiso.tobisoappnative.components.PostActionsRow
import com.tobiso.tobisoappnative.components.ExerciseButtonsRow
import com.tobiso.tobisoappnative.components.RelatedPostsList
import com.tobiso.tobisoappnative.components.DifficultyRating
import com.tobiso.tobisoappnative.components.TableOfContentsDialog
import kotlinx.serialization.json.JsonNull.content
import androidx.compose.foundation.text.selection.SelectionContainer

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    navController: NavController,
    ttsViewModel: TtsViewModel,
) {
    val vm: PostDetailViewModel = hiltViewModel()
    val postDetail by vm.postDetail.collectAsStateWithLifecycle()
    val postDetailError by vm.postDetailError.collectAsStateWithLifecycle()
    val favoritePosts by vm.favoritePosts.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val isOffline by vm.isOffline.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val exercisesLoading by vm.exercisesLoading.collectAsStateWithLifecycle()
    val exercisesError by vm.exercisesError.collectAsStateWithLifecycle()
    val questions by vm.questions.collectAsStateWithLifecycle()
    val grades by vm.grades.collectAsStateWithLifecycle()
    var showGradePicker by remember { mutableStateOf(false) }
    val availableGradeIds by vm.availableGradeIds.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadGrades() }
    LaunchedEffect(postId) { vm.loadAvailableGrades(postId) }
    if (showGradePicker) {
        GradePickerSheet(
            grades = grades,
            activeGradeId = postDetail?.activeVersion?.gradeId,
            availableGradeIds = availableGradeIds,
            onSelect = { vm.selectGrade(postId, it.id) },
            onDismiss = { showGradePicker = false }
        )
    }
    val difficulty by vm.difficulty.collectAsStateWithLifecycle()
    LaunchedEffect(postId) { vm.loadDifficulty(postId) }
    val relatedPosts by vm.relatedPosts.collectAsStateWithLifecycle()
    val relatedPostsError by vm.relatedPostsError.collectAsStateWithLifecycle()
    val relatedPostsLoading by vm.relatedPostsLoading.collectAsStateWithLifecycle()
    val tableOfContents by vm.tableOfContents.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    val hasQuestions by vm.hasQuestions.collectAsStateWithLifecycle()
    val hasExercises by vm.hasExercises.collectAsStateWithLifecycle()
    val ttsManager = ttsViewModel.ttsManager
    val addendums by vm.addendums.collectAsStateWithLifecycle()
    var selectedAddendum by remember { mutableStateOf<Addendum?>(null) }
    var showAddendumDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val isConnected by vm.isConnected.collectAsStateWithLifecycle()

    // Download state exposed by ViewModel
    val downloadProgress by vm.downloadProgress.collectAsStateWithLifecycle()
    val downloadUri by vm.downloadUri.collectAsStateWithLifecycle()
    val downloadError by vm.downloadError.collectAsStateWithLifecycle()

    val downloadPdf: (Int) -> Unit = { id -> vm.startDownloadAndSavePdf(id) }

    // Open downloaded PDF when ViewModel reports a saved URI
    LaunchedEffect(downloadUri) {
        downloadUri?.let { uriStr ->
            try {
                val uri = android.net.Uri.parse(uriStr)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(
                    context,
                    "PDF uloženo do složky Stažené",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Timber.e(e, "Error opening downloaded PDF")
                android.widget.Toast.makeText(
                    context,
                    "Chyba při otevření PDF",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } finally {
                vm.clearDownloadUri()
            }
        }
    }
    
    LaunchedEffect(postId) {
        // Delegate all heavy work to ViewModel to avoid IO in Composables
        vm.loadAllForPost(postId)
        loaded = true
    }

    LaunchedEffect(postId, postDetail?.categoryId, posts, isOffline) {
        // Post detail a posts se načítají async; pro cvičení navázaná na kategorii potřebujeme znát categoryId.
        val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId ?: postDetail?.categoryId
        if (postCategoryId == null && postDetail?.id != postId) return@LaunchedEffect

        // Jakmile známe categoryId, dotáhneme cvičení (kvůli category-based přiřazení)
        try {
            vm.loadExercisesByPostId(postId, postCategoryId)
        } catch (e: Exception) {
            Timber.e(e, "Error reloading exercises")
        }
    }

    // Re-načteme související články jakmile je postDetail dostupný,
    // protože první volání loadRelatedPosts proběhlo před načtením postDetail/posts.
    LaunchedEffect(postDetail?.id) {
        if (postDetail?.id == postId) {
            vm.loadRelatedPosts(postId)
        }
    }

    
    val aiToolsVm: AiToolsViewModel = hiltViewModel()
    var aiInputText by remember { mutableStateOf("") }
    var aiInputExpanded by remember { mutableStateOf(false) }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    var showAiToolsSheet by remember { mutableStateOf(false) }
    var showSentenceSelectPanel by remember { mutableStateOf(false) }
    var showTocDialog by remember { mutableStateOf(false) }

    // Person card
    var personNameForSheet by remember { mutableStateOf<String?>(null) }
    val isConnectedForPerson by vm.isConnected.collectAsStateWithLifecycle()
    LaunchedEffect(postId, isConnectedForPerson) {
        if (isConnectedForPerson) {
            val clientId = "tobiso-android"
            val deviceId = com.tobiso.tobisoappnative.AiCreditManager.instance.deviceId
            vm.detectPersonsForPost(postId, clientId, deviceId)
        }
    }
    
    // Scroll behavior pro nested scrolling
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isOffline) {
                isRefreshing = true
                coroutineScope.launch {
                    vm.loadPostDetail(postId)
                    vm.loadRelatedPosts(postId)
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val breadcrumb by vm.breadcrumb.collectAsStateWithLifecycle()
                LaunchedEffect(postDetail?.categoryId) { vm.loadBreadcrumb(postDetail?.categoryId) }
                TopAppBar(
                    title = {
                        Text(breadcrumb, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    actions = {
                        PostActionsRow(
                            postDetail = postDetail,
                            favoritePosts = favoritePosts,
                            isOffline = !isConnected,
                            ttsManager = ttsManager,
                            onTts = {
                                val plainText = TextUtils.extractPlainTextForTts(postDetail?.activeContent ?: "")
                                if (plainText.isNotEmpty()) {
                                    ttsViewModel.speak(plainText)
                                }
                            },
                            onToggleFavorite = {
                                postDetail?.let {
                                    val isFavorite = favoritePosts.any { it.id == postDetail?.id }
                                    if (isFavorite) vm.unsavePost(it.id) else vm.savePost(it)
                                }
                            },
                            onDownloadClick = {
                                postDetail?.id?.let { id -> downloadPdf(id) }
                            },
                            onShareClick = {
                                postDetail?.let { post ->
                                    val slug = post.filePath.removeSuffix(".md").removeSuffix(".html")
                                    val url = "https://www.tobiso.com/post$slug"
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            },
                            onAiToolsClick = { showAiToolsSheet = true },
                            onTocClick = { showTocDialog = true },
                            hasToc = tableOfContents.isNotEmpty(),
                            onBack = { navController.popBackStack() }
                        )
                    }
                )

                // Download progress (isolated small composable to limit recompositions)
                DownloadProgressBar(vm)

                when {
                    postDetailError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Chyba při načítání článku",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    postDetailError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    !loaded || postDetail == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        var renderError by remember { mutableStateOf<String?>(null) }
                        
                        if (renderError != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Chyba při zobrazení článku",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        renderError ?: "",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { navController.popBackStack() }) {
                                        Text("Zpět")
                                    }
                                }
                            }
                        } else {
                            val contentElements by vm.parsedContent.collectAsStateWithLifecycle()
                            val wordCountText by vm.wordCountText.collectAsStateWithLifecycle()
                            val createdFormatted by vm.createdFormatted.collectAsStateWithLifecycle()
                            val updatedFormatted by vm.updatedFormatted.collectAsStateWithLifecycle()

                            val lazyListState = remember { LazyListState() }

                            // Reading progress → "Pokračovat ve čtení" (+ server when signed in)
                            LaunchedEffect(lazyListState, postId) {
                                snapshotFlow { (readFractionOf(lazyListState) * 100).toInt() }.distinctUntilChanged().debounce(1500).collect { vm.reportReadProgress(postId, it) }
                            }

                            // item0=header → content starts at index 1
                            val contentItemOffset = 1
                            val activeHeadingIdx by remember {
                                derivedStateOf {
                                    val contentIdx = lazyListState.firstVisibleItemIndex - contentItemOffset
                                    if (contentIdx < 0) return@derivedStateOf -1
                                    tableOfContents.indexOfLast { it.elementIndex <= contentIdx }
                                }
                            }

                            val readFraction by remember {
                                derivedStateOf { readFractionOf(lazyListState) }
                            }
                            LinearProgressIndicator(
                                progress = { readFraction },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                                gapSize = 0.dp,
                                drawStopIndicator = {}
                            )

                            // Nový custom markdown renderer
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 16.dp, end = 16.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = postDetail?.title ?: "",
                                        fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        postDetail?.activeVersion?.gradeName?.let {
                                            GradeBadge(
                                                gradeName = it,
                                                reviewed = postDetail?.activeVersion?.isChecked == true,
                                                onClick = if (grades.size > 1) ({ showGradePicker = true }) else null
                                            )
                                        }
                                        wordCountText?.let {
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                // Render each content element as its own lazy item (virtualized)
                                itemsIndexed(contentElements, key = { index, _ -> index }) { _, element ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ElementRenderer(
                                            element = element,
                                            isOffline = isOffline,
                                            posts = posts,
                                            addendums = addendums,
                                            navController = navController,
                                            onAddendumSelected = { add -> selectedAddendum = add; showAddendumDialog = true },
                                            showImagePaths = false,
                                            onPersonSelected = { name -> personNameForSheet = name }
                                        )
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (loaded) Spacer(modifier = Modifier.height(20.dp))
                                }
                                item {
                                    ExerciseButtonsRow(
                                        hasExercises = hasExercises,
                                        exercisesLoading = exercisesLoading,
                                        exercises = exercises,
                                        onLoadExercises = {
                                            coroutineScope.launch {
                                                try {
                                                    val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId
                                                        ?: postDetail?.categoryId
                                                    vm.loadExercisesByPostId(postId, postCategoryId)
                                                    android.widget.Toast.makeText(context, "Načítám cvičení…", android.widget.Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Error loading exercises")
                                                    android.widget.Toast.makeText(context, "Chyba při načítání cvičení", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onOpenExercise = { id, type ->
                                            coroutineScope.launch {
                                                try {
                                                    when (type) {
                                                        "timeline" -> navController.navigate(ExerciseTimelineRoute(exerciseId = id))
                                                        "drag-drop" -> navController.navigate(ExerciseDragDropRoute(exerciseId = id))
                                                        "matching" -> navController.navigate(ExerciseMatchingRoute(exerciseId = id))
                                                        "circuit" -> navController.navigate(ExerciseCircuitRoute(exerciseId = id))
                                                        else -> android.widget.Toast.makeText(context, "Nepodporovaný typ cvičení: $type", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Error opening exercise")
                                                    android.widget.Toast.makeText(context, "Chyba při otevírání cvičení", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                                if (hasQuestions || questions.isNotEmpty()) {
                                    item {
                                        QuizCtaCard(
                                            questionCount = questions.size,
                                            onStart = { navController.navigate(QuestionsRoute(postId = postId)) },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                item {
                                    DifficultyRating(
                                        state = difficulty,
                                        onRate = { vm.rateDifficulty(postId, it) },
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                item {
                                    val linkedPostIds = remember(contentElements) {
                                        contentElements.mapNotNull { (it as? ContentElement.ClickableLink)?.postId }.toSet()
                                    }
                                    val filteredRelated = relatedPosts.filter { it.relatedPostId !in linkedPostIds }
                                    RelatedPostsList(relatedPosts = filteredRelated, posts = posts, navController = navController)
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        @Composable
                                        fun RowScope.DateBox(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(MaterialTheme.colorScheme.surfaceContainer, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                                Column {
                                                    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                        DateBox(Icons.Rounded.EditCalendar, "Vytvořeno", createdFormatted)
                                        if (updatedFormatted.isNotBlank()) {
                                            DateBox(Icons.Rounded.Update, "Upraveno", updatedFormatted)
                                        }
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(120.dp))
                                }
                            }

                            if (showTocDialog) {
                                TableOfContentsDialog(
                                    entries = tableOfContents,
                                    activeEntryIndex = activeHeadingIdx,
                                    onEntryClick = { entry ->
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(entry.elementIndex + contentItemOffset)
                                        }
                                    },
                                    onDismiss = { showTocDialog = false }
                                )
                            }
                    }
                }
            }
        }
            // Long-press now only shows floating button; plain-text navigation is handled by the FAB.

            

        // Floating toolbar — TTS, favourite, ask AI. The pill expands in place into a text input.
        if (loaded && postDetail != null) {
            val postTitle = postDetail?.title ?: ""
            val isFav = favoritePosts.any { it.id == postDetail?.id }
            val aiFocus = remember { androidx.compose.ui.focus.FocusRequester() }
            val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val sendAi: () -> Unit = {
                if (aiInputText.isNotBlank()) {
                    if (hasAiConsent(context)) {
                        navController.navigate(
                            AiChatRoute(
                                postId = postId,
                                postTitle = android.net.Uri.encode(postTitle),
                                firstUserMessage = android.net.Uri.encode(aiInputText)
                            )
                        )
                        aiInputText = ""
                        aiInputExpanded = false
                    } else {
                        showAiConsentDialog = true
                    }
                }
            }
            LaunchedEffect(aiInputExpanded) {
                if (aiInputExpanded) {
                    kotlinx.coroutines.delay(120)
                    runCatching { aiFocus.requestFocus() }
                    keyboard?.show()
                } else {
                    keyboard?.hide()
                }
            }
            androidx.activity.compose.BackHandler(enabled = aiInputExpanded) { aiInputExpanded = false }

            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(if (aiInputExpanded) 28.dp else 50.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.animateContentSize(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    AnimatedContent(
                        targetState = aiInputExpanded,
                        transitionSpec = { (fadeIn(tween(180, 80)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(100)) },
                        label = "askAi"
                    ) { expanded ->
                        if (expanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(onClick = { aiInputExpanded = false }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Zavřít")
                                }
                                Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    if (aiInputText.isEmpty()) {
                                        Text(
                                            "Zeptej se na cokoliv k článku…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = aiInputText,
                                        onValueChange = { aiInputText = it },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = { sendAi() }),
                                        modifier = Modifier.fillMaxWidth().focusRequester(aiFocus)
                                    )
                                }
                                val canSend = aiInputText.isNotBlank()
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable(enabled = canSend) { sendAi() },
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Odeslat",
                                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val plainText = TextUtils.extractPlainTextForTts(postDetail?.activeContent ?: "")
                                        if (plainText.isNotEmpty()) ttsViewModel.speak(plainText)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) { Icon(Icons.Rounded.VolumeUp, contentDescription = "Přečíst článek") }
                                IconButton(
                                    onClick = { postDetail?.let { if (isFav) vm.unsavePost(it.id) else vm.savePost(it) } },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        if (isFav) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                        contentDescription = if (isFav) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                if (isConnected) {
                                    Surface(
                                        onClick = { aiInputExpanded = true },
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.height(48.dp).padding(horizontal = 18.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Text("Zeptat se", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog pro vysvětlení permission
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Ukládání PDF") },
                text = { Text("PDF soubor bude uložen do složky Download/.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        postDetail?.id?.let { downloadPdf(it) }
                    }) {
                        Text("Stáhnout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Zrušit")
                    }
                }
            )
        }

        // Person info bottom sheet
        personNameForSheet?.let { name ->
            com.tobiso.tobisoappnative.components.PersonBottomSheet(
                personName = name,
                clientId = "tobiso-android",
                deviceId = com.tobiso.tobisoappnative.AiCreditManager.instance.deviceId,
                onDismiss = { personNameForSheet = null }
            )
        }

        // AI nástroje – bottom sheet
        if (showAiToolsSheet) {
            com.tobiso.tobisoappnative.components.AiToolsBottomSheet(
                postId = postId,
                onDismiss = { showAiToolsSheet = false },
                onEnterSentenceSelectMode = {
                    showAiToolsSheet = false
                    aiToolsVm.clearExplain()
                    showSentenceSelectPanel = true
                },
                vm = aiToolsVm
            )
        }

        // Sentence select panel – non-modal, anchored at bottom
        if (showSentenceSelectPanel) {
            SentenceSelectPanel(
                postContent = postDetail?.activeContent,
                vm = aiToolsVm,
                postId = postId,
                onDismiss = {
                    showSentenceSelectPanel = false
                    aiToolsVm.clearExplain()
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }

        // Souhlas s podmínkami AI
        if (showAiConsentDialog) {
            AiConsentDialog(
                navController = navController,
                onAccepted = {
                    saveAiConsent(context)
                    showAiConsentDialog = false
                    if (aiInputText.isNotBlank()) {
                        val title = postDetail?.title ?: ""
                        navController.navigate(
                            AiChatRoute(
                                postId = postId,
                                postTitle = android.net.Uri.encode(title),
                                firstUserMessage = android.net.Uri.encode(aiInputText)
                            )
                        )
                        aiInputText = ""
                        aiInputExpanded = false
                    }
                },
                onDismissed = { showAiConsentDialog = false }
            )
        }
        
        // Dialog pro zobrazení dodatku
        if (showAddendumDialog && selectedAddendum != null) {
            AddendumDialog(addendum = selectedAddendum) {
                showAddendumDialog = false
                selectedAddendum = null
            }
        }
    }
}

/** 0 at the very top, 1 once the end of the list is reached, linear in between. */
private fun readFractionOf(state: LazyListState): Float {
    if (!state.canScrollBackward) return 0f
    if (!state.canScrollForward) return 1f
    val info = state.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo
    val first = visible.firstOrNull() ?: return 0f
    val within = if (first.size > 0) (-first.offset).coerceAtLeast(0).toFloat() / first.size else 0f
    val position = first.index + within
    val maxPosition = (total - visible.size).coerceAtLeast(1).toFloat()
    return (position / maxPosition).coerceIn(0f, 0.99f)
}

@Composable
private fun DownloadProgressBar(vm: PostDetailViewModel, modifier: Modifier = Modifier) {
    val downloadProgress by vm.downloadProgress.collectAsStateWithLifecycle()
    val progress = downloadProgress
    if (progress != null) {
        if (progress >= 0) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = modifier.fillMaxWidth()
            )
            Text(
                text = "Stahování: $progress%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textAlign = TextAlign.End
            )
        } else {
            LinearProgressIndicator(modifier = modifier.fillMaxWidth())
            Text(
                text = "Stahování…",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

