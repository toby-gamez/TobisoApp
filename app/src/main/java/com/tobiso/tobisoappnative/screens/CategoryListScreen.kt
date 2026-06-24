package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostSummaryResponse
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import com.tobiso.tobisoappnative.components.FloatingSearchBar
import com.tobiso.tobisoappnative.components.GradeBadge
import com.tobiso.tobisoappnative.utils.loadGradeId
import com.tobiso.tobisoappnative.viewmodel.categorylist.CategoryListViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CategoryListScreen(
    parentCategoryName: String,
    navController: NavController
) {
    val vm: CategoryListViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val favoritePosts by vm.favoritePosts.collectAsState()
    val categories = state.categories
    val categoryError = state.error
    val categoryLoading = state.isLoading
    val posts = state.posts
    val summaries = state.summaries
    val grades = state.grades
    val postError = state.error
    val postLoading = state.isLoading
    val context = LocalContext.current
    val selectedGradeId = remember { loadGradeId(context) }
    LaunchedEffect(Unit) {
        vm.loadCategories()
        vm.loadGradesAndSummaries()
    }

    val parentCategory = categories.find { it.name == parentCategoryName }
    val filteredCategories = parentCategory?.let { parent ->
        categories.filter { it.parentId == parent.id }
    } ?: emptyList()

    // Načtení postů při změně parentCategory
    LaunchedEffect(parentCategory?.id) {
        parentCategory?.id?.let { vm.loadPosts(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(parentCategoryName, style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            val filteredPosts = parentCategory?.let { parent ->
                posts.filter { it.categoryId == parent.id }
            } ?: emptyList()
            val filteredSummaries = parentCategory?.let { parent ->
                summaries.filter { it.categoryId == parent.id }
            } ?: emptyList()
            val hasNoData = filteredCategories.isEmpty() && filteredPosts.isEmpty() && filteredSummaries.isEmpty()

            when {
                categoryLoading || postLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Načítání obsahu...")
                        }
                    }
                }
                postError != null && hasNoData -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                postError ?: "Neznámá chyba",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                else -> {
                    val displayItems: List<Any> = if (filteredSummaries.isNotEmpty()) filteredSummaries else filteredPosts
                    val hasBoth = filteredCategories.isNotEmpty() && displayItems.isNotEmpty()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (filteredCategories.isNotEmpty()) {
                            if (hasBoth) {
                                item {
                                    Text(
                                        text = "Podkategorie",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                                    )
                                }
                            }
                            items(filteredCategories) { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate(CategoryListRoute(categoryName = category.name)) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (displayItems.isNotEmpty()) {
                            if (hasBoth) {
                                item {
                                    Text(
                                        text = "Články",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
                                    )
                                }
                            }
                            items(displayItems) { item ->
                                val postId = when (item) {
                                    is PostSummaryResponse -> item.id
                                    is Post -> item.id
                                    else -> return@items
                                }
                                val postTitle = when (item) {
                                    is PostSummaryResponse -> item.title
                                    is Post -> (item as Post).title
                                    else -> ""
                                }
                                val lastEdit = when (item) {
                                    is PostSummaryResponse -> item.lastEdit
                                    is Post -> (item as Post).lastEdit
                                    else -> null
                                }
                                val lastFix = when (item) {
                                    is PostSummaryResponse -> item.lastFix
                                    is Post -> (item as Post).lastFix
                                    else -> null
                                }
                                val createdAt = when (item) {
                                    is Post -> (item as Post).createdAt
                                    else -> null
                                }
                                val gradeName = when (item) {
                                    is PostSummaryResponse -> bestMatchGradeName(item.availableGradeNames, selectedGradeId, grades)
                                    is Post -> (item as Post).activeVersion?.gradeName
                                    else -> null
                                }
                                val postForFavorite = when (item) {
                                    is Post -> item as Post
                                    is PostSummaryResponse -> Post(id = item.id, title = item.title, filePath = item.filePath, categoryId = item.categoryId)
                                    else -> null
                                }
                                val locale = java.util.Locale("cs", "CZ")
                                val inputFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                val outputFmt = java.text.SimpleDateFormat("dd. MM. yyyy", locale).apply { timeZone = TimeZone.getDefault() }
                                val formatted = listOfNotNull(lastEdit, lastFix, createdAt)
                                    .mapNotNull { try { inputFmt.parse(it) } catch (_: Exception) { null } }
                                    .maxOrNull()?.let { outputFmt.format(it) } ?: ""

                                val isFavorite = favoritePosts.any { it.id == postId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate(PostDetailRoute(postId = postId)) }
                                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = postTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (formatted.isNotBlank()) {
                                            Text(
                                                text = formatted,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                        if (gradeName != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            GradeBadge(gradeName = gradeName)
                                        }
                                    }
                                    IconButton(onClick = {
                                        if (isFavorite) vm.unsavePost(postId)
                                        else postForFavorite?.let { vm.savePost(it) }
                                    }) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                                            contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun bestMatchGradeName(
    availableGradeNames: List<String>,
    selectedGradeId: Int?,
    grades: List<Grade>
): String? {
    if (availableGradeNames.isEmpty()) return null
    if (selectedGradeId == null || grades.isEmpty()) return availableGradeNames.firstOrNull()
    val selectedLevel = grades.find { it.id == selectedGradeId }?.level ?: return availableGradeNames.firstOrNull()
    val withLevel = availableGradeNames.mapNotNull { name ->
        grades.find { it.name == name }?.let { name to it.level }
    }
    if (withLevel.isEmpty()) return availableGradeNames.firstOrNull()
    return (withLevel.filter { it.second <= selectedLevel }.maxByOrNull { it.second }
        ?: withLevel.maxByOrNull { it.second })?.first
}