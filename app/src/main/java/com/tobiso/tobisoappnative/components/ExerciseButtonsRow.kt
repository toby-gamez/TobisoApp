package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse

@Composable
fun ExerciseButtonsRow(
    hasExercises: Boolean,
    exercisesLoading: Boolean,
    exercises: List<InteractiveExerciseResponse>,
    onLoadExercises: () -> Unit,
    onOpenExercise: (Int, String?) -> Unit
) {
    if (hasExercises || exercisesLoading || exercises.isNotEmpty()) {
        SectionLabel("Interaktivní cvičení", Modifier.padding(start = 0.dp, end = 0.dp, top = 22.dp, bottom = 8.dp))
        if (exercisesLoading) {
            ExerciseTile(Icons.Rounded.Extension, "Cvičení…", "Načítám", Modifier.fillMaxWidth(), enabled = false) {}
        } else if (exercises.isEmpty() && hasExercises) {
            ExerciseTile(Icons.Rounded.Extension, "Cvičení", "Klepni pro načtení", Modifier.fillMaxWidth(), onClick = onLoadExercises)
        } else {
            exercises.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { ex ->
                        val (icon, typeLabel) = when (ex.type) {
                            "timeline" -> Icons.Rounded.Timeline to "Časová osa"
                            "circuit" -> Icons.Rounded.ElectricalServices to "Obvod"
                            "drag-drop" -> Icons.Rounded.DragIndicator to "Přetahování"
                            "matching" -> Icons.Rounded.Link to "Přiřazování"
                            else -> Icons.Rounded.Extension to "Cvičení"
                        }
                        ExerciseTile(
                            icon, typeLabel, ex.title?.takeIf { it.isNotBlank() } ?: "",
                            Modifier.weight(1f)
                        ) { onOpenExercise(ex.id, ex.type) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ExerciseTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}
