package com.example.tobisoappnative.screens

import android.net.Uri
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
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.AnnotatedString
import com.example.tobisoappnative.model.ApiClient
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.ClipboardManager
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

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
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()
    var showError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var hasQuestions by remember { mutableStateOf(false) }
    
    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
        // Kontrola otázek pro tento příspěvek
        hasQuestions = try {
            viewModel.checkHasQuestions(postId)
        } catch (e: Exception) {
            // Pro testování zobrazíme tlačítko vždy, když API neexistuje
            true
        }
        loaded = true
    }

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var lastClipboardText by remember { mutableStateOf("") }
    var showCopyDialog by remember { mutableStateOf(false) }
    var copiedText by remember { mutableStateOf("") }
    var showSavedSnackbar by remember { mutableStateOf(false) }
    val postContent = postDetail?.content ?: ""

    // Detekce kopírování přes clipboard
    LaunchedEffect(postContent) {
        while (true) {
            val clipboardText = clipboardManager.getText()?.text ?: ""
            if (
                clipboardText.isNotBlank() &&
                postContent.contains(clipboardText) &&
                clipboardText != lastClipboardText
            ) {
                showCopyDialog = true
                copiedText = clipboardText
                lastClipboardText = clipboardText
            }
            delay(500)
        }
    }

    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.loadPostDetail(postId)
                    isRefreshing = false
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LargeTopAppBar(
                    title = { Text(postDetail?.title ?: "Detail článku") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
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
                        // SHARE BUTTON - druhé vpravo
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
                )

                // Zobrazení počtu slov a času čtení pod nadpisem
                if (postDetail?.content != null) {
                    val words = postDetail!!.content.trim().split("\\s+".toRegex()).size
                    val minutes = Math.ceil(words / 200.0).toInt().coerceAtLeast(1)
                    val infoText = "$words slov | ~${minutes} min čtení"
                    Text(
                        text = infoText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }

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
                            Spacer(modifier = Modifier.height(8.dp))
                            postDetail?.content?.let { content ->
                                // Nejprve nahradíme obrázky
                                val imageRegex = Regex("!\\[(.*?)]\\((images/[^)]+)\\)")
                                var processedContent = content.replace(imageRegex) {
                                    val alt = it.groupValues[1]
                                    val path = it.groupValues[2]
                                    "![${alt}](https://tobiso.com/${path})"
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

                                if (allMatches.isEmpty()) {
                                    SelectionContainer {
                                        RichText { Markdown(processedContent) }
                                    }
                                } else {
                                    var lastIndex = 0
                                    Column {
                                        for ((start, end, typeAndMatch) in allMatches) {
                                            // Text před aktuálním elementem
                                            if (start > lastIndex) {
                                                val before =
                                                    processedContent.substring(lastIndex, start)
                                                SelectionContainer {
                                                    RichText { Markdown(before) }
                                                }
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
                                                        SelectionContainer {
                                                            RichText { Markdown(blockText) }
                                                        }
                                                    }
                                                }

                                                "link" -> {
                                                    // Klikatelný odkaz
                                                    val match = typeAndMatch.second as MatchResult
                                                    val linkText = match.groupValues[1]
                                                    var url = match.groupValues[2]
                                                    var fileName = url
                                                    if (fileName.endsWith(".html")) fileName =
                                                        fileName.removeSuffix(".html") + ".md"
                                                    fileName = fileName.replace(prefixRegex, "")
                                                    if (!fileName.startsWith("/")) fileName =
                                                        "/$fileName"
                                                    // Pokud url obsahuje "files", přidej předponu
                                                    if (url.startsWith("files") || url.contains("/files/")) {
                                                        url =
                                                            "https://tobiso.com/" + url.removePrefix(
                                                                "/"
                                                            )
                                                    }
                                                    ClickableText(
                                                        text = AnnotatedString(linkText),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = MaterialTheme.colorScheme.primary
                                                        ),
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    val postsApi =
                                                                        ApiClient.apiService.getPosts()
                                                                    val post =
                                                                        postsApi.find { it.filePath == fileName }
                                                                    if (post != null) {
                                                                        navController.navigate("postDetail/${post.id}")
                                                                        showError = false
                                                                    } else {
                                                                        // Otevřít v prohlížeči, pokud url obsahuje http nebo začíná na https://tobiso.com/files
                                                                        if (url.contains("http") || url.startsWith(
                                                                                "https://tobiso.com/files"
                                                                            )
                                                                        ) {
                                                                            val intent =
                                                                                android.content.Intent(
                                                                                    android.content.Intent.ACTION_VIEW,
                                                                                    android.net.Uri.parse(
                                                                                        url
                                                                                    )
                                                                                )
                                                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                            try {
                                                                                navController.context.startActivity(
                                                                                    intent
                                                                                )
                                                                                showError = false
                                                                            } catch (e: Exception) {
                                                                                errorText =
                                                                                    "Nelze otevřít odkaz: ${e.message}"
                                                                                showError = true
                                                                            }
                                                                        } else {
                                                                            errorText =
                                                                                "Soubor '$fileName' nebyl nalezen."
                                                                            showError = true
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    errorText =
                                                                        "Chyba při načítání postů: ${e.message}"
                                                                    showError = true
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                "video" -> {
                                                    val match = typeAndMatch.second as MatchResult
                                                    val videoSrc = match.groupValues[1]
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
                                            SelectionContainer {
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
                            
                            val locale = java.util.Locale("cs", "CZ")
                            val formatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale)
                            val createdFormatted = postDetail?.createdAt?.let {
                                try {
                                    formatter.format(it)
                                } catch (e: Exception) {
                                    ""
                                }
                            } ?: ""
                            val updatedFormatted = postDetail?.updatedAt?.let {
                                try {
                                    formatter.format(it)
                                } catch (e: Exception) {
                                    ""
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
            // Snackbar zůstává mimo SwipeRefresh
            if (showError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    Snackbar(
                        action = {
                            TextButton(onClick = { showError = false }) {
                                Text(
                                    "Zavřít",
                                    textAlign = TextAlign.Start
                                )
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(errorText, textAlign = TextAlign.Start)
                    }
                }
            }
        }
        // Dialog pro uložení útržku
        if (showCopyDialog) {
            AlertDialog(
                onDismissRequest = { showCopyDialog = false },
                title = { Text("Uložit do útržků?") },
                text = { Text("Útržky jsou kousky textu, které si uložíte na příště nebo vás zajímají. Možnosti jsou neomezené.") },
                confirmButton = {
                    TextButton(onClick = {
                        val snippet = com.example.tobisoappnative.model.Snippet(
                            postId = postId,
                            content = copiedText,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addSnippet(snippet)
                        showCopyDialog = false
                        showSavedSnackbar = true
                    }) {
                        Text("Ano")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCopyDialog = false }) {
                        Text("Ne")
                    }
                }
            )
        }
        // Snackbar po uložení útržku
        if (showSavedSnackbar) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        Row {
                            TextButton(onClick = { showSavedSnackbar = false }) {
                                Text("Zavřít", textAlign = TextAlign.Start)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                showSavedSnackbar = false
                                navController.navigate("favorites")
                            }) {
                                Text("Zobrazit", textAlign = TextAlign.Start)
                            }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Útržek uložen.", textAlign = TextAlign.Start)
                }
            }
        }
    }
}
