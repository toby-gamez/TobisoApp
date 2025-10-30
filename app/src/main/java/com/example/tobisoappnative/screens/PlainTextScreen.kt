package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.Snippet
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTextScreen(
    postId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val postDetail by viewModel.postDetail.collectAsState()
    val postDetailError by viewModel.postDetailError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    LaunchedEffect(postId) {
        // Ensure detail is loaded (ViewModel handles offline/online)
        viewModel.loadPostDetail(postId)
        isLoading = true
    }

    // Clipboard/snippet states moved here
    val lastHandledClipboard by viewModel.lastHandledClipboard.collectAsState()
    var showCopyDialog by remember { mutableStateOf(false) }
    var copiedText by remember { mutableStateOf("") }
    var showSavedSnackbar by remember { mutableStateOf(false) }
    var showSaveFab by remember { mutableStateOf(false) }

    val content = postDetail?.content ?: ""

    // Use the platform ClipboardManager listener to detect real clipboard changes
    // (more reliable than polling). We still check ViewModel.lastHandledClipboard to
    // avoid prompting repeatedly for the same text.
    val context = LocalContext.current
    val systemClipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    DisposableEffect(systemClipboard, content, lastHandledClipboard) {
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            try {
                val clip = systemClipboard.primaryClip
                val clipboardText = clip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: ""
                // Show dialog for any new clipboard text (while on this screen).
                // We only check that the text is non-blank and wasn't already handled.
                if (clipboardText.isNotBlank() && clipboardText != lastHandledClipboard) {
                    copiedText = clipboardText
                    // show a small FAB letting the user save the copied selection as a snippet
                    showSaveFab = true
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        systemClipboard.addPrimaryClipChangedListener(listener)
        onDispose {
            try {
                systemClipboard.removePrimaryClipChangedListener(listener)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Shared scroll behavior for top bar and content
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val fontSize = (28 - (12 * collapsedFraction)).sp
                    Text(
                        text = "Vybrat text",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        // Apply nestedScroll so the top bar collapses like in PostDetailScreen
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {

            when {
                postDetailError != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Chyba při načítání článku: ${postDetailError}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                postDetail == null || !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Dialog pro uložení útržku
            // Floating button to save the last copied selection as a snippet
            androidx.compose.animation.AnimatedVisibility(
                visible = showSaveFab,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(onClick = {
                        // open confirm dialog
                        showCopyDialog = true
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Uložit útržek")
                    }
                }
            }

            // Dialog pro uložení útržku (opened from FAB)
            if (showCopyDialog) {
                AlertDialog(
                    onDismissRequest = { showCopyDialog = false },
                    title = { Text("Uložit do útržků?") },
                    text = { Text("Uložit právě zkopírovaný text jako útržek?") },
                    confirmButton = {
                        TextButton(onClick = {
                            val actualPostId = postDetail?.id ?: postId
                            val snippet = Snippet(
                                postId = actualPostId,
                                content = copiedText,
                                createdAt = System.currentTimeMillis()
                            )
                            viewModel.addSnippet(snippet)
                            // mark this clipboard text as handled so we won't ask again
                            viewModel.markClipboardHandled(copiedText)
                            showCopyDialog = false
                            showSavedSnackbar = true
                            showSaveFab = false
                        }) {
                            Text("Ano")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // mark this clipboard text as handled (dismissed) so we won't ask again
                            viewModel.markClipboardHandled(copiedText)
                            showCopyDialog = false
                            showSaveFab = false
                        }) {
                            Text("Ne")
                        }
                    }
                )
            }

            // Snackbar po uložení útržku
            if (showSavedSnackbar) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        action = {
                            Row {
                                TextButton(onClick = { showSavedSnackbar = false }) {
                                    Text("Zavřít")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    showSavedSnackbar = false
                                    navController.navigate("favorites")
                                }) {
                                    Text("Zobrazit")
                                }
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Útržek uložen.")
                    }
                }
            }
        }
    }
}
