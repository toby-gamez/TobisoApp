package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
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
    availableGradeIds: Set<Int>? = null,
    onSelect: (Grade) -> Unit,
    onGenerateWithAi: ((Grade) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var pickingAiGrade by remember { mutableStateOf(false) }
    val eligible = if (availableGradeIds == null) grades else grades.filter { it.id in availableGradeIds }
    val missingGrades = if (availableGradeIds == null) emptyList() else grades.filter { it.id !in availableGradeIds }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                if (pickingAiGrade) "Napsat AI pro ročník" else "Verze pro ročník",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (pickingAiGrade) {
                missingGrades.forEach { grade ->
                    ListItem(
                        headlineContent = { Text(grade.name) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable {
                            onGenerateWithAi?.invoke(grade)
                            onDismiss()
                        }
                    )
                }
            } else {
                eligible.forEach { grade ->
                    val active = grade.id == activeGradeId
                    ListItem(
                        headlineContent = { Text(grade.name) },
                        trailingContent = if (active) {
                            { Icon(Icons.Rounded.Check, contentDescription = "Aktivní", tint = MaterialTheme.colorScheme.primary) }
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
                if (onGenerateWithAi != null && missingGrades.isNotEmpty()) {
                    TextButton(
                        onClick = { pickingAiGrade = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) { Text("Napsat pomocí AI pro jiný ročník") }
                }
            }
        }
    }
}
