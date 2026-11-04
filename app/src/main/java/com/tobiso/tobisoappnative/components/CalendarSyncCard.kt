package com.tobiso.tobisoappnative.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tobiso.tobisoappnative.viewmodel.profile.CalendarSyncViewModel

private val feedTypes = listOf("Test" to "Testy", "Homework" to "Úkoly", "DayOff" to "Volno", "Other" to "Jiné")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarSyncCard(modifier: Modifier = Modifier) {
    val vm: CalendarSyncViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sync = state.sync

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kalendář v telefonu", style = MaterialTheme.typography.titleMedium)
            Text(
                "Měj testy, úkoly, volna a službu své třídy přímo v kalendáři v telefonu. Události se doplňují samy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (sync == null) {
                Button(onClick = vm::show, enabled = !state.busy) { Text("Zobrazit odkaz") }
            } else {
                Text("Co v kalendáři chci", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    feedTypes.forEach { (type, label) ->
                        FilterChip(selected = type in sync.types, onClick = { vm.toggleType(type) }, enabled = !state.busy, label = { Text(label) })
                    }
                    FilterChip(selected = sync.duty, onClick = vm::toggleDuty, enabled = !state.busy, label = { Text("Služba") })
                }
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sync.webcalUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Žádná aplikace kalendáře odběr nepřijala. Zkopíruj odkaz.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Přidat do kalendáře") }
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Tobiso kalendář", sync.url))
                        Toast.makeText(context, "Odkaz zkopírován.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kopírovat odkaz") }
                Text(
                    "Odkaz je tvé osobní tajemství, nesdílej ho. Když unikne, vytvoř nový – starý přestane fungovat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = vm::regenerate, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Vytvořit nový odkaz") }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
