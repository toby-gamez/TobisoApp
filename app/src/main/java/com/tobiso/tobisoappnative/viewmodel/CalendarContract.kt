package com.tobiso.tobisoappnative.viewmodel

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.Event
import java.util.Date

data class CalendarState(
    val events: List<Event> = emptyList(),
    val selectedDate: Date? = null,
    val selectedDateEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val detailEvent: Event? = null,
    val detailEventLoading: Boolean = false
) : UiState

sealed interface CalendarIntent : UiIntent {
    data class LoadEventsForMonth(val year: Int, val month: Int) : CalendarIntent
    data class SelectDate(val date: Date) : CalendarIntent
    object ClearSelectedDate : CalendarIntent
    data class LoadEventDetail(val eventId: Int) : CalendarIntent
    data class AddLocalEvent(val event: Event) : CalendarIntent
    data class UpdateLocalEvent(val event: Event) : CalendarIntent
    data class DeleteLocalEvent(val eventId: Int) : CalendarIntent
}

sealed interface CalendarEffect : UiEffect {
    data class EventAdded(val success: Boolean) : CalendarEffect
    data class EventUpdated(val success: Boolean) : CalendarEffect
    data class EventDeleted(val success: Boolean, val eventId: Int) : CalendarEffect
}
