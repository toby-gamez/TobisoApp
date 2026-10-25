package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.TextButton

import com.tobiso.tobisoappnative.components.OutlinedButton
import androidx.compose.material3.Card
import timber.log.Timber

import com.tobiso.tobisoappnative.navigation.EventDetailRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.tobiso.tobisoappnative.components.*
import androidx.compose.ui.layout.layout
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.EventScope
import com.tobiso.tobisoappnative.model.EventType
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.components.StudentEventDialog
import com.tobiso.tobisoappnative.viewmodel.CalendarViewModel
import com.tobiso.tobisoappnative.viewmodel.CalendarIntent
import com.tobiso.tobisoappnative.viewmodel.CalendarEffect
import com.tobiso.tobisoappnative.viewmodel.home.HomeViewModel
import com.tobiso.tobisoappnative.viewmodel.home.HomeIntent
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.AddEditEventDialog
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.formatPointsBalance
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

// Enum pro filtrování událostí
enum class EventFilter(val displayName: String, val needsAccount: Boolean = false) {
    ALL("Vše"),
    LOCAL("Místní"),
    REMOTE("Vzdálené"),
    CLASS_AND_GROUP("Třída a skupina", needsAccount = true),
    PERSONAL("Moje události", needsAccount = true),
    TESTS("Testy"),
    HOMEWORK("Domácí úkoly"),
    DAY_OFF("Volno");

    fun matches(event: Event): Boolean = when (this) {
        ALL -> true
        LOCAL -> event.isLocalSafe()
        REMOTE -> !event.isLocalSafe()
        CLASS_AND_GROUP -> event.eventScope == EventScope.Class || event.eventScope == EventScope.Group
        PERSONAL -> event.eventScope == EventScope.Personal || event.isLocalSafe()
        TESTS -> event.eventType == EventType.Test
        HOMEWORK -> event.eventType == EventType.Homework
        DAY_OFF -> event.eventType == EventType.DayOff
    }
}

