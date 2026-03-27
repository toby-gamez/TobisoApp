package com.tobiso.tobisoappnative.screens
import timber.log.Timber

import com.tobiso.tobisoappnative.navigation.AiChatRoute
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseDragDropRoute
import com.tobiso.tobisoappnative.navigation.ExerciseMatchingRoute
import com.tobiso.tobisoappnative.navigation.ExerciseTimelineRoute
import com.tobiso.tobisoappnative.navigation.PlainTextRoute
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.postdetail.PostDetailViewModel
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.TimeZone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.text.ClickableText
// selection removed from this screen — selection moved to PlainTextScreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.AnnotatedString
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Addendum
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// combinedClickable not used here
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.TtsPlayer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.tobiso.tobisoappnative.utils.TextUtils
import com.tobiso.tobisoappnative.utils.hasAiConsent
import com.tobiso.tobisoappnative.utils.saveAiConsent
import com.tobiso.tobisoappnative.components.AiConsentDialog
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.runtime.saveable.rememberSaveable
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Environment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import kotlin.getOrElse
import coil.compose.AsyncImage
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.tobiso.tobisoappnative.components.InlineFraction
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.AstBlockNodeComposer
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.markdown.node.AstBlockNodeType
import com.halilibo.richtext.markdown.node.AstCode
import com.halilibo.richtext.markdown.node.AstEmphasis
import com.halilibo.richtext.markdown.node.AstHardLineBreak
import com.halilibo.richtext.markdown.node.AstHtmlInline
import com.halilibo.richtext.markdown.node.AstImage
import com.halilibo.richtext.markdown.node.AstLink
import com.halilibo.richtext.markdown.node.AstNode
import com.halilibo.richtext.markdown.node.AstParagraph
import com.halilibo.richtext.markdown.node.AstSoftLineBreak
import com.halilibo.richtext.markdown.node.AstStrikethrough
import com.halilibo.richtext.markdown.node.AstStrongEmphasis
import com.halilibo.richtext.markdown.node.AstText
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.string.InlineContent
import com.halilibo.richtext.ui.string.RichTextString
import com.tobiso.tobisoappnative.components.AddendumDialog
import com.tobiso.tobisoappnative.components.AiInputBar
import com.halilibo.richtext.ui.string.Text as RichTextScopeText
import kotlin.math.max
import com.tobiso.tobisoappnative.components.SafeMarkdown
import com.tobiso.tobisoappnative.components.parseContentToElements
import com.tobiso.tobisoappnative.components.ContentElement
import com.tobiso.tobisoappnative.components.ContentRenderer
import com.tobiso.tobisoappnative.components.PostActionsRow
import com.tobiso.tobisoappnative.components.ExerciseButtonsRow
import com.tobiso.tobisoappnative.components.RelatedPostsList
import kotlinx.serialization.json.JsonNull.content

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    navController: NavController,
    ttsViewModel: TtsViewModel
) {
    val vm: PostDetailViewModel = hiltViewModel()
    val postDetail by vm.postDetail.collectAsState()
    val postDetailError by vm.postDetailError.collectAsState()
    val favoritePosts by vm.favoritePosts.collectAsState()
    val posts by vm.posts.collectAsState()
    val isOffline by vm.isOffline.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val exercisesLoading by vm.exercisesLoading.collectAsState()
    val exercisesError by vm.exercisesError.collectAsState()
    val questions by vm.questions.collectAsState()
    val relatedPosts by vm.relatedPosts.collectAsState()
    val relatedPostsError by vm.relatedPostsError.collectAsState()
    val relatedPostsLoading by vm.relatedPostsLoading.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    val hasQuestions by vm.hasQuestions.collectAsState()
    val hasExercises by vm.hasExercises.collectAsState()
    val ttsManager = ttsViewModel.ttsManager
    val addendums by vm.addendums.collectAsState()
    var selectedAddendum by remember { mutableStateOf<Addendum?>(null) }
    var showAddendumDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPdfDownload by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val isConnected by vm.isConnected.collectAsState()

    // Download state exposed by ViewModel
    val downloadProgress by vm.downloadProgress.collectAsState()
    val downloadUri by vm.downloadUri.collectAsState()
    val downloadError by vm.downloadError.collectAsState()

    val downloadPdf: (Int) -> Unit = { id -> vm.startDownloadAndSavePdf(id) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingPdfDownload) {
            pendingPdfDownload = false
            postDetail?.id?.let { downloadPdf(it) }
        } else if (!isGranted) {
            pendingPdfDownload = false
            android.widget.Toast.makeText(
                context,
                "Pro stažení PDF je potřeba oprávnění k úložišti",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

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

    var showFloatingSelectButton by remember { mutableStateOf(false) }
    var aiInputText by remember { mutableStateOf("") }
    var aiInputExpanded by remember { mutableStateOf(false) }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    
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
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                LargeTopAppBar(
                    title = { 
                        val collapsedFraction = scrollBehavior.state.collapsedFraction
                        val fontSize = (28 - (12 * collapsedFraction)).sp
                        Text(
                            text = postDetail?.title ?: "Detail článku",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize),
                            maxLines = 1
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        PostActionsRow(
                            postDetail = postDetail,
                            favoritePosts = favoritePosts,
                            isOffline = isOffline,
                            ttsManager = ttsManager,
                            onTts = {
                                val plainText = TextUtils.extractPlainTextForTts(postDetail?.content ?: "")
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
                                postDetail?.id?.let { id ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        downloadPdf(id)
                                    } else {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            downloadPdf(id)
                                        } else {
                                            pendingPdfDownload = true
                                            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                            },
                            onShareClick = {
                                postDetail?.id?.let { id ->
                                    val url = "https://www.tobiso.com/post/$id"
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            },
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
                            Text(
                                "Chyba při načítání článku: ${postDetailError}",
                                color = MaterialTheme.colorScheme.error
                            )
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
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Text(
                                        "Chyba při zobrazení článku:",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        renderError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { navController.popBackStack() }) {
                                        Text("Zpět")
                                    }
                                }
                            }
                        } else {
                            val contentElements by vm.parsedContent.collectAsState()
                            val wordCountText by vm.wordCountText.collectAsState()
                            val createdFormatted by vm.createdFormatted.collectAsState()
                            val updatedFormatted by vm.updatedFormatted.collectAsState()

                            // Use LazyColumn for the article body to avoid composing everything at once
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                item {
                                    wordCountText?.let { infoText ->
                                        Text(
                                            text = infoText,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(onLongPress = { showFloatingSelectButton = true })
                                        }
                                    ) {
                                        ContentRenderer(
                                            contentElements = contentElements,
                                            isOffline = isOffline,
                                            posts = posts,
                                            addendums = addendums,
                                            navController = navController,
                                            onAddendumSelected = { add -> selectedAddendum = add; showAddendumDialog = true }
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
                                        hasQuestions = hasQuestions || questions.isNotEmpty(),
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
                                        },
                                        onOpenQuestions = { navController.navigate(QuestionsRoute(postId = postId)) }
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
                                    Text(
                                        text = "Vytvořeno: $createdFormatted",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Start
                                    )
                                    if (updatedFormatted.isNotBlank()) {
                                        Text(
                                            text = "Upraveno: $updatedFormatted",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
            // Long-press now only shows floating button; plain-text navigation is handled by the FAB.

            // Auto-hide floating button after a short period
            LaunchedEffect(showFloatingSelectButton) {
                if (showFloatingSelectButton) {
                    delay(4000)
                    showFloatingSelectButton = false
                }
            }

        // Sticky bottom action bar — AI + Prověrka + Cvičení
        // Nezobrazujeme bar dokud se nedokončí úvodní načítání (loaded=true) a nemáme internet
        val showActionsBar = loaded && isConnected
        if (showActionsBar) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 0.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val postTitle = postDetail?.title ?: ""
                    AiInputBar(
                        aiInputText = aiInputText,
                        onTextChange = { aiInputText = it },
                        aiInputExpanded = aiInputExpanded,
                        onExpandedChange = { aiInputExpanded = it },
                        postTitle = postTitle,
                        enabled = aiInputText.isNotBlank(),
                        onSend = {
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
                    )
                }
            }
        }

        // Floating select button (levitující) — zobrazuje se po podržení
        androidx.compose.animation.AnimatedVisibility(
            visible = showFloatingSelectButton,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = if (loaded && isConnected) 88.dp else 16.dp)
                    .zIndex(1f),
                contentAlignment = androidx.compose.ui.Alignment.BottomEnd
            ) {
                FloatingActionButton(onClick = {
                    showFloatingSelectButton = false
                    navController.navigate(PlainTextRoute(postId = postId))
                }) {
                    Icon(Icons.Default.TextFields, contentDescription = "Vybrat text")
                }
            }
        }

        // Dialog pro vysvětlení permission
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Oprávnění k úložišti") },
                text = { Text("Pro stažení PDF souboru potřebujeme přístup k úložišti vašeho zařízení.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        pendingPdfDownload = true
                        permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }) {
                        Text("Povolit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Zrušit")
                    }
                }
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

@Composable
private fun DownloadProgressBar(vm: PostDetailViewModel, modifier: Modifier = Modifier) {
    val downloadProgress by vm.downloadProgress.collectAsState()
    if (downloadProgress != null) {
        if (downloadProgress!! >= 0) {
            LinearProgressIndicator(
                progress = (downloadProgress!! / 100f),
                modifier = modifier.fillMaxWidth()
            )
            Text(
                text = "Stahování: ${downloadProgress}%",
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

