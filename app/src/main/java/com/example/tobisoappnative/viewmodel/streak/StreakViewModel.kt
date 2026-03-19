package com.example.tobisoappnative.viewmodel.streak

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.StreakFreezeManager
import com.example.tobisoappnative.utils.StreakUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StreakViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val today = Calendar.getInstance()

    private val _calendarMonth = MutableStateFlow(today.get(Calendar.MONTH))
    val calendarMonth: StateFlow<Int> = _calendarMonth

    private val _calendarYear = MutableStateFlow(today.get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear

    private val _streakDays = MutableStateFlow<Set<String>>(emptySet())
    val streakDays: StateFlow<Set<String>> = _streakDays

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _maxStreak = MutableStateFlow(0)
    val maxStreak: StateFlow<Int> = _maxStreak

    @RequiresApi(Build.VERSION_CODES.O)
    fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshStreakData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshStreakData() {
        viewModelScope.launch(Dispatchers.IO) {
            _streakDays.value = StreakUtils.getStreakDays(getApplication())
            val (current, max) = StreakUtils.calculateStreaks(getApplication())
            _currentStreak.value = current
            _maxStreak.value = max
        }
    }

    fun changeMonth(month: Int, year: Int) {
        _calendarMonth.value = month
        _calendarYear.value = year
    }
}
