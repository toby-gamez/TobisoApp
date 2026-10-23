package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.RelatedPost
import com.tobiso.tobisoappnative.model.Post
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.navigation.PostDetailRoute

@Composable
fun RelatedPostsList(
    relatedPosts: List<RelatedPost>,
    posts: List<Post>,
    navController: NavController
) {
    if (relatedPosts.isNotEmpty()) {
        SectionLabel("Souvisí s tímto", Modifier.padding(start = 0.dp, end = 0.dp, top = 22.dp, bottom = 8.dp))
        GroupedList {
            var first = true
            relatedPosts.forEach { relatedPost ->
                val title = posts.find { it.id == relatedPost.relatedPostId }?.title
                    ?: relatedPost.relatedPostTitle
                    ?: return@forEach
                if (!first) GroupedListDivider()
                first = false
                GroupedListRow(
                    title = title,
                    leading = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.secondary) },
                    onClick = { navController.navigate(PostDetailRoute(postId = relatedPost.relatedPostId)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
