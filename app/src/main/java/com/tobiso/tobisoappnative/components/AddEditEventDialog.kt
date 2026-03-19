package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tobiso.tobisoappnative.model.Event
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    initialEvent: Event? = null,
    initialDate: Date? = null
) {
    if (!isVisible) return

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    
    // Datum a čas
    var startDate by remember { 
        mutableStateOf(initialEvent?.startDate ?: initialDate ?: Date())
    }
    var endDate by remember { 
        mutableStateOf(initialEvent?.endDate ?: initialDate ?: Date())
    }
    
    // Nastavení času
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    // Barva
    var selectedColor by remember { mutableStateOf(initialEvent?.color ?: "#33d17a") }
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Opakování
    var isRecurring by remember { mutableStateOf(initialEvent?.isRecurring ?: false) }
    var recurrencePattern by remember { mutableStateOf(initialEvent?.recurrencePattern ?: "weekly") }
    var recurrenceEndDate by remember { mutableStateOf(initialEvent?.recurrenceEndDate) }
    var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("d.M.yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialEvent != null) "Upravit událost" else "Nová událost",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Název
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Název události") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    // Popis
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Popis (volitelný)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                    
                    // Místo
                    item {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Místo (volitelné)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    // Celý den toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Celý den",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isAllDay,
                                onCheckedChange = { 
                                    isAllDay = it
                                    if (it) {
                                        // Nastav čas na začátek a konec dne
                                        val startCal = Calendar.getInstance().apply {
                                            time = startDate
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                        }
                                        val endCal = Calendar.getInstance().apply {
                                            time = endDate
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                        }
                                        startDate = startCal.time
                                        endDate = endCal.time
                                    }
                                }
                            )
                        }
                    }
                    
                    // Datum a čas začátku
                    item {
                        Text(
                            text = "Začátek",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Datum
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(dateFormat.format(startDate))
                                }
                            }
                            
                            // Čas (pouze pokud není celý den)
                            if (!isAllDay) {
                                OutlinedCard(
                                    modifier = Modifier.clickable { showStartTimePicker = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(timeFormat.format(startDate))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Datum a čas konce
                    item {
                        Text(
                            text = "Konec",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Datum
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(dateFormat.format(endDate))
                                }
                            }
                            
                            // Čas (pouze pokud není celý den)
                            if (!isAllDay) {
                                OutlinedCard(
                                    modifier = Modifier.clickable { showEndTimePicker = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(timeFormat.format(endDate))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Barva
                    item {
                        Text(
                            text = "Barva",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showColorPicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = try {
                                                Color(android.graphics.Color.parseColor(selectedColor))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Vybrat barvu")
                            }
                        }
                    }
                    
                    // Opakování
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opakování",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it }
                            )
                        }
                    }
                    
                    // Vzor opakování
                    if (isRecurring) {
                        item {
                            val patterns = listOf(
                                "daily" to "Denně",
                                "weekly" to "Týdně", 
                                "monthly" to "Měsíčně",
                                "yearly" to "Ročně"
                            )
                            
                            Text(
                                text = "Vzor opakování",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            LazyColumn(
                                modifier = Modifier.height(120.dp)
                            ) {
                                items(patterns) { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { recurrencePattern = value }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = recurrencePattern == value,
                                            onClick = { recurrencePattern = value }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                }
                            }
                        }
                        
                        // Konec opakování
                        item {
                            Text(
                                text = "Konec opakování (volitelné)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showRecurrenceEndDatePicker = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = recurrenceEndDate?.let { dateFormat.format(it) } ?: "Neurčeno"
                                        )
                                    }
                                }
                                
                                if (recurrenceEndDate != null) {
                                    IconButton(
                                        onClick = { recurrenceEndDate = null }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Zrušit konec")
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tlačítka
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zrušit")
                    }
                    
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val event = Event(
                                    id = initialEvent?.id ?: 0, // ID se nastaví v LocalEventManager
                                    title = title.trim(),
                                    description = if (description.isBlank()) null else description.trim(),
                                    startDate = startDate,
                                    endDate = endDate,
                                    isAllDay = isAllDay,
                                    location = if (location.isBlank()) null else location.trim(),
                                    color = selectedColor,
                                    isRecurring = isRecurring,
                                    recurrencePattern = if (isRecurring) recurrencePattern else null,
                                    recurrenceEndDate = if (isRecurring) recurrenceEndDate else null,
                                    isLocal = true
                                )
                                onSave(event)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (initialEvent != null) "Uložit" else "Vytvořit")
                    }
                }
            }
        }
    }
    
    // Date/Time Pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { 
                startDate = it
                showStartDatePicker = false
                
                // Pokud je konec před začátkem, posuň konec
                if (endDate.before(startDate)) {
                    endDate = startDate
                }
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { 
                endDate = it
                showEndDatePicker = false
                
                // Pokud je konec před začátkem, posuň začátek
                if (startDate.after(endDate)) {
                    startDate = endDate
                }
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
    
    if (showRecurrenceEndDatePicker) {
        DatePickerDialog(
            initialDate = recurrenceEndDate ?: Date(),
            onDateSelected = { 
                recurrenceEndDate = it
                showRecurrenceEndDatePicker = false
            },
            onDismiss = { showRecurrenceEndDatePicker = false }
        )
    }
    
    if (showStartTimePicker) {
        TimePickerDialog(
            initialDate = startDate,
            onTimeSelected = {
                startDate = it
                showStartTimePicker = false
                
                // Pokud je konec před začátkem, posuň konec o hodinu
                if (endDate.before(startDate)) {
                    val cal = Calendar.getInstance()
                    cal.time = startDate
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    endDate = cal.time
                }
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            initialDate = endDate,
            onTimeSelected = {
                endDate = it
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
    
    if (showColorPicker) {
        ColorPickerDialog(
            selectedColor = selectedColor,
            onColorSelected = { 
                selectedColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}