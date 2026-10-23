package com.tobiso.tobisoappnative.viewmodel
import timber.log.Timber

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.LocalEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val calendarRepo: com.tobiso.tobisoappnative.repository.CalendarRepository,
    private val classesRepo: com.tobiso.tobisoappnative.repository.ClassesRepository,
) : BaseAndroidViewModel<CalendarState, CalendarIntent, CalendarEffect>(
    application, CalendarState(), savedStateHandle
) {

    private val apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private var lastLoadedYear = savedStateHandle.get<Int>("lastLoadedYear") ?: LocalDate.now().year
    private var lastLoadedMonth = savedStateHandle.get<Int>("lastLoadedMonth")
        ?: (LocalDate.now().monthValue - 1)

    init {
        savedStateHandle.get<Long>("selectedDateMillis")?.let { millis ->
            setState { copy(selectedDate = Date(millis)) }
        }
    }

    override fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadEventsForMonth -> loadEventsForMonth(intent.year, intent.month)
            is CalendarIntent.SelectDate -> selectDate(intent.date)
            CalendarIntent.ClearSelectedDate -> setState { copy(selectedDate = null, selectedDateEvents = emptyList()) }
            is CalendarIntent.LoadEventDetail -> loadEventDetail(intent.eventId)
            is CalendarIntent.AddLocalEvent -> addLocalEvent(intent.event)
            is CalendarIntent.UpdateLocalEvent -> updateLocalEvent(intent.event)
            is CalendarIntent.DeleteLocalEvent -> deleteLocalEvent(intent.eventId)
            is CalendarIntent.SaveStudentEvent -> saveStudentEvent(intent.request, intent.editId)
            is CalendarIntent.DeleteStudentEvent -> deleteStudentEvent(intent.eventId)
            CalendarIntent.RefreshMyClass -> refreshMyClass()
        }
    }

    // ── Pure helpers (public for use from CalendarGrid composable) ─────────────

    fun getEventsForDay(year: Int, month: Int, day: Int): List<Event> {
        return currentState.events.filter { event -> doesEventOverlapDay(event, year, month, day) }
    }

    fun getEventsForDay(events: List<Event>, year: Int, month: Int, day: Int): List<Event> {
        return events.filter { event -> doesEventOverlapDay(event, year, month, day) }
    }

    // ── Private implementation ─────────────────────────────────────────────────

    private fun doesEventOverlapDay(event: Event, year: Int, month: Int, day: Int): Boolean {
        val eventStart = event.getStartDateSafe()
        val eventEnd = event.getEndDateSafe()

        // month is 0-indexed (Calendar convention), LocalDate uses 1-indexed
        val targetDate = LocalDate.of(year, month + 1, day)
        val zone = ZoneId.systemDefault()
        val startOfDayMs = targetDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDayMs = targetDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        if (eventStart.time == eventEnd.time) {
            val eventDate = Instant.ofEpochMilli(eventStart.time).atZone(zone).toLocalDate()
            return eventDate == targetDate
        }

        return (eventStart.time <= endOfDayMs) && (eventEnd.time >= startOfDayMs)
    }

    private fun loadEventsForMonth(year: Int, month: Int) {
        lastLoadedYear = year
        lastLoadedMonth = month
        savedStateHandle?.set("lastLoadedYear", year)
        savedStateHandle?.set("lastLoadedMonth", month)
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true, error = null) }

            try {
                // month is 0-indexed (Calendar convention), LocalDateTime uses 1-indexed
                val startOfMonth = LocalDateTime.of(year, month + 1, 1, 0, 0, 0)
                val endOfMonth = startOfMonth
                    .withDayOfMonth(startOfMonth.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59)
                val zone = ZoneId.systemDefault()
                val startDateObj = Date.from(startOfMonth.atZone(zone).toInstant())
                val endDateObj = Date.from(endOfMonth.atZone(zone).toInstant())

                val serverEvents = calendarRepo.loadRange(startDateObj, endDateObj)
                val localEvents = try {
                    LocalEventManager.expandRecurringEvents(getApplication(), startDateObj, endDateObj)
                } catch (_: Exception) { emptyList() }

                setState { copy(events = serverEvents + localEvents) }
            } catch (e: Exception) {
                Timber.e(e, "Error loading events")
                setState { copy(error = "Chyba při načítání eventů: ${e.message}", events = emptyList()) }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun selectDate(date: Date) {
        val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val eventsForDay = getEventsForDay(
            localDate.year,
            localDate.monthValue - 1,
            localDate.dayOfMonth,
        )
        setState { copy(selectedDate = date, selectedDateEvents = eventsForDay) }
        savedStateHandle?.set("selectedDateMillis", date.time)
    }

    private fun loadEventDetail(eventId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(detailEventLoading = true) }
            val event = if (eventId < 0) {
                LocalEventManager.getLocalEvent(getApplication(), eventId)
            } else {
                // Class/group/personal events can't be fetched by id as a guest, so look at what the
                // calendar already loaded (and our cache) before asking the server.
                currentState.events.find { it.id == eventId && !it.isLocalSafe() }
                    ?: calendarRepo.cachedEvent(eventId)
                    ?: calendarRepo.fetchEvent(eventId)
            }
            setState { copy(detailEvent = event, detailEventLoading = false) }
        }
    }

    private fun addLocalEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newEvent = LocalEventManager.addLocalEvent(getApplication(), event)
                setState { copy(events = events + newEvent) }
                loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                emitEffect(CalendarEffect.EventAdded(success = true))
            } catch (e: Exception) {
                Timber.e(e, "Error adding local event")
                emitEffect(CalendarEffect.EventAdded(success = false))
            }
        }
    }

    private fun updateLocalEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedEvent = LocalEventManager.updateLocalEvent(getApplication(), event)
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
                Timber.e(e, "Error updating local event")
                emitEffect(CalendarEffect.EventUpdated(success = false))
            }
        }
    }

    private fun deleteLocalEvent(eventId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = LocalEventManager.deleteLocalEvent(getApplication(), eventId)
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
                Timber.e(e, "Error deleting local event")
                emitEffect(CalendarEffect.EventDeleted(success = false, eventId = eventId))
            }
        }
    }

    private fun saveStudentEvent(request: com.tobiso.tobisoappnative.model.StudentEventRequest, editId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = if (editId == null) calendarRepo.createStudentEvent(request).map { }
            else calendarRepo.updateStudentEvent(editId, request)
            result.fold(
                onSuccess = {
                    loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                    if (editId == null) emitEffect(CalendarEffect.EventAdded(success = true))
                    else emitEffect(CalendarEffect.EventUpdated(success = true))
                },
                onFailure = { e ->
                    emitEffect(CalendarEffect.Message(e.message ?: "Událost se nepodařilo uložit."))
                    if (editId == null) emitEffect(CalendarEffect.EventAdded(success = false))
                    else emitEffect(CalendarEffect.EventUpdated(success = false))
                }
            )
        }
    }

    private fun deleteStudentEvent(eventId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            calendarRepo.deleteStudentEvent(eventId).fold(
                onSuccess = {
                    setState {
                        copy(
                            events = events.filter { it.id != eventId },
                            selectedDateEvents = selectedDateEvents.filter { it.id != eventId },
                            detailEvent = if (detailEvent?.id == eventId) null else detailEvent
                        )
                    }
                    loadEventsForMonth(lastLoadedYear, lastLoadedMonth)
                    emitEffect(CalendarEffect.EventDeleted(success = true, eventId = eventId))
                },
                onFailure = { e ->
                    emitEffect(CalendarEffect.Message(e.message ?: "Událost se nepodařilo smazat."))
                    emitEffect(CalendarEffect.EventDeleted(success = false, eventId = eventId))
                }
            )
        }
    }

    private fun refreshMyClass() {
        if (!com.tobiso.tobisoappnative.model.AuthSession.isSignedIn) {
            setState { copy(myClass = null) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            classesRepo.getMyClass().onSuccess { m -> setState { copy(myClass = m) } }
        }
    }
}
