package com.example.tobisoappnative.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Event
import com.example.tobisoappnative.model.LocalEventManager
import com.example.tobisoappnative.model.OfflineDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel(application: Application) :
    BaseAndroidViewModel<CalendarState, CalendarIntent, CalendarEffect>(application, CalendarState()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val context = application.applicationContext
    private val offlineDataManager = OfflineDataManager(application)

    /** The month currently loaded – used for internal reloads after mutations. */
    private var lastLoadedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var lastLoadedMonth = Calendar.getInstance().get(Calendar.MONTH)

    override fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadEventsForMonth -> loadEventsForMonth(intent.year, intent.month)
            is CalendarIntent.SelectDate -> selectDate(intent.date)
            CalendarIntent.ClearSelectedDate -> setState { copy(selectedDate = null, selectedDateEvents = emptyList()) }
            is CalendarIntent.LoadEventDetail -> loadEventDetail(intent.eventId)
            is CalendarIntent.AddLocalEvent -> addLocalEvent(intent.event)
            is CalendarIntent.UpdateLocalEvent -> updateLocalEvent(intent.event)
            is CalendarIntent.DeleteLocalEvent -> deleteLocalEvent(intent.eventId)
        }
    }

    // ── Pure helpers (public for use from CalendarGrid composable) ─────────────

    fun getEventsForDay(year: Int, month: Int, day: Int): List<Event> {
        return currentState.events.filter { event -> doesEventOverlapDay(event, year, month, day) }
    }

    // ── Private implementation ─────────────────────────────────────────────────

    private fun doesEventOverlapDay(event: Event, year: Int, month: Int, day: Int): Boolean {
        val eventStart = event.getStartDateSafe()
        val eventEnd = event.getEndDateSafe()

        val targetDay = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val targetDayEnd = Calendar.getInstance().apply {
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        if (eventStart == eventEnd) {
            val eventCal = Calendar.getInstance().apply { time = eventStart }
            val eventDay = Calendar.getInstance().apply {
                set(eventCal.get(Calendar.YEAR), eventCal.get(Calendar.MONTH), eventCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            return eventDay == targetDay
        }

        return (eventStart.before(targetDayEnd) || eventStart == targetDayEnd) &&
               (eventEnd.after(targetDay) || eventEnd == targetDay)
    }

    private fun loadEventsForMonth(year: Int, month: Int) {
        lastLoadedYear = year
        lastLoadedMonth = month
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true, error = null) }

            try {
                val calendar = Calendar.getInstance()

                calendar.set(year, month, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = dateFormat.format(calendar.time)
                val startDateObj = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endDate = dateFormat.format(calendar.time)
                val endDateObj = calendar.time

                try {
                    if (offlineDataManager.isEventsCacheFresh(15)) {
                        val cached = offlineDataManager.getCachedEvents() ?: emptyList()
                        val cachedInRange = cached.filter { ev ->
                            val evStart = ev.getStartDateSafe()
                            val evEnd = ev.getEndDateSafe()
                            (evStart.before(endDateObj) || evStart == endDateObj) &&
                            (evEnd.after(startDateObj) || evEnd == startDateObj)
                        }
                        val localEvents = try {
                            LocalEventManager.expandRecurringEvents(context, startDateObj, endDateObj)
                        } catch (e: Exception) { emptyList() }
                        setState { copy(events = cachedInRange + localEvents, isLoading = false) }
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CalendarViewModel", "Error checking/using events cache: ${e.message}")
                }

                val apiEvents = try {
                    ApiClient.apiService.getEventsInRange(startDate, endDate).toList()
                } catch (e: Exception) {
                    android.util.Log.e("CalendarViewModel", "Error loading API events", e)
                    emptyList()
                }

                try {
                    if (apiEvents.isNotEmpty()) offlineDataManager.saveEvents(apiEvents)
                } catch (e: Exception) {
                    android.util.Log.w("CalendarViewModel", "Failed to save API events to cache: ${e.message}")
                }

                val localEvents = try {
                    LocalEventManager.expandRecurringEvents(context, startDateObj, endDateObj)
                } catch (e: Exception) { emptyList() }

                val allEvents = apiEvents.filter { it.startDate != null } + localEvents
                setState { copy(events = allEvents) }

            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error loading events", e)
                setState { copy(error = "Chyba při načítání eventů: ${e.message}", events = emptyList()) }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun selectDate(date: Date) {
        val calendar = Calendar.getInstance().apply { time = date }
        val eventsForDay = getEventsForDay(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        setState { copy(selectedDate = date, selectedDateEvents = eventsForDay) }
    }

    private fun loadEventDetail(eventId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(detailEventLoading = true) }
            val event = if (eventId < 0) {
                LocalEventManager.getLocalEvent(context, eventId)
            } else {
                try { ApiClient.apiService.getEvent(eventId) } catch (e: Exception) { null }
            }
            setState { copy(detailEvent = event, detailEventLoading = false) }
        }
    }

    private fun addLocalEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newEvent = LocalEventManager.addLocalEvent(context, event)
                if (newEvent != null) {
                    setState { copy(events = events + newEvent) }
                    loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                }
                emitEffect(CalendarEffect.EventAdded(success = newEvent != null))
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error adding local event", e)
                emitEffect(CalendarEffect.EventAdded(success = false))
            }
        }
    }

    private fun updateLocalEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedEvent = LocalEventManager.updateLocalEvent(context, event)
                if (updatedEvent != null) {
                    setState {
                        val updatedList = events.map { if (it.id == updatedEvent.id) updatedEvent else it }
                        val updatedSelected = selectedDateEvents.map { if (it.id == updatedEvent.id) updatedEvent else it }
                        copy(
                            events = updatedList,
                            selectedDateEvents = updatedSelected,
                            detailEvent = if (detailEvent?.id == updatedEvent.id) updatedEvent else detailEvent
                        )
                    }
                    loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                }
                emitEffect(CalendarEffect.EventUpdated(success = updatedEvent != null))
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error updating local event", e)
                emitEffect(CalendarEffect.EventUpdated(success = false))
            }
        }
    }

    private fun deleteLocalEvent(eventId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = LocalEventManager.deleteLocalEvent(context, eventId)
                if (deleted) {
                    setState {
                        copy(
                            events = events.filter { it.id != eventId },
                            selectedDateEvents = selectedDateEvents.filter { it.id != eventId },
                            detailEvent = if (detailEvent?.id == eventId) null else detailEvent
                        )
                    }
                    loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                }
                emitEffect(CalendarEffect.EventDeleted(success = deleted, eventId = eventId))
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "Error deleting local event", e)
                emitEffect(CalendarEffect.EventDeleted(success = false, eventId = eventId))
            }
        }
    }
}
