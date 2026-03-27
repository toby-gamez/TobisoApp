package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    hasQuestions: Boolean,
    onLoadExercises: () -> Unit,
    onOpenExercise: (Int, String?) -> Unit,
    onOpenQuestions: () -> Unit
) {
    if (hasExercises || exercisesLoading || exercises.isNotEmpty() || hasQuestions) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasExercises || exercisesLoading || exercises.isNotEmpty()) {
                if (exercisesLoading) {
                    Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("Cvičení…") }
                } else if (exercises.isEmpty() && hasExercises) {
                    Button(onClick = onLoadExercises, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("Cvičení") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercises.forEach { ex ->
                            Button(onClick = { onOpenExercise(ex.id, ex.type) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                                val exLabel = ex.title?.takeIf { it.isNotBlank() } ?: when (ex.type) {
                                    "timeline" -> "Cvičení na časovou osu"
                                    "circuit" -> "Cvičení: obvod"
                                    "drag-drop" -> "Cvičení: přetahování"
                                    "matching" -> "Cvičení: párování"
                                    else -> "Cvičení"
                                }
                                Text(exLabel)
                            }
                        }
                    }
                }
            }

            if (hasQuestions) {
                Button(onClick = onOpenQuestions, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Prověrka")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
