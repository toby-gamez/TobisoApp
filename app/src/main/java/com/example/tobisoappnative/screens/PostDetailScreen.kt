package com.example.tobisoappnative.screens

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.text.ClickableText
// selection removed from this screen — selection moved to PlainTextScreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.AnnotatedString
import com.example.tobisoappnative.model.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
// combinedClickable not used here
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import com.example.tobisoappnative.components.MultiplierIndicator
import com.example.tobisoappnative.components.TtsPlayer
import androidx.compose.material.icons.filled.VolumeUp
import com.example.tobisoappnative.utils.TextUtils

val prefixRegex = Regex("^(ml-|sl-|li-|hv-|m-|ch-|f-|pr-|z-)")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val postDetail by viewModel.postDetail.collectAsState()
    val postDetailError by viewModel.postDetailError.collectAsState()
    val favoritePosts by viewModel.favoritePosts.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val relatedPosts by viewModel.relatedPosts.collectAsState()
    val relatedPostsError by viewModel.relatedPostsError.collectAsState()
    val relatedPostsLoading by viewModel.relatedPostsLoading.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var hasQuestions by remember { mutableStateOf(false) }
    val ttsManager = viewModel.getTtsManager()
    
    LaunchedEffect(postId) {
        // Načteme detail (ViewModel má logiku pro offline i online režim)
        viewModel.loadPostDetail(postId)
        // Načteme související články (pouze v online režimu)
        if (!isOffline) {
            viewModel.loadRelatedPosts(postId)
        }
        loaded = true
    }
    
    // Separate effect pro kontrolu otázek a posts - reaguje na změny offline stavu
    LaunchedEffect(isOffline, posts.isEmpty()) {
        // Načteme všechny posts pro vyhledávání odkazů (pouze pokud není offline nebo jsou prázdné)
        if (posts.isEmpty() && !isOffline) {
            viewModel.loadPosts()
        }
        
        // Kontrola otázek pro tento příspěvek (nyní funguje v online i offline režimu)
        hasQuestions = try {
            viewModel.checkHasQuestions(postId)
        } catch (e: Exception) {
            println("DEBUG: Error checking questions: ${e.message}")
            false
        }
    }

    var showFloatingSelectButton by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Scroll behavior pro nested scrolling
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (!isOffline) {
                    isRefreshing = true
                    coroutineScope.launch {
                        viewModel.loadPostDetail(postId)
                        viewModel.loadRelatedPosts(postId)
                        isRefreshing = false
                    }
                } else {
                    // V offline režimu jen resetujeme refresh state
                    isRefreshing = false
                }
            }
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
                        // Zobrazení aktivního multiplikátoru
                        MultiplierIndicator()
                        
                        // TTS BUTTON - nejlevější tlačítko
                        if (ttsManager != null && postDetail?.content != null) {
                            IconButton(onClick = {
                                val plainText = TextUtils.extractPlainTextForTts(postDetail!!.content)
                                if (plainText.isNotEmpty()) {
                                    viewModel.speakText(plainText)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Přečíst článek",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        val isFavorite = favoritePosts.any { it.id == postDetail?.id }
                        // HVĚZDIČKA - první vpravo
                        IconButton(onClick = {
                            postDetail?.let {
                                if (isFavorite) viewModel.unsavePost(it.id) else viewModel.savePost(it)
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // SHARE BUTTON - druhé vpravo (pouze v online režimu)
                        if (!isOffline) {
                            IconButton(onClick = {
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
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Sdílet odkaz",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )

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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Zobrazení počtu slov a času čtení
                            if (postDetail?.content != null) {
                                val words = postDetail!!.content.trim().split("\\s+".toRegex()).size
                                val minutes = Math.ceil(words / 200.0).toInt().coerceAtLeast(1)
                                val infoText = "$words slov | ~${minutes} min čtení"
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
                            postDetail?.content?.let { content ->
                                // Nejprve nahradíme nebo odstraníme obrázky 
                                val imageRegex = Regex("!\\[(.*?)]\\((images/[^)]+)\\)")
                                var processedContent = if (!isOffline) {
                                    content.replace(imageRegex) {
                                        val alt = it.groupValues[1]
                                        val path = it.groupValues[2]
                                        "![${alt}](https://tobiso.com/${path})"
                                    }
                                } else {
                                    // V offline režimu úplně odstraníme obrázky a nahradíme jen textem
                                    content.replace(imageRegex) {
                                        val alt = it.groupValues[1]
                                        "\n\n**[Obrázek: $alt - nedostupný v offline režimu]**\n\n"
                                    }
                                }

                                // Najdeme zvýrazněné bloky ...text...
                                val blockRegex = Regex("\\.\\.\\.\\s*([\\s\\S]*?)\\s*\\.\\.\\.")
                                val blockMatches = blockRegex.findAll(processedContent).toList()

                                // Najdeme odkazy [text](url) ale ne obrázky ![alt](url)
                                val linkRegex = Regex("(?<!!)\\[(.+?)\\]\\((.+?)\\)")
                                val linkMatches = linkRegex.findAll(processedContent).toList()

                                // Detekce video tagu včetně obsahu a closing tagu
                                val videoRegex = Regex(
                                    "<video[^>]*src=\"([^\"]+)\"[^>]*>(.*?)</video>",
                                    RegexOption.DOT_MATCHES_ALL
                                )
                                val videoMatches = videoRegex.findAll(processedContent).toList()

                                // Kombinujeme všechny matches a seřadíme podle pozice
                                val allMatches = (blockMatches.map {
                                    Triple(it.range.first, it.range.last + 1, "block" to it)
                                } + linkMatches.map {
                                    Triple(it.range.first, it.range.last + 1, "link" to it)
                                } + videoMatches.map {
                                    Triple(it.range.first, it.range.last + 1, "video" to it)
                                }).sortedBy { it.first }

                                // Wrap rendered markdown area with a long-press detector that does NOT
                                // interfere with inner clickable links. A long-press will show the
                                // custom "Vybrat text" UI which navigates to the plain text screen.
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                // show floating select button (ChatGPT-like)
                                                showFloatingSelectButton = true
                                            }
                                        )
                                    }
                                ) {
                                    if (allMatches.isEmpty()) {
                                        RichText { Markdown(processedContent) }
                                    } else {
                                    var lastIndex = 0
                                    Column {
                                        for ((start, end, typeAndMatch) in allMatches) {
                                            // Text před aktuálním elementem
                                                if (start > lastIndex) {
                                                val before =
                                                    processedContent.substring(lastIndex, start)
                                                RichText { Markdown(before) }
                                            }

                                            when (typeAndMatch.first) {
                                                "block" -> {
                                                    // Zvýrazněný blok
                                                    val match = typeAndMatch.second as MatchResult
                                                    val blockText = match.groupValues[1]
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.surfaceVariant,
                                                                shape = MaterialTheme.shapes.medium
                                                            )
                                                            .padding(8.dp)
                                                    ) {
                                                        RichText { Markdown(blockText) }
                                                    }
                                                }

                                                "link" -> {
                                                    // Klikatelný odkaz
                                                    val match = typeAndMatch.second as MatchResult
                                                    val linkText = match.groupValues[1]
                                                    var url = match.groupValues[2]
                                                    var filePath = url
                                                    if (filePath.endsWith(".html")) filePath =
                                                        filePath.removeSuffix(".html") + ".md"
                                                    filePath = filePath.replace(prefixRegex, "")
                                                    if (!filePath.startsWith("/")) filePath =
                                                        "/$filePath"
                                                    
                                                    ClickableText(
                                                        text = AnnotatedString(linkText),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = MaterialTheme.colorScheme.primary
                                                        ),
                                                        onClick = {
                                                            // Hledáme post podle filePath (funguje offline i online)
                                                            val post = posts.find { it.filePath == filePath }
                                                            if (post != null) {
                                                                navController.navigate("postDetail/${post.id}")
                                                            } else if (!isOffline) {
                                                                // Pouze v online režimu zkusíme otevřít externí odkazy
                                                                if (url.contains("http") || url.startsWith("files") || url.contains("/files/")) {
                                                                    val fullUrl = if (url.startsWith("http")) {
                                                                        url
                                                                    } else {
                                                                        "https://tobiso.com/" + url.removePrefix("/")
                                                                    }
                                                                    val intent = android.content.Intent(
                                                                        android.content.Intent.ACTION_VIEW,
                                                                        android.net.Uri.parse(fullUrl)
                                                                    )
                                                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    try {
                                                                        navController.context.startActivity(intent)
                                                                    } catch (e: Exception) {
                                                                        // Tiše ignorujeme chyby
                                                                    }
                                                                }
                                                            }
                                                            // V offline režimu nebo při nenalezení souboru neděláme nic
                                                        }
                                                    )
                                                }

                                                "video" -> {
                                                    val match = typeAndMatch.second as MatchResult
                                                    val videoSrc = match.groupValues[1]
                                                    
                                                    if (isOffline) {
                                                        // V offline režimu zobrazíme pouze text
                                                        Text(
                                                            text = "*[Video nedostupné v offline režimu]*",
                                                            modifier = Modifier.padding(vertical = 8.dp),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    } else {
                                                        val videoUrl =
                                                            if (videoSrc.startsWith("http")) videoSrc else "https://tobiso.com/$videoSrc"
                                                        OutlinedButton(
                                                            onClick = {
                                                                navController.navigate(
                                                                    "videoPlayer/${
                                                                        Uri.encode(
                                                                            videoUrl
                                                                        )
                                                                    }"
                                                                )
                                                            },
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "Přehrát video",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                "Video",
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                    // Nastavíme lastIndex na konec celého video bloku včetně closing tagu
                                                    lastIndex = end
                                                }
                                            }
                                            lastIndex = end
                                        }

                                        // Zbytek textu za posledním elementem
                                        if (lastIndex < processedContent.length) {
                                            val after = processedContent.substring(lastIndex)
                                            // Zobrazíme pouze text za closing tagem, closing tag ani text uvnitř videa se nezobrazí
                                            RichText { Markdown(after) }
                                        }
                                    }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Tlačítko Prověrka
                            if (hasQuestions) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            navController.navigate("questions/$postId")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("Prověrka")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // Související články
                            if (!isOffline && relatedPosts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Související články",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        relatedPosts.forEach { relatedPost ->
                                            val relatedPostData = posts.find { it.id == relatedPost.relatedPostId }
                                            if (relatedPostData != null) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    onClick = {
                                                        navController.navigate("postDetail/${relatedPost.relatedPostId}")
                                                    }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp)
                                                    ) {
                                                        Text(
                                                            text = relatedPostData.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = relatedPost.text,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val locale = java.util.Locale("cs", "CZ")
                            val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale)
                            val outputFormatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale)
                            val createdFormatted = postDetail?.createdAt?.let { dateString ->
                                try {
                                    val date = inputFormatter.parse(dateString)
                                    date?.let { outputFormatter.format(it) } ?: ""
                                } catch (e: Exception) {
                                    dateString // fallback to raw string
                                }
                            } ?: ""
                            val updatedFormatted = postDetail?.updatedAt?.let { dateString ->
                                try {
                                    val date = inputFormatter.parse(dateString)
                                    date?.let { outputFormatter.format(it) } ?: ""
                                } catch (e: Exception) {
                                    dateString // fallback to raw string
                                }
                            } ?: ""
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
                            postDetail?.filePath.takeIf { !it.isNullOrBlank() }?.let {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
            // Long-press now only shows floating button; plain-text navigation is handled by the FAB.
            // Floating select button (levitující) — zobrazuje se po podržení
            androidx.compose.animation.AnimatedVisibility(
                visible = showFloatingSelectButton,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomEnd
                ) {
                    FloatingActionButton(onClick = {
                        showFloatingSelectButton = false
                        // Proper navigation to the plain-text selectable screen
                        navController.navigate("plainText/$postId")
                    }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Vybrat text")
                    }
                }
            }

            // Auto-hide floating button after a short period
            LaunchedEffect(showFloatingSelectButton) {
                if (showFloatingSelectButton) {
                    delay(4000)
                    showFloatingSelectButton = false
                }
            }
        }
        
        // Persistent TTS player is provided globally in MainActivity.MyApp().
        // Do not render a local TtsPlayer here to avoid duplication and ensure
        // playback continues across navigation.
    }
}
