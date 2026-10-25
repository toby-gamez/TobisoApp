package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.utils.pluralCs
import com.tobiso.tobisoappnative.viewmodel.postdetail.DifficultyState

private val labels = listOf(1 to "Snadné", 2 to "Střední", 3 to "Těžké")

@Composable
fun DifficultyRating(
    state: DifficultyState,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jak těžký je tento článek?", style = MaterialTheme.typography.titleSmall)
            val results = state.results
            if (state.myVote == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { (value, label) ->
                        FilterChip(
                            selected = false,
                            enabled = !state.submitting,
                            onClick = { onRate(value) },
                            label = { Text(label) },
                            shape = CircleShape
                        )
                    }
                }
            } else if (results != null && results.total > 0) {
                val counts = mapOf(1 to results.easy, 2 to results.ok, 3 to results.hard)
                labels.forEach { (value, label) ->
                    val count = counts.getValue(value)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
                        LinearProgressIndicator(
                            progress = { count.toFloat() / results.total },
                            modifier = Modifier.weight(1f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text("${count * 100 / results.total} %", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    }
                }
                Text(
                    "Děkujeme za hodnocení · ${results.total} ${pluralCs(results.total, "hlas", "hlasy", "hlasů")}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Děkujeme za hodnocení", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
