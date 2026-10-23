package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Grade
import androidx.compose.foundation.clickable

/** Bottom sheet to pick which grade version of an article to read. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradePickerSheet(
    grades: List<Grade>,
    activeGradeId: Int?,
    onSelect: (Grade) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Verze pro ročník",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            grades.forEach { grade ->
                val active = grade.id == activeGradeId
                ListItem(
                    headlineContent = { Text(grade.name) },
                    trailingContent = if (active) {
                        { Icon(Icons.Default.Check, contentDescription = "Aktivní", tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    colors = ListItemDefaults.colors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.clickable {
                        onSelect(grade)
                        onDismiss()
                    }
                )
            }
        }
    }
}
