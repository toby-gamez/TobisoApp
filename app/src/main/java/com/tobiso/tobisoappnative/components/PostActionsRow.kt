package com.tobiso.tobisoappnative.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.tts.TtsManager
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme

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
    onBack: () -> Unit
) {
    MultiplierIndicator()

    if (postDetail?.content != null) {
        IconButton(onClick = onTts) {
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = "Přečíst článek",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    val isFavorite = favoritePosts.any { it.id == postDetail?.id }
    IconButton(onClick = onToggleFavorite) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
            contentDescription = if (isFavorite) "Odebrat z oblíbených" else "Uložit do oblíbených",
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (!isOffline) {
        IconButton(onClick = onDownloadClick) {
            Icon(
                imageVector = Icons.Filled.Print,
                contentDescription = "Stáhnout PDF",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Sdílet odkaz",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
