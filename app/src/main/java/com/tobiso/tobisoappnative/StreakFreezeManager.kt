package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.manager.IStreakFreezeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakFreezeManager private constructor(context: Context) : IStreakFreezeManager {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _availableFreezes = MutableStateFlow(0)
    override val availableFreezes: StateFlow<Int> = _availableFreezes

    private val _usedFreezes = MutableStateFlow<Set<String>>(emptySet())
    override val usedFreezes: StateFlow<Set<String>> = _usedFreezes

    init {
        _availableFreezes.value = prefs.getInt(KEY_FREEZE_COUNT, 0)
        _usedFreezes.value = prefs.getStringSet(KEY_USED_FREEZES, emptySet()) ?: emptySet()
    }

    override fun addStreakFreeze(): Boolean {
        val currentCount = prefs.getInt(KEY_FREEZE_COUNT, 0)
        if (currentCount >= MAX_FREEZES) return false
        val newCount = currentCount + 1
        prefs.edit().putInt(KEY_FREEZE_COUNT, newCount).apply()
        _availableFreezes.value = newCount
        return true
    }

    override fun checkAndAutoUseFreeze(): Boolean {
        val streakDays = getStreakDays()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        if (_availableFreezes.value <= 0) return false

        val yesterday = today.minusDays(1)
        val yesterdayString = yesterday.format(formatter)
        val hasYesterdayActivity = streakDays.contains(yesterdayString)
        val usedFreezesSet = _usedFreezes.value

        if (!hasYesterdayActivity && !usedFreezesSet.contains(yesterdayString)) {
            if (wouldSaveStreak(yesterdayString)) {
                return useFreeze(yesterdayString)
            }
        }
        return false
    }

    private fun wouldSaveStreak(freezeDate: String): Boolean {
        val streakDays = getStreakDays()
        val freezeLocalDate = LocalDate.parse(freezeDate)
        val today = LocalDate.now()
        val dayBeforeFreeze = freezeLocalDate.minusDays(1)
        val dayAfterFreeze = freezeLocalDate.plusDays(1)
        val hasDayBefore = streakDays.contains(dayBeforeFreeze.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        val hasDayAfter = dayAfterFreeze == today
        return hasDayBefore && hasDayAfter
    }

    override fun useFreeze(dateString: String): Boolean {
        val currentCount = prefs.getInt(KEY_FREEZE_COUNT, 0)
        if (currentCount <= 0) return false
        val usedFreezesSet = _usedFreezes.value.toMutableSet()
        if (usedFreezesSet.contains(dateString)) return false
        val newCount = currentCount - 1
        usedFreezesSet.add(dateString)
        prefs.edit()
            .putInt(KEY_FREEZE_COUNT, newCount)
            .putStringSet(KEY_USED_FREEZES, usedFreezesSet)
            .apply()
        _availableFreezes.value = newCount
        _usedFreezes.value = usedFreezesSet
        return true
    }

    override fun getAvailableFreezes(): Int = _availableFreezes.value

    override fun getUsedFreezes(): Set<String> = _usedFreezes.value

    override fun isFreezeActive(dateString: String): Boolean = _usedFreezes.value.contains(dateString)

    override fun resetFreezes() {
        prefs.edit().clear().apply()
        _availableFreezes.value = 0
        _usedFreezes.value = emptySet()
    }

    private fun getStreakDays(): Set<String> {
        return appContext.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
            .getStringSet("streak_days", emptySet()) ?: emptySet()
    }

    companion object {
        private const val PREFS_NAME = "streak_freeze_prefs"
        private const val KEY_FREEZE_COUNT = "freeze_count"
        private const val KEY_USED_FREEZES = "used_freezes"
        private const val MAX_FREEZES = 3

        @Volatile private var INSTANCE: StreakFreezeManager? = null

        val instance: StreakFreezeManager
            get() = INSTANCE ?: error("StreakFreezeManager.initialize() must be called before use")

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = StreakFreezeManager(context.applicationContext)
                    }
                }
            }
        }

        // Delegations for direct access without .instance
        val availableFreezes get() = instance.availableFreezes
        val usedFreezes get() = instance.usedFreezes

        fun addStreakFreeze() = instance.addStreakFreeze()
        fun checkAndAutoUseFreeze() = instance.checkAndAutoUseFreeze()
        fun useFreeze(dateString: String) = instance.useFreeze(dateString)
        fun getAvailableFreezes() = instance.getAvailableFreezes()
        fun getUsedFreezes() = instance.getUsedFreezes()
        fun isFreezeActive(dateString: String) = instance.isFreezeActive(dateString)
        fun resetFreezes() = instance.resetFreezes()
    }
}