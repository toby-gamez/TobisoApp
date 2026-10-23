package com.tobiso.tobisoappnative.viewmodel

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.StudentEventRequest
import java.util.Date

data class CalendarState(
    val events: List<Event> = emptyList(),
    val selectedDate: Date? = null,
    val selectedDateEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val detailEvent: Event? = null,
    val detailEventLoading: Boolean = false,
    /** The signed-in student's class membership (needed to offer Class/Group scope when adding events). */
    val myClass: com.tobiso.tobisoappnative.model.MyClass? = null,
    /** Posts and categories a student can attach to an event (loaded when the add/edit dialog first opens). */
    val themeOptions: List<com.tobiso.tobisoappnative.model.EventTheme> = emptyList()
) : UiState

sealed interface CalendarIntent : UiIntent {
    data class LoadEventsForMonth(val year: Int, val month: Int) : CalendarIntent
    data class SelectDate(val date: Date) : CalendarIntent
    object ClearSelectedDate : CalendarIntent
    data class LoadEventDetail(val eventId: Int) : CalendarIntent
    data class AddLocalEvent(val event: Event) : CalendarIntent
    data class UpdateLocalEvent(val event: Event) : CalendarIntent
    data class DeleteLocalEvent(val eventId: Int) : CalendarIntent
    /** Create ([editId] = null) or update the signed-in student's own server event. */
    data class SaveStudentEvent(val request: StudentEventRequest, val editId: Int? = null) : CalendarIntent
    data class DeleteStudentEvent(val eventId: Int) : CalendarIntent
    object RefreshMyClass : CalendarIntent
    object LoadThemeOptions : CalendarIntent
}

sealed interface CalendarEffect : UiEffect {
    data class EventAdded(val success: Boolean) : CalendarEffect
    data class EventUpdated(val success: Boolean) : CalendarEffect
    data class EventDeleted(val success: Boolean, val eventId: Int) : CalendarEffect
    data class Message(val text: String) : CalendarEffect
}
