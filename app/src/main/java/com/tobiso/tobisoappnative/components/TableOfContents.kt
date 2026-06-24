package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TableOfContentsDialog(
    entries: List<TocEntry>,
    activeEntryIndex: Int,
    onEntryClick: (TocEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeEntryIndex) {
        if (activeEntryIndex >= 0) {
            listState.animateScrollToItem(activeEntryIndex.coerceAtMost(entries.lastIndex.coerceAtLeast(0)))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Obsah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    itemsIndexed(entries) { idx, entry ->
                        val isActive = idx == activeEntryIndex
                        val indent = (entry.level - 1) * 16
                        val baseStyle = when (entry.level) {
                            1 -> MaterialTheme.typography.bodyLarge
                            2 -> MaterialTheme.typography.bodyMedium
                            else -> MaterialTheme.typography.bodySmall
                        }
                        val fontWeight = when {
                            isActive -> FontWeight.Bold
                            entry.level == 1 -> FontWeight.SemiBold
                            else -> FontWeight.Normal
                        }
                        val color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            entry.level >= 3 -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = entry.text,
                            style = baseStyle.copy(fontWeight = fontWeight),
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEntryClick(entry); onDismiss() }
                                .padding(start = (20 + indent).dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
