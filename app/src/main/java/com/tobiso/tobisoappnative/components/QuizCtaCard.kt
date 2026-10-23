package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.utils.pluralCs

/** "Ověř si porozumění" call-to-action shown at the end of an article that has questions. */
@Composable
fun QuizCtaCard(
    questionCount: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val countText = if (questionCount > 0) {
        "$questionCount ${pluralCs(questionCount, "otázka", "otázky", "otázek")}"
    } else "Ověř si porozumění"
    Card(
        onClick = onStart,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(48.dp).background(scheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Quiz, contentDescription = null, tint = scheme.onPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("Vyzkoušet se", style = MaterialTheme.typography.titleMedium, color = scheme.onPrimary)
                Text(countText, fontSize = 13.sp, color = scheme.onPrimary.copy(alpha = 0.85f))
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onPrimary)
        }
    }
}
