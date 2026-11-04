package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.DutyOverview
import com.tobiso.tobisoappnative.model.DutyPerson
import com.tobiso.tobisoappnative.model.DutyWeek
import com.tobiso.tobisoappnative.model.MyDutyPerson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayMonth = DateTimeFormatter.ofPattern("d. M.")
private val dayMonthYear = DateTimeFormatter.ofPattern("d. M. yyyy")

@Composable
private fun DutyWeekText(week: DutyWeek) {
    if (week.pair != null) {
        Text(week.pair.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        if (week.isPenalty) Text("(trest)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text("bez služby", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Calendar banner: duty of this and next week, full width, with a button for the whole schedule. */
@Composable
fun DutyBanner(duty: DutyOverview, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOfNotNull(duty.thisWeek?.let { "Tento týden" to it }, duty.nextWeek?.let { "Příští týden" to it })
    if (rows.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Checklist, contentDescription = null)
                Text("Služba", style = MaterialTheme.typography.titleMedium)
            }
            rows.forEach { (label, week) ->
                Column(Modifier.fillMaxWidth()) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    DutyWeekText(week)
                }
            }
            OutlinedButton(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) { Text("Celý rozpis") }
        }
    }
}

/** Whole duty schedule in a full-width bottom sheet; your own weeks are highlighted, the current week is marked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutyScheduleDialog(weeks: List<DutyWeek>, myPersonId: Int?, onDismiss: () -> Unit) {
    val thisMonday = remember { LocalDate.now().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("Rozpis služeb", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        if (weeks.isEmpty()) {
            Text("Rozpis zatím není sestavený.", modifier = Modifier.padding(20.dp))
            return@ModalBottomSheet
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weeks, key = { it.id }) { w ->
                val monday = w.monday
                val mine = myPersonId != null && w.pair != null && (w.pair.personAId == myPersonId || w.pair.personBId == myPersonId)
                val current = monday == thisMonday
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            mine -> MaterialTheme.colorScheme.primaryContainer
                            current -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            (monday?.let { "${it.format(dayMonth)} – ${it.plusDays(4).format(dayMonthYear)}" } ?: w.weekStart) +
                                if (current) " · tento týden" else "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                        )
                        DutyWeekText(w)
                        if (mine) Text("Tady máš službu ty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/** Profile block: which name on the class duty roster is you (feeds the duty into the calendar). Hidden for an empty roster. */
@Composable
fun DutyPersonCard(duty: MyDutyPerson, busy: Boolean, onPick: (Int?) -> Unit) {
    if (duty.people.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val current: DutyPerson? = duty.people.firstOrNull { it.id == duty.dutyPersonId }
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Moje služba", style = MaterialTheme.typography.titleMedium)
            Text(
                "Vyber své jméno ze seznamu třídy. V kalendáři se ti pak zvýrazní tvoje týdny služby.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                OutlinedButton(onClick = { open = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Jsem: " + (current?.name ?: "Nevybráno"))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text("Nevybráno") }, onClick = { open = false; onPick(null) })
                    duty.people.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { open = false; onPick(p.id) })
                    }
                }
            }
        }
    }
}
