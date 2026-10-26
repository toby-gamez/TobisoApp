package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.cozyEntrance

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import com.tobiso.tobisoappnative.components.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.sp
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
    val state by vm.state.collectAsStateWithLifecycle()
    val favoritePosts by vm.favoritePosts.collectAsStateWithLifecycle()
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(com.tobiso.tobisoappnative.navigation.HomeRoute) {
                            popUpTo(com.tobiso.tobisoappnative.navigation.HomeRoute) { inclusive = true }
                        }
                    }) { Icon(Icons.Rounded.Search, contentDescription = "Hledat", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
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
                                imageVector = Icons.Rounded.Description,
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
                    val allItems: List<Any> = if (filteredSummaries.isNotEmpty()) filteredSummaries else filteredPosts
                    fun titleOf(item: Any): String = when (item) {
                        is PostSummaryResponse -> item.title
                        is Post -> item.title
                        else -> ""
                    }
                    // Příspěvek se stejným názvem jako kategorie slouží jako její „Úvod“
                    val introItem = allItems.firstOrNull { titleOf(it).trim().equals(parentCategoryName.trim(), ignoreCase = true) }
                    val introId = when (introItem) {
                        is PostSummaryResponse -> introItem.id
                        is Post -> introItem.id
                        else -> null
                    }
                    val displayItems: List<Any> = if (introItem != null) allItems.filter { it !== introItem } else allItems
                    val hasBoth = filteredCategories.isNotEmpty() && displayItems.isNotEmpty()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            val parentOfParent = parentCategory?.parentId?.let { pid -> categories.find { it.id == pid }?.name }
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                                Text(
                                    text = parentOfParent ?: "Učivo",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = parentCategoryName,
                                    fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 30.sp,
                                    lineHeight = 34.sp
                                )
                                Text(
                                    text = "${displayItems.size} článků",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (introId != null) {
                            item {
                                GroupedList(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    GroupedListRow(
                                        title = "Úvod",
                                        leading = {
                                            IconCircle(
                                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                                size = 36.dp,
                                                background = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = { navController.navigate(PostDetailRoute(postId = introId)) }
                                    )
                                }
                            }
                        }
                        if (filteredCategories.isNotEmpty()) {
                            item { SectionLabel("Oblasti") }
                            if (displayItems.isEmpty()) item {
                                GroupedList(Modifier.padding(horizontal = 16.dp)) {
                                    filteredCategories.forEachIndexed { index, category ->
                                        if (index > 0) GroupedListDivider()
                                        GroupedListRow(
                                            title = category.name,
                                            leading = {
                                                IconCircle(
                                                    icon = Icons.Rounded.Folder,
                                                    size = 36.dp,
                                                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = { navController.navigate(CategoryListRoute(categoryName = category.name)) }
                                        )
                                    }
                                }
                            } else item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredCategories) { category ->
                                        Card(
                                            onClick = { navController.navigate(CategoryListRoute(categoryName = category.name)) },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                            elevation = CardDefaults.cardElevation(0.dp),
                                            modifier = Modifier.widthIn(min = 118.dp).cozyEntrance(category.name)
                                        ) {
                                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                                Icon(
                                                    Icons.Rounded.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = category.name,
                                                    fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 17.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val n = summaries.count { it.categoryId == category.id }
                                                if (n > 0) {
                                                    Text(
                                                        "$n ${com.tobiso.tobisoappnative.utils.pluralCs(n, "článek", "články", "článků")}",
                                                        fontSize = 12.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (displayItems.isNotEmpty()) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Články v „$parentCategoryName“", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text("Podle osnovy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            item {
                                GroupedList(Modifier.padding(horizontal = 16.dp)) {
                                    displayItems.forEachIndexed { index, item ->
                                        val postId = when (item) {
                                            is PostSummaryResponse -> item.id
                                            is Post -> item.id
                                            else -> return@forEachIndexed
                                        }
                                        val postTitle = when (item) {
                                            is PostSummaryResponse -> item.title
                                            is Post -> item.title
                                            else -> ""
                                        }
                                        val lastEdit = (item as? PostSummaryResponse)?.lastEdit ?: (item as? Post)?.lastEdit
                                        val lastFix = (item as? PostSummaryResponse)?.lastFix ?: (item as? Post)?.lastFix
                                        val createdAt = (item as? Post)?.createdAt
                                        val gradeName = when (item) {
                                            is PostSummaryResponse -> bestMatchGradeName(item.availableGradeNames, selectedGradeId, grades)
                                            is Post -> item.activeVersion?.gradeName
                                            else -> null
                                        }
                                        val postForFavorite = when (item) {
                                            is Post -> item
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

                                        if (index > 0) GroupedListDivider()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate(PostDetailRoute(postId = postId)) }
                                                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconCircle(
                                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                                size = 36.dp,
                                                background = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = postTitle,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (gradeName != null) GradeBadge(gradeName = gradeName)
                                                    if (formatted.isNotBlank()) {
                                                        Text(
                                                            text = formatted,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (isFavorite) vm.unsavePost(postId)
                                                    else postForFavorite?.let { vm.savePost(it) }
                                                },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                                    contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
                                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(24.dp)
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