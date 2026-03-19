package com.example.tobisoappnative.screens

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
import com.example.tobisoappnative.viewmodel.postdetail.PostDetailViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Addendum
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.tobisoappnative.utils.TextUtils
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
import com.example.tobisoappnative.components.InlineFraction
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
import com.halilibo.richtext.ui.string.Text as RichTextScopeText
import kotlin.math.max

val prefixRegex = Regex("^(ml-|sl-|li-|hv-|m-|ch-|f-|pr-|z-)")

private val inlineFractionRegex = Regex(
    """(?<![\p{L}\p{N}_])([0-9]+(?:[\.,][0-9]+)?|[\p{L}])\/([0-9]+(?:[\.,][0-9]+)?|[\p{L}])(?![\p{L}\p{N}_])"""
)

private fun fractionInlineContent(numerator: String, denominator: String): InlineContent {
    return InlineContent(
        initialSize = {
            // Rough sizing so the placeholder stays on the same line.
            val maxLen = max(numerator.length, denominator.length).coerceAtLeast(1)
            val charWidth = 6.sp.toPx()
            val width = (maxLen * charWidth + 8.sp.toPx()).toInt().coerceAtLeast(1)
            val height = (18.sp.toPx()).toInt().coerceAtLeast(1)
            IntSize(width, height)
        },
        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
    ) {
        InlineFraction(
            numerator = numerator,
            denominator = denominator,
            modifier = Modifier
        )
    }
}

private fun inlineChildren(astNode: AstNode): List<AstNode> {
    val out = mutableListOf<AstNode>()
    var child = astNode.links.firstChild
    while (child != null) {
        out.add(child)
        child = child.links.next
    }
    return out
}

private fun appendTextWithFractions(builder: RichTextString.Builder, text: String) {
    var lastIndex = 0
    for (match in inlineFractionRegex.findAll(text)) {
        val start = match.range.first
        val end = match.range.last + 1
        if (start > lastIndex) builder.append(text.substring(lastIndex, start))

        val numerator = match.groupValues[1]
        val denominator = match.groupValues[2]

        val numIsWord = numerator.length > 1 && numerator.all { it.isLetter() }
        val denIsWord = denominator.length > 1 && denominator.all { it.isLetter() }
        if (numIsWord || denIsWord) {
            builder.append(match.value)
        } else {
            builder.appendInlineContent(
                alternateText = match.value,
                content = fractionInlineContent(numerator, denominator)
            )
        }

        lastIndex = end
    }
    if (lastIndex < text.length) builder.append(text.substring(lastIndex))
}

