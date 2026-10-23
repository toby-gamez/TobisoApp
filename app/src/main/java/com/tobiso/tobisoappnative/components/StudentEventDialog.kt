package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tobiso.tobisoappnative.model.ClassMembershipStatus
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.EventScope
import com.tobiso.tobisoappnative.model.EventType
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.model.StudentEventRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val STUDENT_TYPES = listOf(EventType.Test, EventType.Homework, EventType.Other)
private val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

private fun startOfDay(d: Date): Date = Calendar.getInstance().apply {
    time = d; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.time

private fun endOfDay(d: Date): Date = Calendar.getInstance().apply {
    time = d; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
}.time

/** Builds the request body; the end date is only sent for multi-day events. Exposed for tests. */
fun buildStudentEventRequest(
    title: String,
    description: String,
    start: Date,
    end: Date,
    type: EventType,
    scope: EventScope,
    groupId: Int?
): StudentEventRequest {
    val multiDay = startOfDay(end).after(startOfDay(start))
    return StudentEventRequest(
        title = title.trim(),
        description = description.trim().ifEmpty { null },
        startDate = apiFormat.format(startOfDay(start)),
        endDate = if (multiDay) apiFormat.format(endOfDay(end)) else null,
        type = type.name,
        scope = scope.name,
        groupId = if (scope == EventScope.Group) groupId else null
    )
}

/** Dialog for the signed-in student's own (server-side) events. Class/Group scopes need an approved class. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentEventDialog(
    isVisible: Boolean,
    myClass: MyClass?,
    onDismiss: () -> Unit,
    onSave: (StudentEventRequest) -> Unit,
    initialEvent: Event? = null,
    initialDate: Date? = null
) {
    if (!isVisible) return

    val inClass = myClass?.membershipStatus == ClassMembershipStatus.Approved
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var start by remember { mutableStateOf(initialEvent?.startDate ?: initialDate ?: Date()) }
    var end by remember { mutableStateOf(initialEvent?.endDate ?: initialEvent?.startDate ?: initialDate ?: Date()) }
    var type by remember { mutableStateOf(initialEvent?.eventType?.takeIf { it in STUDENT_TYPES } ?: EventType.Test) }
    var scope by remember {
        mutableStateOf(initialEvent?.eventScope?.takeIf { it != EventScope.Global && (inClass || it == EventScope.Personal) } ?: EventScope.Personal)
    }
    var groupId by remember { mutableStateOf(initialEvent?.groupId ?: myClass?.groups?.firstOrNull()?.id) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d.M.yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (initialEvent != null) "Upravit událost" else "Nová událost",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Zavřít") }
                }
                Spacer(Modifier.height(12.dp))

                Column(Modifier.weight(1f, false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it }, label = { Text("Název") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description, onValueChange = { description = it }, label = { Text("Popis (volitelný)") },
                        maxLines = 3, modifier = Modifier.fillMaxWidth()
                    )

                    Text("Typ", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        STUDENT_TYPES.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                        }
                    }

                    Text("Kdo událost uvidí", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = scope == EventScope.Personal, onClick = { scope = EventScope.Personal }, label = { Text("Jen já") })
                        if (inClass) {
                            FilterChip(selected = scope == EventScope.Class, onClick = { scope = EventScope.Class }, label = { Text("Moje třída") })
                            if (myClass?.groups?.isNotEmpty() == true) {
                                FilterChip(selected = scope == EventScope.Group, onClick = { scope = EventScope.Group }, label = { Text("Skupina") })
                            }
                        }
                    }
                    if (!inClass) {
                        Text(
                            "Události pro třídu nebo skupinu půjdou přidat po schválení členství ve třídě.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (scope == EventScope.Group) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            myClass?.groups?.forEach { g ->
                                FilterChip(selected = groupId == g.id, onClick = { groupId = g.id }, label = { Text(g.name) })
                            }
                        }
                    }

                    Text("Datum", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(dateFormat.format(start))
                        }
                        Text("–")
                        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                            Text(dateFormat.format(end))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Zrušit") }
                    Button(
                        onClick = { onSave(buildStudentEventRequest(title, description, start, end, type, scope, groupId)) },
                        enabled = title.isNotBlank() && (scope != EventScope.Group || groupId != null),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (initialEvent != null) "Uložit" else "Přidat") }
                }
            }
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            initialDate = start,
            onDateSelected = {
                start = it
                if (end.before(start)) end = start
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerDialog(
            initialDate = end,
            onDateSelected = {
                end = it
                if (start.after(end)) start = end
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}
