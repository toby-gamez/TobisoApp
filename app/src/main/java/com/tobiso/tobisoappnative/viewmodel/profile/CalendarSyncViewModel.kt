package com.tobiso.tobisoappnative.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.CalendarSync
import com.tobiso.tobisoappnative.model.UpdateCalendarSyncRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CalendarSyncState(
    val sync: CalendarSync? = null,
    val busy: Boolean = false,
    val error: String? = null
)

/** Profile block that subscribes phone calendars (Google, Apple, ...) to the student's personal .ics feed. */
@HiltViewModel
class CalendarSyncViewModel @Inject constructor(private val api: ApiService) : ViewModel() {

    private val _state = MutableStateFlow(CalendarSyncState())
    val state: StateFlow<CalendarSyncState> = _state

    /** Fetches the feed address (the server creates it on first use). */
    fun show() = run { api.getCalendarSync() }

    fun regenerate() = run { api.regenerateCalendarSync() }

    fun toggleType(type: String) {
        val s = _state.value.sync ?: return
        val types = if (type in s.types) s.types - type else s.types + type
        run { api.updateCalendarSync(UpdateCalendarSyncRequest(types, s.duty)) }
    }

    fun toggleDuty() {
        val s = _state.value.sync ?: return
        run { api.updateCalendarSync(UpdateCalendarSyncRequest(s.types, !s.duty)) }
    }

    private fun run(call: suspend () -> CalendarSync) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val sync = call()
                _state.update { it.copy(busy = false, sync = sync) }
            } catch (e: Exception) {
                Timber.e(e, "calendar sync")
                _state.update { it.copy(busy = false, error = "Nepodařilo se spojit se serverem. Zkus to prosím znovu.") }
            }
        }
    }
}