private fun appendInlineNode(builder: RichTextString.Builder, node: AstNode) {
    when (val t = node.type) {
        is AstText -> appendTextWithFractions(builder, t.literal)
        is AstSoftLineBreak -> builder.append(" ")
        is AstHardLineBreak -> builder.append("\n")
        is AstHtmlInline -> builder.append(t.literal)
        is AstCode -> {
            val idx = builder.pushFormat(RichTextString.Format.Code)
            builder.append(t.literal)
            builder.pop(idx)
        }
        is AstEmphasis -> {
            val idx = builder.pushFormat(RichTextString.Format.Italic)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstStrongEmphasis -> {
            val idx = builder.pushFormat(RichTextString.Format.Bold)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstStrikethrough -> {
            val idx = builder.pushFormat(RichTextString.Format.Strikethrough)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstLink -> {
            val idx = builder.pushFormat(RichTextString.Format.Link(t.destination))
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstImage -> {
            // If an image is mixed into a sentence, keep at least the title as fallback.
            if (t.title.isNotBlank()) builder.append(t.title)
        }
        else -> {
            // Ignore unknown inline node types.
        }
    }
}

@Composable
fun SafeMarkdown(content: String?, modifier: Modifier = Modifier) {
    // Kontrola na null nebo prázdný string - odstraníme null bytes a ořežeme
    val safeContent = content
        ?.replace("\u0000", "")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return

    val displayMarkdown = remember(safeContent) {
        runCatching { TextUtils.preprocessMarkdownForDisplay(safeContent) }.getOrElse { safeContent }
    }
    
    // Použijeme remember pro zachycení případných chyb během inicializace
    val renderError = remember(safeContent) {
        try {
            // Validace že content neobsahuje problematické hodnoty
            safeContent.length // test že není null
            null
        } catch (e: Exception) {
            android.util.Log.e("SafeMarkdown", "Content validation failed", e)
            e
        }
    }
    
    if (renderError != null) {
        // Fallback na prostý text pokud validace selže
        Text(
            text = "Chyba při načítání obsahu",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        // Bezpečné vykreslení Markdownu s zachycením chyb
        val result = runCatching {
            val parser = remember { CommonmarkAstNodeParser() }
            val astNode = remember(displayMarkdown) { parser.parse(displayMarkdown) }

            val paragraphComposer = remember {
                object : AstBlockNodeComposer {
                    override fun predicate(astBlockNodeType: AstBlockNodeType): Boolean {
                        return astBlockNodeType == AstParagraph
                    }

                    @Composable
                    override fun RichTextScope.Compose(
                        astNode: AstNode,
                        visitChildren: @Composable (AstNode) -> Unit
                    ) {
                        val children = inlineChildren(astNode)

                        // Standalone image paragraph: keep the original block behavior.
                        if (children.size == 1 && children.first().type is AstImage) {
                            val img = children.first().type as AstImage
                            AsyncImage(
                                model = img.destination,
                                contentDescription = img.title.ifBlank { "image" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(androidx.compose.ui.graphics.Color.White)
                            )
                            return
                        }

                        val builder = RichTextString.Builder(children.sumOf { (it.type as? AstText)?.literal?.length ?: 8 })
                        children.forEach { appendInlineNode(builder, it) }
                        val rich = builder.toRichTextString()
                        RichTextScopeText(text = rich)
                    }
                }
            }

            RichText(modifier = modifier) {
                BasicMarkdown(
                    astNode = astNode,
                    astBlockNodeComposer = paragraphComposer
                )
            }
        }
        if (result.isFailure) {
            android.util.Log.e("SafeMarkdown", "Markdown render failed", result.exceptionOrNull())
            // Fallback na prostý text, aby aplikace nespadla
            Text(
                text = safeContent,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier
            )
        } else {
            // Pokud se vykreslení povedlo, nic dalšího neděláme — obsah je zobrazen uvnitř RichText
            result.getOrNull()
        }
    }
}

// Data class pro speciální prvky v obsahu
sealed class ContentElement {
    data class MarkdownText(val text: String) : ContentElement()
    data class HighlightedBlock(val text: String) : ContentElement()
    data class ClickableLink(val text: String, val url: String, val postId: Int?) : ContentElement()
    data class VideoPlayer(val videoUrl: String, val posterUrl: String?) : ContentElement()
    data class AddendumReference(val addendumId: Int) : ContentElement()
}

// Parsuje obsah článku na seznam elementů
fun parseContentToElements(
    content: String,
    isOffline: Boolean,
    posts: List<com.example.tobisoappnative.model.Post>
): List<ContentElement> {
    try {
        // Nejprve zpracujeme obrázky
        val imageRegex = Regex("!\\[(.*?)]\\((images/[^)]+)\\)")
        val processedContent = if (!isOffline) {
            content.replace(imageRegex) {
                val alt = it.groups[1]?.value ?: ""
                val path = it.groups[2]?.value ?: ""
                "![${alt}](https://files.tobiso.com/${path})"
            }
        } else {
            content.replace(imageRegex) {
                val alt = it.groups[1]?.value ?: ""
                "\n\n**[Obrázek: $alt - nedostupný v offline režimu]**\n\n"
            }
        }

        // Najdeme všechny speciální prvky
        val blockRegex = Regex("\\.\\.\\.\\s*([\\s\\S]*?)\\s*\\.\\.\\.")
        val linkRegex = Regex("(?<!!)\\[(.+?)\\]\\((.+?)\\)")
        val videoRegex = Regex("<video[^>]*src=\"([^\"]+)\"[^>]*>(.*?)</video>", RegexOption.DOT_MATCHES_ALL)
        val addendumRegex = Regex("\\(--DOD-(\\d+)--\\)")

        // Najdeme všechny matche
        val allMatches = mutableListOf<Triple<Int, Int, Pair<String, MatchResult>>>()
        
        blockRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "block" to it))
        }
        linkRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "link" to it))
        }
        videoRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "video" to it))
        }
        addendumRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "addendum" to it))
        }
        
        allMatches.sortBy { it.first }

        // Pokud nejsou žádné speciální prvky, vrátíme celý obsah jako jeden markdown element
        if (allMatches.isEmpty()) {
            return listOf(ContentElement.MarkdownText(processedContent))
        }

        // Sestavíme seznam elementů
        val elements = mutableListOf<ContentElement>()
        var lastIndex = 0

        for ((start, end, typeAndMatch) in allMatches) {
            // Přidáme text před aktuálním elementem
            if (start > lastIndex && start <= processedContent.length) {
                try {
                    val textBefore = processedContent.substring(lastIndex, start)
                        .replace("\u0000", "")
                        .trim()
                    if (textBefore.isNotBlank()) {
                        elements.add(ContentElement.MarkdownText(textBefore))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("parseContentToElements", "Error extracting text before element", e)
                }
            }

            // Přidáme speciální element
            when (typeAndMatch.first) {
                "block" -> {
                    val blockText = typeAndMatch.second.groups[1]?.value?.trim() ?: ""
                    if (blockText.isNotBlank()) {
                        elements.add(ContentElement.HighlightedBlock(blockText))
                    }
                }
                "link" -> {
                    val linkText = typeAndMatch.second.groups[1]?.value?.trim() ?: ""
                    val url = typeAndMatch.second.groups[2]?.value?.trim() ?: ""
                    if (linkText.isNotBlank() && url.isNotBlank()) {
                        var filePath = url
                        if (filePath.endsWith(".html")) {
                            filePath = filePath.removeSuffix(".html") + ".md"
                        }
                        filePath = filePath.replace(prefixRegex, "")
                        if (!filePath.startsWith("/")) {
                            filePath = "/$filePath"
                        }
                        val post = posts.find { it.filePath == filePath }
                        elements.add(ContentElement.ClickableLink(linkText, url, post?.id))
                    }
                }
                "video" -> {
                    val videoSrc = typeAndMatch.second.groups[1]?.value ?: ""
                    if (videoSrc.isNotBlank()) {
                        val videoUrl = if (videoSrc.startsWith("http")) videoSrc else "https://tobiso.com/$videoSrc"
                        elements.add(ContentElement.VideoPlayer(videoUrl, null))
                    }
                }
                "addendum" -> {
                    val addendumIdStr = typeAndMatch.second.groups[1]?.value ?: ""
                    val addendumId = addendumIdStr.toIntOrNull()
                    if (addendumId != null) {
                        elements.add(ContentElement.AddendumReference(addendumId))
                    }
                }
            }

            lastIndex = end
        }

        // Přidáme zbývající text na konci
        if (lastIndex < processedContent.length) {
            try {
                val textAfter = processedContent.substring(lastIndex)
                    .replace("\u0000", "")
                    .trim()
                if (textAfter.isNotBlank()) {
                    elements.add(ContentElement.MarkdownText(textAfter))
                }
            } catch (e: Exception) {
                android.util.Log.e("parseContentToElements", "Error extracting text after elements", e)
            }
        }

        return elements
    } catch (e: Exception) {
        android.util.Log.e("parseContentToElements", "Chyba při parsování obsahu", e)
        // V případě chyby vrátíme celý obsah jako prostý text (s kontrolou na null)
        val safeContent = content.replace("\u0000", "").trim()
        return if (safeContent.isNotBlank()) {
            listOf(ContentElement.MarkdownText(safeContent))
        } else {
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    navController: NavController
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
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var hasQuestions by remember { mutableStateOf(false) }
    var hasExercises by remember { mutableStateOf(false) }
    val ttsManager = vm.getTtsManager()
    val addendums by vm.addendums.collectAsState()
    var selectedAddendum by remember { mutableStateOf<Addendum?>(null) }
    var showAddendumDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPdfDownload by rememberSaveable { mutableStateOf(false) }
    
    val context = LocalContext.current

    // Skutečná kontrola připojení (stejně jako v MainActivity)
    var isConnected by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isConnected = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            delay(2000)
        }
    }

    // Funkce pro stažení PDF
    val downloadPdf: (Int) -> Unit = { id ->
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("PostDetailScreen", "Stahování PDF pro post ID: $id")
                val responseBody = vm.downloadPostPdf(id)
                val pdfBytes = responseBody.bytes()
                android.util.Log.d("PostDetailScreen", "PDF staženo, velikost: ${pdfBytes.size} bytes")
                
                val fileName = "tobiso_post_${id}.pdf"
                var pdfUri: android.net.Uri? = null
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Pro Android 10+ použijeme MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    
                    val resolver = context.contentResolver
                    pdfUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    
                    pdfUri?.let { uri ->
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(pdfBytes)
                        }
                        android.util.Log.d("PostDetailScreen", "PDF uloženo do Downloads: $fileName")
                    }
                } else {
                    // Pro starší verze použijeme starý způsob
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val pdfFile = File(downloadsDir, fileName)
                    
                    FileOutputStream(pdfFile).use { output ->
                        output.write(pdfBytes)
                    }
                    pdfUri = android.net.Uri.fromFile(pdfFile)
                    android.util.Log.d("PostDetailScreen", "PDF uloženo: ${pdfFile.absolutePath}")
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (pdfUri != null) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(pdfUri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(
                                context,
                                "PDF uloženo do složky Stažené",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Chyba při ukládání PDF",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PostDetailScreen", "Chyba při generování PDF", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val errorMsg = when {
                        e.message != null -> e.message
                        e.cause?.message != null -> e.cause?.message
                        else -> e.javaClass.simpleName
                    }
                    android.widget.Toast.makeText(
                        context,
                        "Chyba při generování PDF: $errorMsg",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
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
    
    LaunchedEffect(postId) {
        try {
            android.util.Log.d("PostDetailScreen", "LaunchedEffect started for post $postId")
            // Načteme detail (ViewModel má logiku pro offline i online režim)
            vm.loadPostDetail(postId)
            android.util.Log.d("PostDetailScreen", "Post detail loaded")
            
            // Načteme všechny posts pro vyhledávání odkazů a zobrazení souvisejících článků
            if (posts.isEmpty()) {
                vm.loadPosts()
                android.util.Log.d("PostDetailScreen", "Posts loaded")
            }
            // Načteme související články (funguje v online i offline režimu)
            vm.loadRelatedPosts(postId)
            android.util.Log.d("PostDetailScreen", "Related posts loaded")
            
            // Načteme dodatky
            if (addendums.isEmpty()) {
                vm.loadAddendums()
                android.util.Log.d("PostDetailScreen", "Addendums loaded")
            }
            
            // Kontrola otázek pro tento příspěvek (nyní funguje v online i offline režimu)
            hasQuestions = try {
                vm.checkHasQuestions(postId)
            } catch (e: Exception) {
                android.util.Log.e("PostDetailScreen", "Error checking questions", e)
                false
            }
            
            // Kontrola cvičení pro tento příspěvek (pouze v online režimu)
            hasExercises = try {
                val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId ?: postDetail?.categoryId
                vm.checkHasExercises(postId, postCategoryId)
            } catch (e: Exception) {
                android.util.Log.e("PostDetailScreen", "Error checking exercises", e)
                false
            }

            // Přednačteme cvičení (online i offline), aby se tlačítko zobrazilo spolehlivě
            try {
                val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId ?: postDetail?.categoryId
                vm.loadExercisesByPostId(postId, postCategoryId)
            } catch (e: Exception) {
                android.util.Log.e("PostDetailScreen", "Error preloading exercises", e)
            }
            
            loaded = true
            android.util.Log.d("PostDetailScreen", "LaunchedEffect completed for post $postId")
        } catch (e: Exception) {
            android.util.Log.e("PostDetailScreen", "Critical error in LaunchedEffect for post $postId", e)
            loaded = true // Stejně nastavíme, aby se zobrazila chyba místo nekonečného načítání
        }
    }

    LaunchedEffect(postId, postDetail?.categoryId, posts, isOffline) {
        // Post detail a posts se načítají async; pro cvičení navázaná na kategorii potřebujeme znát categoryId.
        val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId ?: postDetail?.categoryId
        if (postCategoryId == null && postDetail?.id != postId) return@LaunchedEffect

        hasExercises = try {
            vm.checkHasExercises(postId, postCategoryId)
        } catch (e: Exception) {
            android.util.Log.e("PostDetailScreen", "Error re-checking exercises", e)
            hasExercises
        }

        // Jakmile známe categoryId, dotáhneme cvičení (kvůli category-based přiřazení)
        try {
            vm.loadExercisesByPostId(postId, postCategoryId)
        } catch (e: Exception) {
            android.util.Log.e("PostDetailScreen", "Error reloading exercises", e)
        }
    }

    LaunchedEffect(exercises) {
        hasExercises = exercises.isNotEmpty()
    }

    // Re-načteme související články jakmile je postDetail dostupný,
    // protože první volání loadRelatedPosts proběhlo před načtením postDetail/posts.
    LaunchedEffect(postDetail?.id) {
        if (postDetail != null && postDetail!!.id == postId) {
            vm.loadRelatedPosts(postId)
        }
    }

    var showFloatingSelectButton by remember { mutableStateOf(false) }
    var aiInputText by remember { mutableStateOf("") }
    var aiInputExpanded by remember { mutableStateOf(false) }
    
    // Scroll behavior pro nested scrolling
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (!isOffline) {
                    isRefreshing = true
                    coroutineScope.launch {
                        vm.loadPostDetail(postId)
                        vm.loadRelatedPosts(postId)
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
                                    vm.getTtsManager().speak(plainText)
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
                                if (isFavorite) vm.unsavePost(it.id) else vm.savePost(it)
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // PRINT PDF BUTTON - pouze v online režimu
                        if (!isOffline) {
                            IconButton(onClick = {
                                postDetail?.id?.let { id ->
                                    // Pro Android 10+ (API 29+) nepotřebujeme permission
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        downloadPdf(id)
                                    } else {
                                        // Pro starší verze Android < 10 zkontrolujeme permission
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        
                                        if (hasPermission) {
                                            downloadPdf(id)
                                        } else {
                                            // Požádáme o permission
                                            showPermissionDialog = true
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = "Stáhnout PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // SHARE BUTTON - třetí vpravo (pouze v online režimu)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Zobrazení počtu slov a času čtení
                                postDetail?.content?.let { contentText ->
                                val wordCountResult = remember(contentText) {
                                    runCatching {
                                        val trimmed = contentText.trim()
                                        if (trimmed.isNotEmpty()) {
                                            val words = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                                            val minutes = Math.ceil(words / 200.0).toInt().coerceAtLeast(1)
                                            "$words slov | ~${minutes} min čtení"
                                        } else null
                                    }.getOrNull()
                                }
                                
                                wordCountResult?.let { infoText ->
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
                            }
                            postDetail?.content?.let { content ->
                                val contentElements = remember(content, isOffline, posts) {
                                    android.util.Log.d("PostDetailScreen", "Parsování článku ID: $postId, délka: ${content.length}")
                                    parseContentToElements(content, isOffline, posts)
                                }

                                // Renderování obsahu podle elementů
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showFloatingSelectButton = true
                                            }
                                        )
                                    }
                                ) {
                                    Column {
                                        contentElements.forEach { element ->
                                            when (element) {
                                                is ContentElement.MarkdownText -> {
                                                    if (element.text.isNotBlank()) {
                                                        SafeMarkdown(element.text)
                                                    }
                                                }
                                                
                                                is ContentElement.HighlightedBlock -> {
                                                    if (element.text.isNotBlank()) {
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
                                                            SafeMarkdown(element.text)
                                                        }
                                                    }
                                                }
                                                
                                                is ContentElement.ClickableLink -> {
                                                    val linkText = element.text.trim()
                                                    if (linkText.isNotBlank()) {
                                                        Text(
                                                            text = linkText,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.primary,
                                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                            ),
                                                            modifier = Modifier.clickable {
                                                                if (element.postId != null) {
                                                                    navController.navigate("postDetail/${element.postId}")
                                                                } else if (!isOffline) {
                                                                    val url = element.url
                                                                    if (url.contains("http") || url.startsWith("files") || url.contains("/files/")) {
                                                                        val fullUrl = if (url.startsWith("http")) {
                                                                            url
                                                                        } else {
                                                                            "https://files.tobiso.com/" + url.removePrefix("/")
                                                                        }
                                                                        val intent = android.content.Intent(
                                                                            android.content.Intent.ACTION_VIEW,
                                                                            android.net.Uri.parse(fullUrl)
                                                                        )
                                                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        try {
                                                                            navController.context.startActivity(intent)
                                                                        } catch (e: Exception) {
                                                                            android.util.Log.e("PostDetailScreen", "Chyba při otevírání odkazu", e)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                                
                                                is ContentElement.VideoPlayer -> {
                                                    if (isOffline) {
                                                        Text(
                                                            text = "*[Video nedostupné v offline režimu]*",
                                                            modifier = Modifier.padding(vertical = 8.dp),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = {
                                                                navController.navigate(
                                                                    "videoPlayer/${Uri.encode(element.videoUrl)}"
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
                                                            Text("Video", color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                                
                                                is ContentElement.AddendumReference -> {
                                                    val addendum = addendums.find { it.id == element.addendumId }
                                                    if (addendum != null) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedAddendum = addendum
                                                                showAddendumDialog = true
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Help,
                                                                contentDescription = "Zobrazit dodatek",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "[Dodatek #${element.addendumId}]",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Placeholder spacer to prevent content being hidden behind sticky bar (AI input bar only)
                            if (loaded) {
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Tlačítka Prověrka a Cvičení — inline v obsahu (pod body, nad souvisejícími články)
                            if (hasExercises || exercisesLoading || exercises.isNotEmpty() || hasQuestions || questions.isNotEmpty() || !exercisesError.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (hasExercises || exercisesLoading || exercises.isNotEmpty()) {
                                        val exerciseLabel: (String) -> String = { type ->
                                            when (type) {
                                                "timeline" -> "Cvičení na časovou osu"
                                                "circuit" -> "Cvičení: obvod"
                                                "drag-drop" -> "Cvičení: přetahování"
                                                "matching" -> "Cvičení: párování"
                                                else -> "Cvičení"
                                            }
                                        }

                                        if (exercisesLoading) {
                                            Button(
                                                onClick = {},
                                                enabled = false,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                            ) { Text("Cvičení…") }
                        } else if (exercises.isEmpty() && hasExercises) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            val postCategoryId = posts.firstOrNull { it.id == postId }?.categoryId
                                                                ?: postDetail?.categoryId
                                                            vm.loadExercisesByPostId(postId, postCategoryId)
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Načítám cvičení…",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("PostDetailScreen", "Error loading exercises", e)
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Chyba při načítání cvičení",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                            ) { Text("Cvičení") }
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                exercises.forEach { ex ->
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    when (ex.type) {
                                                                        "timeline" -> navController.navigate("exerciseTimeline/${ex.id}")
                                                                        "drag-drop" -> navController.navigate("exerciseDragDrop/${ex.id}")
                                                                        "matching" -> navController.navigate("exerciseMatching/${ex.id}")
                                                                        "circuit" -> navController.navigate("exerciseCircuit/${ex.id}")
                                                                        else -> android.widget.Toast.makeText(
                                                                            context,
                                                                            "Nepodporovaný typ cvičení: ${ex.type}",
                                                                            android.widget.Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    android.util.Log.e("PostDetailScreen", "Error opening exercise", e)
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Chyba při otevírání cvičení",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                                    ) {
                                                        val exLabel = ex.title.takeIf { it.isNotBlank() } ?: exerciseLabel(ex.type)
                                                        Text(exLabel)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (hasQuestions || questions.isNotEmpty()) {
                                        Button(
                                            onClick = { navController.navigate("questions/$postId") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Text("Prověrka")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // Související články
                            if (relatedPosts.isNotEmpty()) {
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
                                            val title = posts.find { it.id == relatedPost.relatedPostId }?.title
                                                ?: relatedPost.relatedPostTitle
                                                ?: return@forEach
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
                                                        text = title,
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
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val locale = java.util.Locale("cs", "CZ")
                            // Parse server timestamps as UTC and display them in device's local timezone
                            val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val outputFormatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale).apply {
                                timeZone = TimeZone.getDefault()
                            }
                            val createdFormatted = postDetail?.createdAt?.let { dateString ->
                                try {
                                    val date = inputFormatter.parse(dateString)
                                    date?.let { outputFormatter.format(it) } ?: ""
                                } catch (e: Exception) {
                                    dateString // fallback to raw string
                                }
                            } ?: ""

                            // Compute updatedFormatted as the most recent among lastEdit, lastFix (fallback to createdAt)
                            val candidates = listOfNotNull(postDetail?.lastEdit, postDetail?.lastFix, postDetail?.createdAt)
                            val latest = candidates.mapNotNull { ds ->
                                try {
                                    inputFormatter.parse(ds)
                                } catch (e: Exception) {
                                    null
                                }
                            }.maxOrNull()

                            val updatedFormatted = latest?.let { outputFormatter.format(it) } ?: candidates.firstOrNull() ?: ""
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

        // Sticky bottom action bar — AI + Prověrka + Cvičení
        // Nezobrazujeme bar dokud se nedokončí úvodní načítání (loaded=true) a nemáme internet
        val showActionsBar = loaded && isConnected
        if (showActionsBar) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // AI vstupní řádek
                    val postTitle = postDetail?.title ?: ""
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiInputText,
                            onValueChange = { aiInputText = it; if (!aiInputExpanded) aiInputExpanded = true },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Zeptej se...",
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (aiInputText.isNotBlank()) {{
                                IconButton(onClick = { aiInputText = ""; aiInputExpanded = false }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Smazat")
                                }
                            }} else null,
                            singleLine = !aiInputExpanded,
                            maxLines = if (aiInputExpanded) 4 else 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (aiInputText.isNotBlank()) {
                                        navController.navigate(
                                            "aiChat/$postId/${android.net.Uri.encode(postTitle)}/${android.net.Uri.encode(aiInputText)}"
                                        )
                                        aiInputText = ""
                                        aiInputExpanded = false
                                    }
                                }
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (aiInputText.isNotBlank()) {
                                    navController.navigate(
                                        "aiChat/$postId/${android.net.Uri.encode(postTitle)}/${android.net.Uri.encode(aiInputText)}"
                                    )
                                    aiInputText = ""
                                    aiInputExpanded = false
                                }
                            },
                            enabled = aiInputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Odeslat",
                                tint = if (aiInputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
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
        
        // Dialog pro zobrazení dodatku
        if (showAddendumDialog && selectedAddendum != null) {
            AlertDialog(
                onDismissRequest = { 
                    showAddendumDialog = false
                    selectedAddendum = null
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = (selectedAddendum!!.name ?: "Dodatek").ifBlank { "Dodatek" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { 
                            showAddendumDialog = false
                            selectedAddendum = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Zavřít")
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SafeMarkdown(
                            content = selectedAddendum!!.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        selectedAddendum!!.updatedAt?.let { updatedAt ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val locale = java.util.Locale("cs", "CZ")
                            val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val outputFormatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale).apply {
                                timeZone = TimeZone.getDefault()
                            }
                            val updatedFormatted = try {
                                val date = inputFormatter.parse(updatedAt)
                                date?.let { outputFormatter.format(it) } ?: updatedAt
                            } catch (e: Exception) {
                                updatedAt
                            }
                            
                            Text(
                                text = "Aktualizováno: $updatedFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showAddendumDialog = false
                        selectedAddendum = null
                    }) {
                        Text("Zavřít")
                    }
                }
            )
        }
    }
}

