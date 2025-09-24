package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tobisoappnative.model.Event
import com.example.tobisoappnative.viewmodel.CalendarViewModel
import com.example.tobisoappnative.components.AddEditEventDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    navController: NavHostController,
    viewModel: CalendarViewModel = viewModel()
) {
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Stavy pro editaci a mazání
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.loadEventDetail(eventId) { result ->
            event = result
            isLoading = false
            if (result == null) {
                error = "Nepodařilo se načíst detail události"
            }
        }
    }
    
    // LaunchedEffect pro navigaci zpět po smazání
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail události") },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Jednoduchá navigace zpět na předchozí obrazovku
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    // Tlačítka pro editaci a mazání pouze u místních eventů
                    event?.let { currentEvent ->
                        if (currentEvent.isLocalSafe()) {
                            // Tlačítko pro editaci
                            IconButton(
                                onClick = { showEditDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Upravit událost",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Tlačítko pro mazání
                            IconButton(
                                onClick = { 
                                    android.util.Log.d("EventDetailScreen", "Delete button clicked, showing dialog")
                                    showDeleteDialog = true 
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Smazat událost",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                event != null -> {
                    EventDetailContent(event = event!!)
                }
            }
        }
    }
    
    // Dialog pro editaci události
    event?.let { currentEvent ->
        if (currentEvent.isLocalSafe()) {
            AddEditEventDialog(
                isVisible = showEditDialog,
                onDismiss = { showEditDialog = false },
                onSave = { updatedEvent ->
                    viewModel.updateLocalEvent(updatedEvent) { result ->
                        if (result != null) {
                            event = result
                            showEditDialog = false
                            // Refresh eventi pro aktualizaci kalendáře
                            val eventCalendar = Calendar.getInstance().apply { 
                                time = result.getStartDateSafe() 
                            }
                            viewModel.loadEventsForMonth(
                                eventCalendar.get(Calendar.YEAR),
                                eventCalendar.get(Calendar.MONTH)
                            )
                        }
                    }
                },
                initialEvent = currentEvent
            )
        }
    }
    
    // Dialog pro potvrzení smazání
    if (showDeleteDialog) {
        android.util.Log.d("EventDetailScreen", "Showing delete dialog")
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat událost") },
            text = { Text("Opravdu chcete smazat tuto událost? Tato akce je nevratná.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteLocalEvent(eventId) { success ->
                            android.util.Log.d("EventDetailScreen", "Delete result: $success for eventId: $eventId")
                            if (success) {
                                showDeleteDialog = false
                                shouldNavigateBack = true
                            } else {
                                android.util.Log.e("EventDetailScreen", "Failed to delete event $eventId")
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Smazat")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
fun EventDetailContent(event: Event) {
    val scrollState = rememberScrollState()
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale("cs", "CZ"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("d. MMMM yyyy 'v' HH:mm", Locale("cs", "CZ"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hlavní karta s názvem a časem
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    // Barevný indikátor
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = try {
                                    Color(android.graphics.Color.parseColor(event.getColorSafe()))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape
                            )
                            .padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.getTitleSafe(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Datum a čas
                        if (event.isAllDaySafe()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Datum",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Celý den - ${dateFormat.format(event.getStartDateSafe())}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Čas",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Od: ${dateTimeFormat.format(event.getStartDateSafe())}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Do: ${dateTimeFormat.format(event.getEndDateSafe())}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Popis události
        if (!event.description.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Popis",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Popis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }

        // Místo události
        if (!event.location.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Místo",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Místo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Opakování události
        if (event.isRecurringSafe() && !event.recurrencePattern.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Opakování",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opakování",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Opakuje se ${event.getRecurrencePatternCzech()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (event.recurrenceEndDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Do: ${dateFormat.format(event.recurrenceEndDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Další informace
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Další informace",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Další informace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ID události:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#${event.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Typ události:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (event.isAllDaySafe()) "Celodenní" else "Časově vymezená",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Zdroj:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (event.isLocalSafe()) Icons.Default.Smartphone else Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (event.isLocalSafe()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (event.isLocalSafe()) "Místní událost" else "Vzdálená událost",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (event.isLocalSafe()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}