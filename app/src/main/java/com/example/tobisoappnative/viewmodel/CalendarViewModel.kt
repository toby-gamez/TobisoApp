package com.example.tobisoappnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Event
import com.example.tobisoappnative.model.LocalEventManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _selectedDateEvents = MutableStateFlow<List<Event>>(emptyList())
    val selectedDateEvents: StateFlow<List<Event>> = _selectedDateEvents

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val context = application.applicationContext

    // Centralizovaná funkce pro kontrolu, jestli event zasahuje do konkrétního dne
    private fun doesEventOverlapDay(event: Event, year: Int, month: Int, day: Int): Boolean {
        val eventStart = event.getStartDateSafe()
        val eventEnd = event.getEndDateSafe()
        
        // Vytvoříme konkrétní den, pro který testujeme
        val targetDay = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val targetDayEnd = Calendar.getInstance().apply {
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
        
        // Debug výpis
        val dateStr = String.format("%d-%02d-%02d", year, month + 1, day)
        android.util.Log.d("EventOverlap", "=== Checking event '${event.getTitleSafe()}' for day $dateStr ===")
        android.util.Log.d("EventOverlap", "  Event start: $eventStart")
        android.util.Log.d("EventOverlap", "  Event end: $eventEnd")
        android.util.Log.d("EventOverlap", "  Target day: $targetDay - $targetDayEnd")
        android.util.Log.d("EventOverlap", "  Is all day: ${event.isAllDaySafe()}")
        
        // Speciální ošetření pro jednodenní události (start == end)
        if (eventStart == eventEnd) {
            // Pro jednodenní události s identickým časem kontrolujeme jen datum
            val eventCal = Calendar.getInstance().apply { time = eventStart }
            val eventDay = Calendar.getInstance().apply {
                set(eventCal.get(Calendar.YEAR), eventCal.get(Calendar.MONTH), eventCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val overlaps = eventDay == targetDay
            android.util.Log.d("EventOverlap", "  Single-moment event overlap result: $overlaps")
            android.util.Log.d("EventOverlap", "  Logic: eventDay(${eventDay}) == targetDay(${targetDay})")
            return overlaps
        }
        
        // Standardní logika pro rozpětí událostí
        val overlaps = (eventStart.before(targetDayEnd) || eventStart == targetDayEnd) &&
                      (eventEnd.after(targetDay) || eventEnd == targetDay)
        
        android.util.Log.d("EventOverlap", "  Standard overlap result: $overlaps")
        android.util.Log.d("EventOverlap", "  Logic: start(${eventStart}) <= dayEnd(${targetDayEnd}) = ${eventStart.before(targetDayEnd) || eventStart == targetDayEnd}")
        android.util.Log.d("EventOverlap", "         end(${eventEnd}) >= dayStart(${targetDay}) = ${eventEnd.after(targetDay) || eventEnd == targetDay}")
        
        return overlaps
    }

    fun getEventsForDay(year: Int, month: Int, day: Int): List<Event> {
        return _events.value.filter { event ->
            doesEventOverlapDay(event, year, month, day)
        }
    }

    fun loadEventsForMonth(year: Int, month: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            try {
                val calendar = Calendar.getInstance()
                
                // Začátek měsíce
                calendar.set(year, month, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = dateFormat.format(calendar.time)
                val startDateObj = calendar.time
                
                // Konec měsíce
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endDate = dateFormat.format(calendar.time)
                val endDateObj = calendar.time
                
                android.util.Log.d("CalendarViewModel", "Loading events from $startDate to $endDate")
                
                // Načti API eventy
                val apiEvents = try {
                    ApiClient.apiService.getEventsInRange(startDate, endDate)
                } catch (e: Exception) {
                    android.util.Log.e("CalendarViewModel", "Error loading API events", e)
                    emptyList()
                }
                
                // Načti místní eventy
                val localEvents = try {
                    LocalEventManager.expandRecurringEvents(context, startDateObj, endDateObj)
                } catch (e: Exception) {
                    android.util.Log.e("CalendarViewModel", "Error loading local events", e)
                    emptyList()
                }
                
                android.util.Log.d("CalendarViewModel", "Loaded ${apiEvents.size} API events and ${localEvents.size} local events")
                
                // Filtruj API eventy s nevalidními daty - endDate může být null pro jednodenní události
                val validApiEvents = apiEvents.filter { event ->
                    event.startDate != null  // Jen startDate je povinné
                }
                
                // Kombinuj všechny eventy
                val allEvents = validApiEvents + localEvents
                
                android.util.Log.d("CalendarViewModel", "Total valid events: ${allEvents.size}")
                _events.value = allEvents
                
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error loading events", e)
                _error.value = "Chyba při načítání eventů: ${e.message}"
                _events.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        
        // Najdi eventy pro vybraný den
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        android.util.Log.d("CalendarViewModel", "=== SELECTING DATE ===")
        android.util.Log.d("CalendarViewModel", "Selected date: $date ($year-${month+1}-$day)")
        android.util.Log.d("CalendarViewModel", "Total events loaded: ${_events.value.size}")
        
        // Debug všech událostí pro tento měsíc
        _events.value.forEach { event ->
            android.util.Log.d("CalendarViewModel", "Available event: ${event.getTitleSafe()} (${event.getStartDateSafe()} - ${event.getEndDateSafe()}) AllDay: ${event.isAllDaySafe()}")
        }
        
        val eventsForDay = getEventsForDay(year, month, day)
        
        android.util.Log.d("CalendarViewModel", "Events found for day: ${eventsForDay.size}")
        eventsForDay.forEachIndexed { index, event ->
            android.util.Log.d("CalendarViewModel", "Event #${index+1}: ${event.getTitleSafe()} (${event.getStartDateSafe()} - ${event.getEndDateSafe()}) AllDay: ${event.isAllDaySafe()}")
        }
        android.util.Log.d("CalendarViewModel", "=== END SELECTION ===")
        
        _selectedDateEvents.value = eventsForDay
    }

    fun clearSelectedDate() {
        _selectedDate.value = null
        _selectedDateEvents.value = emptyList()
    }

    fun getEventsForDate(date: Date): List<Event> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        return getEventsForDay(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH), 
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun loadEventDetail(eventId: Int, onResult: (Event?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Zkus nejdřív najít mezi místními eventy (negativní ID)
                val event = if (eventId < 0) {
                    LocalEventManager.getLocalEvent(context, eventId)
                } else {
                    // API event
                    try {
                        ApiClient.apiService.getEvent(eventId)
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(event)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    // Metody pro práci s místními eventy
    fun addLocalEvent(event: Event, onResult: (Event?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newEvent = LocalEventManager.addLocalEvent(context, event)
                onResult(newEvent)
                
                if (newEvent != null) {
                    // Okamžitě přidej event do seznamu
                    val currentEvents = _events.value.toMutableList()
                    currentEvents.add(newEvent)
                    _events.value = currentEvents
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error adding local event", e)
                onResult(null)
            }
        }
    }
    
    fun updateLocalEvent(event: Event, onResult: (Event?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedEvent = LocalEventManager.updateLocalEvent(context, event)
                onResult(updatedEvent)
                
                if (updatedEvent != null) {
                    // Okamžitě aktualizuj event v seznamu
                    val currentEvents = _events.value.toMutableList()
                    val index = currentEvents.indexOfFirst { it.id == updatedEvent.id }
                    if (index >= 0) {
                        currentEvents[index] = updatedEvent
                        _events.value = currentEvents
                    }
                    
                    // Aktualizuj také vybraný den pokud obsahoval upravovaný event
                    val selectedEvents = _selectedDateEvents.value.toMutableList()
                    val selectedIndex = selectedEvents.indexOfFirst { it.id == updatedEvent.id }
                    if (selectedIndex >= 0) {
                        selectedEvents[selectedIndex] = updatedEvent
                        _selectedDateEvents.value = selectedEvents
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error updating local event", e)
                onResult(null)
            }
        }
    }
    
    fun deleteLocalEvent(eventId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("CalendarViewModel", "Attempting to delete local event with ID: $eventId")
                val deleted = LocalEventManager.deleteLocalEvent(context, eventId)
                android.util.Log.d("CalendarViewModel", "Delete operation result: $deleted")
                
                onResult(deleted)
                if (deleted) {
                    // Okamžitě odstraň event ze seznamu
                    val currentEvents = _events.value.toMutableList()
                    val removedFromMain = currentEvents.removeAll { it.id == eventId }
                    android.util.Log.d("CalendarViewModel", "Removed from main events: $removedFromMain")
                    _events.value = currentEvents
                    
                    // Aktualizuj také vybraný den pokud obsahoval smazaný event
                    val selectedEvents = _selectedDateEvents.value.toMutableList()
                    val removedFromSelected = selectedEvents.removeAll { it.id == eventId }
                    android.util.Log.d("CalendarViewModel", "Removed from selected events: $removedFromSelected")
                    _selectedDateEvents.value = selectedEvents
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error deleting local event", e)
                onResult(false)
            }
        }
    }
}