/** Local events and the signed-in student's own server events can be edited/deleted. */
private fun Event.canBeModified(myUserId: Int?): Boolean =
    isLocalSafe() || (myUserId != null && createdByUserId == myUserId)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
    initialYear: Int? = null,
    initialMonth: Int? = null
) {
    val homeVm: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val events = state.events
    val selectedDate = state.selectedDate
    val isLoading = state.isLoading
    val error = state.error
    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
    val isOfflineMode = homeState.isOffline

    var currentMonth by remember { 
        mutableStateOf(initialMonth ?: Calendar.getInstance().get(Calendar.MONTH)) 
    }
    var currentYear by remember { 
        mutableStateOf(initialYear ?: Calendar.getInstance().get(Calendar.YEAR)) 
    }
    var showDateDetail by remember { mutableStateOf(false) }
    
    // States pro TopAppBar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Stavy pro dialog přidání/úpravy eventu
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDateForNewEvent by remember { mutableStateOf<Date?>(null) }
    
    // Stavy pro dialog smazání
    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    
    // Přihlášený student: události ze serveru (třída/skupina/osobní)
    val user by AuthSession.user.collectAsStateWithLifecycle()
    val signedIn = user != null
    val myUserId = if (signedIn) AuthSession.userId else null
    var showStudentDialog by remember { mutableStateOf(false) }
    var showAddChooser by remember { mutableStateOf(false) }
    var editingStudentEvent by remember { mutableStateOf<Event?>(null) }

    // State pro filtrování událostí
    var currentFilter by remember { mutableStateOf(EventFilter.ALL) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    // Derived: events for the selected date, stays fresh whenever events or selection changes
    val selectedDateEvents = remember(events, selectedDate) {
        selectedDate?.let { date ->
            val cal = Calendar.getInstance().apply { time = date }
            viewModel.getEventsForDay(
                events,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        } ?: emptyList()
    }

    // Streak state (lives at top-level to survive TopAppBar recompositions)
    val availableFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.availableFreezes.collectAsStateWithLifecycle()
    val usedFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.usedFreezes.collectAsStateWithLifecycle()
    val currentStreak = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                com.tobiso.tobisoappnative.utils.StreakUtils.getCurrentStreak(context)
            else 0
        )
    }

    LaunchedEffect(currentMonth, currentYear, user) {
        viewModel.onIntent(CalendarIntent.LoadEventsForMonth(currentYear, currentMonth))
    }
    LaunchedEffect(user) {
        viewModel.onIntent(CalendarIntent.RefreshMyClass)
        // A filter that only exists for signed-in users must not outlive the session.
        if (user == null && currentFilter.needsAccount) currentFilter = EventFilter.ALL
    }
    
    // Načtení dat pro FloatingSearchBar
    LaunchedEffect(Unit) {
        homeVm.onIntent(HomeIntent.Load)
    }

    LaunchedEffect(availableFreezes, usedFreezes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentStreak.value = com.tobiso.tobisoappnative.utils.StreakUtils.getCurrentStreak(context)
        }
    }

    // Zpracování one-shot efektů z CalendarViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CalendarEffect.Message -> android.widget.Toast.makeText(context, effect.text, android.widget.Toast.LENGTH_LONG).show()
                is CalendarEffect.EventAdded -> {
                    if (effect.success) {
                        showStudentDialog = false
                        editingStudentEvent = null
                        showAddEventDialog = false
                        selectedDateForNewEvent = null
                    }
                }
                is CalendarEffect.EventUpdated -> {
                    if (effect.success) {
                        showStudentDialog = false
                        editingStudentEvent = null
                        showAddEventDialog = false
                        editingEvent = null
                        selectedDate?.let { viewModel.onIntent(CalendarIntent.SelectDate(it)) }
                    }
                }
                is CalendarEffect.EventDeleted -> {
                    isDeleting = false
                    if (effect.success) {
                        showDeleteDialog = false
                        eventToDelete = null
                        selectedDate?.let { viewModel.onIntent(CalendarIntent.SelectDate(it)) }
                    }
                }
            }
        }
    }

    // Hlavní Box pro celou obrazovku
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Obsah kalendáře
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 110.dp)
            ) {
                Box(Modifier.layout { m, c ->
                    val extra = 16.dp.roundToPx()
                    val p = m.measure(c.copy(maxWidth = c.maxWidth + extra * 2))
                    layout(c.maxWidth, p.height) { p.place(-extra, 0) }
                }) {
                LargeScreenHeader(
                    title = "Kalendář",
                    actions = {
                    // Filter dropdown - přesunut doleva
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showFilterDropdown = true }
                        ) {
                            Icon(
                                Icons.Rounded.FilterList,
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
                            EventFilter.entries.filter { !it.needsAccount || signedIn }.forEach { filter ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(filter.displayName)
                                            if (filter == currentFilter) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Rounded.Check,
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
                    
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = {
                                val now = Calendar.getInstance()
                                currentMonth = now.get(Calendar.MONTH)
                                currentYear = now.get(Calendar.YEAR)
                                viewModel.onIntent(CalendarIntent.SelectDate(now.time))
                                showDateDetail = true
                            },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(start = 10.dp, end = 14.dp)
                        ) {
                            Icon(Icons.Rounded.Today, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dnes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                )
                }
                Spacer(Modifier.height(14.dp))
                // Header s navigací měsíců
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                Column(Modifier.padding(12.dp)) {
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
                        modifier = Modifier.fillMaxWidth().cozyEntrance(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error ?: "Neznámá chyba",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    // Kalendářní mřížka
                    CalendarGrid(
                        currentMonth = currentMonth,
                        currentYear = currentYear,
                        events = events,
                        viewModel = viewModel,
                        eventFilter = currentFilter,
                        onDateClick = { date ->
                            viewModel.onIntent(CalendarIntent.SelectDate(date))
                            showDateDetail = true
                        },
                        onDateLongClick = { date ->
                            selectedDateForNewEvent = date
                            editingEvent = null
                            if (signedIn) showAddChooser = true else showAddEventDialog = true
                        }
                    )
                }
                }
                }
                Row(Modifier.padding(start = 8.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf("Moje události" to MaterialTheme.colorScheme.primary, "Prázdniny a svátky" to MaterialTheme.colorScheme.secondary).forEach { (label, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(color, CircleShape))
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Detail vybraného dne
                if (showDateDetail && selectedDate != null) {
                    val currentSelectedDate = selectedDate
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Filtruj události podle aktuálního filtru
                    val filteredEvents = selectedDateEvents.filter { event ->
                        currentFilter.matches(event)
                    }
                    
                    DateDetailCard(
                        date = currentSelectedDate,
                        events = filteredEvents,
                        navController = navController,
                        onClose = {
                            showDateDetail = false
                            viewModel.onIntent(CalendarIntent.ClearSelectedDate)
                        },
                        myUserId = myUserId,
                        onEditEvent = { event ->
                            selectedDateForNewEvent = null
                            if (event.isLocalSafe()) {
                                editingEvent = event
                                showAddEventDialog = true
                            } else {
                                editingStudentEvent = event
                                showStudentDialog = true
                            }
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
                run {
                    val startOfToday = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.time
                    val upcoming = events
                        .filter { currentFilter.matches(it) && (it.startDate?.let { d -> !d.before(startOfToday) } == true) }
                        .sortedBy { it.startDate }
                        .take(4)
                    if (upcoming.isNotEmpty()) {
                        SectionLabel("Nejbližší", Modifier.padding(horizontal = 0.dp))
                        GroupedList {
                            upcoming.forEachIndexed { index, ev ->
                                if (index > 0) GroupedListDivider()
                                val holiday = ev.eventType == EventType.DayOff
                                val cal = Calendar.getInstance().apply { time = ev.startDate!! }
                                val month = SimpleDateFormat("LLL", Locale.forLanguageTag("cs-CZ")).format(ev.startDate!!)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController?.navigate(EventDetailRoute(eventId = ev.id)) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (holiday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(14.dp)
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val fg = if (holiday) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        Text(cal.get(Calendar.DAY_OF_MONTH).toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = fg)
                                        Text(month, fontSize = 10.5.sp, color = fg)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(ev.getTitleSafe(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text(
                                            SimpleDateFormat("EEEE", Locale.forLanguageTag("cs-CZ")).format(ev.startDate!!).replaceFirstChar { it.uppercase() } +
                                                if (ev.isAllDay == true) " · celý den" else "",
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isOfflineMode) {
                    val hasCachedApiEvents = events.any { !it.isLocalSafe() }
                    Text(
                        text = if (hasCachedApiEvents)
                            "Offline — místní a uložené události"
                        else
                            "Offline — pouze místní události",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        text = "Školní prázdniny a svátky",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        
        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = {
                editingEvent = null
                selectedDateForNewEvent = Calendar.getInstance().time
                if (signedIn) showAddChooser = true else showAddEventDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Událost", fontWeight = FontWeight.Bold)
        }
    }

    // Přihlášený uživatel si vybírá, kam událost uloží
    if (showAddChooser) {
        AlertDialog(
            onDismissRequest = { showAddChooser = false },
            title = { Text("Přidat událost") },
            text = { Text("Událost pro třídu, skupinu nebo jen pro tebe se uloží na server. Místní událost zůstane jen v tomto zařízení.") },
            confirmButton = {
                TextButton(onClick = {
                    showAddChooser = false
                    editingStudentEvent = null
                    showStudentDialog = true
                }) { Text("Do mého účtu") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddChooser = false
                    showAddEventDialog = true
                }) { Text("Jen v zařízení") }
            }
        )
    }

    StudentEventDialog(
        isVisible = showStudentDialog,
        myClass = state.myClass,
        onDismiss = {
            showStudentDialog = false
            editingStudentEvent = null
            selectedDateForNewEvent = null
        },
        onSave = { request ->
            viewModel.onIntent(CalendarIntent.SaveStudentEvent(request, editingStudentEvent?.id))
        },
        initialEvent = editingStudentEvent,
        initialDate = selectedDateForNewEvent,
        themeOptions = state.themeOptions,
        onNeedThemeOptions = { viewModel.onIntent(CalendarIntent.LoadThemeOptions) }
    )

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
                viewModel.onIntent(CalendarIntent.UpdateLocalEvent(event))
            } else {
                viewModel.onIntent(CalendarIntent.AddLocalEvent(event))
            }
        },
        initialEvent = editingEvent,
        initialDate = selectedDateForNewEvent
    )
    
    // Dialog pro potvrzení smazání
    if (showDeleteDialog && eventToDelete != null) {
        val currentEventToDelete = eventToDelete
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                eventToDelete = null
                isDeleting = false
            },
            title = { Text("Smazat událost") },
            text = { 
                Text("Opravdu chcete smazat událost \"${currentEventToDelete?.getTitleSafe() ?: "tuto událost"}\"? Tato akce je nevratná.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        currentEventToDelete?.let { ev ->
                            viewModel.onIntent(
                                if (ev.isLocalSafe()) CalendarIntent.DeleteLocalEvent(ev.id)
                                else CalendarIntent.DeleteStudentEvent(ev.id)
                            )
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
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Předchozí měsíc")
        }

        Text(
            text = "${monthNames[currentMonth]} $currentYear",
            fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Další měsíc")
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Int,
    currentYear: Int,
    events: List<Event>,
    viewModel: CalendarViewModel = hiltViewModel(),
    eventFilter: EventFilter = EventFilter.ALL,
    onDateClick: (Date) -> Unit,
    onDateLongClick: (Date) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate = state.selectedDate
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
        
        // Vytvoř mřížku — regular Column/Row (not lazy) so every cell always recomposes
        // when events change; a calendar has at most 42 cells, all visible at once.
        val numRows = ((adjustedFirstDay - 1 + daysInMonth + 6) / 7)

        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(numRows) { rowIndex ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { colIndex ->
                        val index = rowIndex * 7 + colIndex
                        val dayNumber = index - adjustedFirstDay + 2

                        if (dayNumber in 1..daysInMonth) {
                            val dayCalendar = Calendar.getInstance()
                            dayCalendar.set(currentYear, currentMonth, dayNumber)
                            val dayDate = dayCalendar.time

                            val allDayEvents = viewModel.getEventsForDay(events, currentYear, currentMonth, dayNumber)

                            val dayEvents = allDayEvents.filter { event ->
                                eventFilter.matches(event)
                            }

                            val isSelected = selectedDate?.let { selected ->
                                val selectedCalendar = Calendar.getInstance().apply { time = selected }
                                val dayCalendarForCheck = Calendar.getInstance().apply { time = dayDate }
                                selectedCalendar.get(Calendar.YEAR) == dayCalendarForCheck.get(Calendar.YEAR) &&
                                selectedCalendar.get(Calendar.MONTH) == dayCalendarForCheck.get(Calendar.MONTH) &&
                                selectedCalendar.get(Calendar.DAY_OF_MONTH) == dayCalendarForCheck.get(Calendar.DAY_OF_MONTH)
                            } ?: false

                            Box(modifier = Modifier.weight(1f)) {
                                CalendarDay(
                                    day = dayNumber,
                                    hasEvents = dayEvents.isNotEmpty(),
                                    eventCount = dayEvents.size,
                                    hasHoliday = dayEvents.any { it.eventType == EventType.DayOff },
                                    hasOwn = dayEvents.any { it.eventType != EventType.DayOff },
                                    isToday = isToday(dayDate),
                                    isSelected = isSelected,
                                    onClick = { onDateClick(dayDate) },
                                    onLongClick = { onDateLongClick(dayDate) }
                                )
                            }
                        } else {
                            // Empty cell
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
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
    onLongClick: () -> Unit = {},
    hasHoliday: Boolean = false,
    hasOwn: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isToday) scheme.primary else Color.Transparent)
            .then(if (isSelected && !isToday) Modifier.border(2.dp, scheme.primary, CircleShape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                fontSize = 15.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) scheme.onPrimary else scheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 1.dp).height(5.dp)) {
                if (hasEvents && hasOwn) Box(Modifier.size(5.dp).background(if (isToday) scheme.onPrimary else scheme.primary, CircleShape))
                if (hasEvents && hasHoliday) Box(Modifier.size(5.dp).background(if (isToday) scheme.onPrimary else scheme.secondary, CircleShape))
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
    myUserId: Int? = null,
    onEditEvent: (Event) -> Unit = {},
    onDeleteEvent: (Int) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.forLanguageTag("cs-CZ"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().cozyEntrance(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = dateFormat.format(date).replaceFirstChar { it.uppercase() },
                    fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Zavřít")
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
                                navController?.navigate(EventDetailRoute(eventId = event.id))
                            },
                            onEdit = if (event.canBeModified(myUserId)) { { onEditEvent(event) } } else null,
                            onDelete = if (event.canBeModified(myUserId)) { { onDeleteEvent(event.id) } } else null
                        )

                        Spacer(modifier = Modifier.height(8.dp))
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
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
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
                        .padding(top = 2.dp)
                        .size(12.dp)
                        .background(
                            color = try {
                                Color(android.graphics.Color.parseColor(event.getColorSafe()))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.getTitleSafe(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    eventMetaLine(event)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

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
                                Icons.Rounded.LocationOn,
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
                                Icons.Rounded.Repeat,
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
                                    Icons.Rounded.Edit,
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
                                    Icons.Rounded.Delete,
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

/** "Test · Moje třída · Jana" — type, audience and author of a server event (null for plain public events). */
internal fun eventMetaLine(event: Event): String? {
    val parts = buildList {
        event.eventType?.let { add(it.label) }
        when (event.eventScope) {
            EventScope.Class -> add(event.className ?: "Třída")
            EventScope.Group -> add(listOfNotNull(event.className, event.groupName).joinToString(" · ").ifBlank { "Skupina" })
            EventScope.Personal -> add("Osobní")
            else -> Unit
        }
        event.createdByName?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    // A plain global admin event ("Volno" for everyone) needs no label.
    return parts.takeIf { event.eventScope != null && event.eventScope != EventScope.Global || event.isStudentCreated }
        ?.joinToString(" · ")
}

private fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val targetCalendar = Calendar.getInstance()
    targetCalendar.time = date
    
    return today.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)
}