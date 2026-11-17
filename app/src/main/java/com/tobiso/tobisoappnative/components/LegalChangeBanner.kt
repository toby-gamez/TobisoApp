package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val LEGAL_DOCUMENTS_URL = "https://www.tobiso.com/pravni-dokumenty"

private val czechDate = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("cs", "CZ"))

/** Formats the server's change timestamp (e.g. `2026-11-01T08:00:00Z`) as "1. listopadu 2026"; null if unparseable. */
fun formatLegalChangeDate(iso: String): String? =
    runCatching { LocalDate.parse(iso.take(10)).format(czechDate) }.getOrNull()

/** Tells a signed-in user that the Terms of Use or Privacy Policy changed (mirrors the web's LegalChangeBanner). */
@Composable
fun LegalChangeBanner(changedAt: String, onRead: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val date = formatLegalChangeDate(changedAt)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Změna právních dokumentů", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                "Změnili jsme Podmínky použití nebo Zásady ochrany osobních údajů" +
                    (date?.let { " ($it)" } ?: "") + ". Doporučujeme si je přečíst.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRead) { Text("Přečíst") }
                OutlinedButton(onClick = onDismiss) { Text("Zavřít") }
            }
        }
    }
}
