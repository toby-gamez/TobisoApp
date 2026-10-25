package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Post

/** Sage-tinted "Článek dne" card with a shortcut to a random article. */
@Composable
fun ArticleOfDayCard(
    post: Post?,
    onOpenPost: (Int) -> Unit,
    onRandom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { post?.let { onOpenPost(it.id) } },
        enabled = post != null,
        modifier = modifier.fillMaxWidth().cozyPress(0.97f).cozyEntrance(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoStories, contentDescription = null)
                Text("Článek dne", style = MaterialTheme.typography.labelLarge)
            }
            if (post != null) {
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onRandom) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null)
                Text("Náhodný článek", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
