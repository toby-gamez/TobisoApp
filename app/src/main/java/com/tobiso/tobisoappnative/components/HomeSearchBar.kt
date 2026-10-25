package com.tobiso.tobisoappnative.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.utils.hasAiConsent
import com.tobiso.tobisoappnative.utils.normalizeText
import com.tobiso.tobisoappnative.utils.saveAiConsent
import kotlinx.coroutines.delay

/**
 * Docked search pill at the top of Učivo (replaces FloatingSearchBar).
 * Typing searches categories + articles; the trailing AI button switches to "attach an article,
 * then ask" mode and sends the question to [onAiSend].
 */
@Composable
fun HomeSearchBar(
    navController: NavHostController,
    posts: List<Post>,
    categories: List<Category>,
    isOffline: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onAiSend: (Post, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var aiMode by remember { mutableStateOf(false) }
    var attached by remember { mutableStateOf<Post?>(null) }
    var showConsent by remember { mutableStateOf(false) }
    val lastAttached = remember { mutableStateOf<Post?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(text) {
        delay(400)
        debounced = text.trim()
    }
    LaunchedEffect(aiMode, attached) {
        if (aiMode) {
            delay(100)
            runCatching { focus.requestFocus() }
        }
    }

    val query = normalizeText(debounced)
    val showResults = debounced.isNotBlank() && attached == null
    val foundCategories = if (!showResults || aiMode) emptyList() else
        categories.filter { normalizeText(it.name).contains(query) }.take(5)
    val foundPosts = if (!showResults) emptyList() else {
        val valid: (Post) -> Boolean = { it.categoryId != null && it.categoryId != 42 }
        val titleHits = posts.filter { valid(it) && normalizeText(it.title).contains(query) }
        val contentHits = if (aiMode) emptyList() else posts.filter {
            valid(it) && !normalizeText(it.title).contains(query) && normalizeText(it.content ?: "").contains(query)
        }
        (titleHits + contentHits).distinctBy { it.id }.take(20)
    }

    val barColor by androidx.compose.animation.animateColorAsState(
        if (aiMode) androidx.compose.ui.graphics.lerp(scheme.surfaceContainerHigh, scheme.primaryContainer, 0.55f) else scheme.surfaceContainerHigh,
        androidx.compose.animation.core.tween(350), label = "searchBarColor"
    )
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding).animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(barColor)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CozySwap(aiMode) { ai ->
                Icon(
                    if (ai) Icons.Rounded.AutoAwesome else Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (ai) scheme.primary else scheme.onSurfaceVariant
                )
            }
            Box(Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    androidx.compose.animation.Crossfade(
                        when {
                            aiMode && attached != null -> "Napiš dotaz…"
                            aiMode -> "Připojit článek…"
                            else -> "Hledej článek nebo se zeptej"
                        },
                        animationSpec = androidx.compose.animation.core.tween(300),
                        label = "searchPlaceholder"
                    ) { hint ->
                        Text(
                            hint,
                            fontSize = 16.sp,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = text.isNotEmpty(),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(spring(dampingRatio = 0.5f)),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                IconButton(onClick = { text = "" }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Smazat", tint = scheme.onSurfaceVariant)
                }
            }
            val canSend = aiMode && attached != null && text.isNotBlank()
            val aiDisabled = isOffline && !aiMode
            Box(
                modifier = Modifier
                    .cozyPress(0.88f)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            aiDisabled -> scheme.surfaceVariant
                            aiMode -> scheme.primary
                            else -> scheme.primary
                        }
                    )
                    .clickable(enabled = !aiDisabled) {
                        when {
                            canSend -> {
                                onAiSend(attached!!, text.trim())
                                text = ""
                                attached = null
                                aiMode = false
                            }
                            aiMode -> {
                                aiMode = false; attached = null; text = ""
                            }
                            hasAiConsent(context) -> {
                                aiMode = true; text = ""
                            }
                            else -> showConsent = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                CozySwap(if (canSend) 2 else if (aiMode) 1 else 0) { kind ->
                    Icon(
                        when (kind) {
                            2 -> Icons.Rounded.Send
                            1 -> Icons.Rounded.Search
                            else -> Icons.Rounded.AutoAwesome
                        },
                        contentDescription = when {
                            aiDisabled -> "AI nedostupné (offline)"
                            canSend -> "Odeslat"
                            aiMode -> "Přepnout na vyhledávání"
                            else -> "Zeptat se AI"
                        },
                        tint = if (aiDisabled) scheme.onSurfaceVariant.copy(alpha = 0.4f) else scheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = attached != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(spring(dampingRatio = 0.8f)),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            val post = attached ?: lastAttached.value ?: return@AnimatedVisibility
            lastAttached.value = post
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.AttachFile, null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    post.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = { attached = null }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Clear, "Odpojit článek", modifier = Modifier.size(16.dp))
                }
            }
        }

        if (showResults) {
            Box(Modifier.padding(top = 8.dp)) {
                GroupedList {
                    if (foundCategories.isEmpty() && foundPosts.isEmpty()) {
                        Text(
                            "Nenalezeny žádné výsledky",
                            modifier = Modifier.padding(16.dp),
                            color = scheme.onSurfaceVariant
                        )
                    }
                    foundCategories.forEachIndexed { i, cat ->
                        if (i > 0) GroupedListDivider()
                        GroupedListRow(
                            title = cat.name,
                            subtitle = "Kategorie",
                            onClick = {
                                text = ""
                                navController.navigate(CategoryListRoute(categoryName = cat.name))
                            }
                        )
                    }
                    if (foundCategories.isNotEmpty() && foundPosts.isNotEmpty()) GroupedListDivider()
                    foundPosts.forEachIndexed { i, post ->
                        if (i > 0) GroupedListDivider()
                        GroupedListRow(
                            title = post.title,
                            subtitle = categories.firstOrNull { it.id == post.categoryId }?.name,
                            onClick = {
                                if (aiMode) {
                                    attached = post
                                    text = ""
                                } else {
                                    text = ""
                                    navController.navigate(PostDetailRoute(postId = post.id))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showConsent) {
        AiConsentDialog(
            navController = navController,
            onAccepted = {
                saveAiConsent(context)
                showConsent = false
                aiMode = true
                text = ""
            },
            onDismissed = { showConsent = false }
        )
    }
}
