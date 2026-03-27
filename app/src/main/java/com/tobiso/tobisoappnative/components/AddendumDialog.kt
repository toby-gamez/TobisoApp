package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import com.tobiso.tobisoappnative.model.Addendum
import java.text.SimpleDateFormat
import java.util.TimeZone

@Composable
fun AddendumDialog(addendum: Addendum?, onDismiss: () -> Unit) {
    if (addendum == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = (addendum.name ?: "Dodatek").ifBlank { "Dodatek" })
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                SafeMarkdown(content = addendum.content ?: "", modifier = Modifier.fillMaxWidth())
                addendum.updatedAt?.let { updatedAt ->
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    val locale = java.util.Locale("cs", "CZ")
                    val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    val outputFormatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale).apply { timeZone = TimeZone.getDefault() }
                    val updatedFormatted = try {
                        val date = inputFormatter.parse(updatedAt)
                        date?.let { outputFormatter.format(it) } ?: updatedAt
                    } catch (e: Exception) { updatedAt }
                    Text(text = "Aktualizováno: $updatedFormatted", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zavřít") }
        }
    )
}
