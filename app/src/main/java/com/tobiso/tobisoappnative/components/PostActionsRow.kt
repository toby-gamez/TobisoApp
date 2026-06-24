package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.tts.TtsManager

@Composable
fun PostActionsRow(
    postDetail: Post?,
    favoritePosts: List<Post>,
    isOffline: Boolean,
    ttsManager: TtsManager?,
    onTts: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onAiToolsClick: () -> Unit,
    onTocClick: () -> Unit,
    hasToc: Boolean,
    onBack: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    MultiplierIndicator()

    if (hasToc) {
        IconButton(onClick = onTocClick) {
            Icon(
                imageVector = Icons.Filled.FormatListBulleted,
                contentDescription = "Obsah článku",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Box {
        IconButton(onClick = { if (!isOffline) onAiToolsClick() }) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = if (isOffline) "AI nedostupné (offline)" else "AI nástroje",
                tint = if (isOffline)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.primary
            )
        }
        if (isOffline) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }

    IconButton(onClick = { menuExpanded = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Další možnosti",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
    ) {
        if (postDetail?.activeContent != null) {
            DropdownMenuItem(
                text = { Text("Přečíst článek") },
                leadingIcon = {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                },
                onClick = { menuExpanded = false; onTts() }
            )
        }

        val isFavorite = favoritePosts.any { it.id == postDetail?.id }
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených") },
            leadingIcon = {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = { menuExpanded = false; onToggleFavorite() }
        )

        if (!isOffline) {
            DropdownMenuItem(
                text = { Text("Stáhnout PDF") },
                leadingIcon = {
                    Icon(Icons.Filled.Print, contentDescription = null)
                },
                onClick = { menuExpanded = false; onDownloadClick() }
            )

            DropdownMenuItem(
                text = { Text("Sdílet odkaz") },
                leadingIcon = {
                    Icon(Icons.Filled.Share, contentDescription = null)
                },
                onClick = { menuExpanded = false; onShareClick() }
            )
        }
    }
}
