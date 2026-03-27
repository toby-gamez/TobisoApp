package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInputBar(
    aiInputText: String,
    onTextChange: (String) -> Unit,
    aiInputExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    postTitle: String,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = aiInputText,
                onValueChange = { onTextChange(it); if (!aiInputExpanded) onExpandedChange(true) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nějaká otázka k článku...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (aiInputText.isNotBlank()) {
                        IconButton(onClick = { onTextChange(""); onExpandedChange(false) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Smazat")
                        }
                    }
                },
                singleLine = !aiInputExpanded,
                maxLines = if (aiInputExpanded) 4 else 1,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { if (aiInputText.isNotBlank()) onSend() }),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            SmallFloatingActionButton(onClick = { if (aiInputText.isNotBlank()) onSend() }, containerColor = if (aiInputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (aiInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat")
            }
        }
    }
}
