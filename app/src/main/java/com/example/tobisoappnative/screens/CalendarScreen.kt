package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tobisoappnative.model.Event
import com.example.tobisoappnative.viewmodel.CalendarViewModel
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.AddEditEventDialog
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

// Enum pro filtrování událostí
enum class EventFilter(val displayName: String) {
    ALL("Vše"),
    LOCAL("Místní"),
    REMOTE("Vzdálené")
}

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakCalendar(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
    val streakDays = sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
    
    if (streakDays.isEmpty()) return 0
    
    val sortedDates = streakDays.map { LocalDate.parse(it) }.sorted()
    if (sortedDates.size == 1) return 1
    
    var currentStreak = 0
    val today = LocalDate.now()
    val lastRecordedDay = sortedDates.last()
    
    if (lastRecordedDay == today || lastRecordedDay == today.minusDays(1)) {
        var expectedDate = lastRecordedDay
        for (i in sortedDates.indices.reversed()) {
            if (sortedDates[i] == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
    }
    
    return currentStreak
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController? = null,
    viewModel: CalendarViewModel = viewModel(),
    initialYear: Int? = null,
    initialMonth: Int? = null,
    mainViewModel: com.example.tobisoappnative.viewmodel.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val events by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateEvents by viewModel.selectedDateEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOfflineMode by mainViewModel.isOffline.collectAsState()

    var currentMonth by remember { 
        mutableStateOf(initialMonth ?: Calendar.getInstance().get(Calendar.MONTH)) 
    }
    var currentYear by remember { 
        mutableStateOf(initialYear ?: Calendar.getInstance().get(Calendar.YEAR)) 
    }
    var showDateDetail by remember { mutableStateOf(false) }
    
    // States pro TopAppBar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val totalPoints by PointsManager.totalPoints.collectAsState()
    val context = LocalContext.current

    // Stavy pro dialog přidání/úpravy eventu
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDateForNewEvent by remember { mutableStateOf<Date?>(null) }
    
    // Stavy pro dialog smazání
    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    
    // State pro filtrování událostí
    var currentFilter by remember { mutableStateOf(EventFilter.ALL) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(currentMonth, currentYear) {
        viewModel.loadEventsForMonth(currentYear, currentMonth)
    }

    // Hlavní Box pro celou obrazovku
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // LargeTopAppBar s akcemi
            LargeTopAppBar(
                title = { Text("Kalendář", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    // Filter dropdown - přesunut doleva
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showFilterDropdown = true }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtrovat události",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentFilter.displayName,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showFilterDropdown,
                            onDismissRequest = { showFilterDropdown = false }
                        ) {
                            EventFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(filter.displayName)
                                            if (filter == currentFilter) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Vybráno",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        currentFilter = filter
                                        showFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                    val points = remember { mutableStateOf(PointsManager.getPoints()) }

                    LaunchedEffect(Unit) {
                        PointsManager.totalPoints.collect { total ->
                            points.value = total
                        }
                    }
                    
                    // Body button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .background(
                                color = tertiaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { showTotalOverlay = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = points.value.toString(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Streak button s počtem dní
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreakCalendar(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController?.navigate("streak") }
                    ) {
                        if (currentStreak.value > 0) {
                            Text(
                                text = currentStreak.value.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
            
            // Obsah kalendáře
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header s navigací měsíců
                CalendarHeader(
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    onPreviousMonth = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    },
                    onNextMonth = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Načítání událostí...")
                        }
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = error ?: "Neznámá chyba",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    // Kalendářní mřížka
                    CalendarGrid(
                        currentMonth = currentMonth,
                        currentYear = currentYear,
                        viewModel = viewModel,
                        eventFilter = currentFilter,
                        onDateClick = { date ->
                            viewModel.selectDate(date)
                            showDateDetail = true
                        },
                        onDateLongClick = { date ->
                            selectedDateForNewEvent = date
                            editingEvent = null
                            showAddEventDialog = true
                        }
                    )
                }

                // Detail vybraného dne
                if (showDateDetail && selectedDate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Filtruj události podle aktuálního filtru
                    val filteredEvents = selectedDateEvents.filter { event ->
                        when (currentFilter) {
                            EventFilter.ALL -> true
                            EventFilter.LOCAL -> event.isLocalSafe()
                            EventFilter.REMOTE -> !event.isLocalSafe()
                        }
                    }
                    
                    DateDetailCard(
                        date = selectedDate!!,
                        events = filteredEvents,
                        navController = navController,
                        onClose = {
                            showDateDetail = false
                            viewModel.clearSelectedDate()
                        },
                        onEditEvent = { event ->
                            editingEvent = event
                            selectedDateForNewEvent = null
                            showAddEventDialog = true
                        },
                        onDeleteEvent = { eventId ->
                            // Najdi event pro zobrazení v dialogu
                            val event = selectedDateEvents.find { it.id == eventId }
                            if (event != null) {
                                eventToDelete = event
                                showDeleteDialog = true
                            }
                        }
                    )
                }
                if (isOfflineMode) {
                    Text("Zobrazují se zde pouze místní události, jsi totiž v offline režimu.")
                }
                else {
                    Text("Kalendář dnů, kdy je ve škole volno.")
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { 
                editingEvent = null
                selectedDateForNewEvent = Calendar.getInstance().time
                showAddEventDialog = true 
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Přidat událost",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        // Overlay pro celkové body
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }
    }
    
    // Dialog pro přidání/úpravu eventu
    AddEditEventDialog(
        isVisible = showAddEventDialog,
        onDismiss = { 
            showAddEventDialog = false
            editingEvent = null
            selectedDateForNewEvent = null
        },
        onSave = { event ->
            if (editingEvent != null) {
                // Úprava existujícího eventu
                viewModel.updateLocalEvent(event) { result ->
                    if (result != null) {
                        showAddEventDialog = false
                        editingEvent = null
                        // Refresh celého měsíce pro aktualizaci kalendáře
                        viewModel.loadEventsForMonth(currentYear, currentMonth)
                        // Aktualizuj vybraný den pokud je některý vybraný
                        selectedDate?.let { date ->
                            viewModel.selectDate(date)
                        }
                    }
                }
            } else {
                // Přidání nového eventu
                viewModel.addLocalEvent(event) { result ->
                    if (result != null) {
                        showAddEventDialog = false
                        selectedDateForNewEvent = null
                        // Refresh celého měsíce pro aktualizaci kalendáře
                        viewModel.loadEventsForMonth(currentYear, currentMonth)
                    }
                }
            }
        },
        initialEvent = editingEvent,
        initialDate = selectedDateForNewEvent
    )
    
    // Dialog pro potvrzení smazání
    if (showDeleteDialog && eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                eventToDelete = null
            },
            title = { Text("Smazat událost") },
            text = { 
                Text("Opravdu chcete smazat událost \"${eventToDelete!!.getTitleSafe()}\"? Tato akce je nevratná.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteLocalEvent(eventToDelete!!.id) { success ->
                            isDeleting = false
                            if (success) {
                                showDeleteDialog = false
                                eventToDelete = null
                                // Refresh celého měsíce pro aktualizaci kalendáře
                                viewModel.loadEventsForMonth(currentYear, currentMonth)
                                // Aktualizuj seznam eventů pro vybraný den
                                selectedDate?.let { date ->
                                    viewModel.selectDate(date)
                                }
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
                    onClick = { 
                        showDeleteDialog = false
                        eventToDelete = null
                    },
                    enabled = !isDeleting
                ) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: Int,
    currentYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = arrayOf(
        "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
        "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Předchozí měsíc")
        }

        Text(
            text = "${monthNames[currentMonth]} $currentYear",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Další měsíc")
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Int,
    currentYear: Int,
    viewModel: CalendarViewModel = viewModel(),
    eventFilter: EventFilter = EventFilter.ALL,
    onDateClick: (Date) -> Unit,
    onDateLongClick: (Date) -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayNames = arrayOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    
    Column {
        // Hlavička s názvy dnů
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { dayName ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kalendářní dny
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        
        // Najdi první den měsíce a jeho den v týdnu
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        val adjustedFirstDay = if (firstDayOfMonth == Calendar.SUNDAY) 7 else firstDayOfMonth - 1
        
        // Počet dnů v měsíci
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Vytvoř mřížku
        val totalCells = ((adjustedFirstDay - 1 + daysInMonth) / 7 + 1) * 7
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalCells) { index ->
                val dayNumber = index - adjustedFirstDay + 2
                
                if (dayNumber in 1..daysInMonth) {
                    val dayCalendar = Calendar.getInstance()
                    dayCalendar.set(currentYear, currentMonth, dayNumber)
                    val dayDate = dayCalendar.time
                    
                    val allDayEvents = viewModel.getEventsForDay(currentYear, currentMonth, dayNumber)
                    
                    // Filtruj události podle aktuálního filtru
                    val dayEvents = allDayEvents.filter { event ->
                        when (eventFilter) {
                            EventFilter.ALL -> true
                            EventFilter.LOCAL -> event.isLocalSafe()
                            EventFilter.REMOTE -> !event.isLocalSafe()
                        }
                    }
                    
                    // Zkontroluj, jestli je tento den vybraný
                    val isSelected = selectedDate?.let { selected ->
                        val selectedCalendar = Calendar.getInstance().apply { time = selected }
                        val dayCalendarForCheck = Calendar.getInstance().apply { time = dayDate }
                        
                        selectedCalendar.get(Calendar.YEAR) == dayCalendarForCheck.get(Calendar.YEAR) &&
                        selectedCalendar.get(Calendar.MONTH) == dayCalendarForCheck.get(Calendar.MONTH) &&
                        selectedCalendar.get(Calendar.DAY_OF_MONTH) == dayCalendarForCheck.get(Calendar.DAY_OF_MONTH)
                    } ?: false
                    
                    // Debug pro všechny dny - ne jen prosinec
                    android.util.Log.d("CalendarGrid", "Day $dayNumber/$currentMonth: Found ${dayEvents.size} events")
                    dayEvents.forEach { event ->
                        android.util.Log.d("CalendarGrid", "  - ${event.getTitleSafe()} (AllDay: ${event.isAllDaySafe()})")
                    }
                    
                    CalendarDay(
                        day = dayNumber,
                        hasEvents = dayEvents.isNotEmpty(),
                        eventCount = dayEvents.size,
                        isToday = isToday(dayDate),
                        isSelected = isSelected,
                        onClick = { onDateClick(dayDate) },
                        onLongClick = { onDateLongClick(dayDate) }
                    )
                } else {
                    // Prázdná buňka
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    hasEvents: Boolean,
    eventCount: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary
                    hasEvents -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary 
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    hasEvents -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            if (hasEvents) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Ukažeme až 3 tečky pro eventy
                    repeat(minOf(eventCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary 
                                           else MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                        if (it < minOf(eventCount, 3) - 1) {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    
                    // Pokud je více než 3 eventy, ukážeme "+2" atd.
                    if (eventCount > 3) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "+${eventCount - 3}",
                            fontSize = 8.sp,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary 
                                   else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateDetailCard(
    date: Date,
    events: List<Event>,
    navController: NavHostController? = null,
    onClose: () -> Unit,
    onEditEvent: (Event) -> Unit = {},
    onDeleteEvent: (Int) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.forLanguageTag("cs-CZ"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Zavřít")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (events.isEmpty()) {
                Text(
                    text = "Žádné události pro tento den",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Události (${events.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    events.forEach { event ->
                        EventItem(
                            event = event,
                            timeFormat = timeFormat,
                            onClick = {
                                navController?.navigate("eventDetail/${event.id}")
                            },
                            onEdit = if (event.isLocalSafe()) { { onEditEvent(event) } } else null,
                            onDelete = if (event.isLocalSafe()) { { onDeleteEvent(event.id) } } else null
                        )
                        
                        if (event != events.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(
    event: Event,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Barevný indikátor
                Box(
                    modifier = Modifier
                        .size(12.dp)
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
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.getTitleSafe(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (!event.isAllDaySafe()) {
                        val startDate = event.getStartDateSafe()
                        val endDate = event.getEndDateSafe()
                        val startCalendar = Calendar.getInstance().apply { time = startDate }
                        val endCalendar = Calendar.getInstance().apply { time = endDate }
                        
                        // Zkontroluj, jestli je událost vícedenní
                        val isMultiDay = startCalendar.get(Calendar.DAY_OF_YEAR) != endCalendar.get(Calendar.DAY_OF_YEAR) ||
                                        startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)
                        
                        if (isMultiDay) {
                            Text(
                                text = "Vícedenní událost",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${SimpleDateFormat("d.M.", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("d.M.", Locale.getDefault()).format(endDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val startDate = event.getStartDateSafe()
                        val endDate = event.getEndDateSafe()
                        val startCalendar = Calendar.getInstance().apply { time = startDate }
                        val endCalendar = Calendar.getInstance().apply { time = endDate }
                        
                        // Zkontroluj, jestli je celodenní událost vícedenní
                        val isMultiDay = startCalendar.get(Calendar.DAY_OF_YEAR) != endCalendar.get(Calendar.DAY_OF_YEAR) ||
                                        startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)
                        
                        if (isMultiDay) {
                            Text(
                                text = "Celé dny",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${SimpleDateFormat("d.M.", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("d.M.", Locale.getDefault()).format(endDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Celý den",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (!event.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (!event.location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Místo",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (event.isRecurringSafe() && !event.recurrencePattern.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Opakování",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Opakuje se ${event.getRecurrencePatternCzech()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Akční tlačítka pro místní eventy
                if (onEdit != null || onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Upravit",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Smazat",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val targetCalendar = Calendar.getInstance()
    targetCalendar.time = date
    
    return today.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)
}