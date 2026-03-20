package com.tobiso.tobisoappnative.fake

import com.tobiso.tobisoappnative.manager.IStreakFreezeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory fake [IStreakFreezeManager] for unit tests.
 * No Android/SharedPreferences dependencies.
 */
class FakeStreakFreezeManager(initialFreezes: Int = 0) : IStreakFreezeManager {

    companion object {
        const val MAX_FREEZES = 3
    }

    private var freezeCount = initialFreezes
    private val usedFreezesSet = mutableSetOf<String>()

    override val availableFreezes = MutableStateFlow(initialFreezes)
    override val usedFreezes: StateFlow<Set<String>> = MutableStateFlow(emptySet<String>())
    private val _usedFreezes get() = usedFreezes as MutableStateFlow<Set<String>>

    override fun addStreakFreeze(): Boolean {
        if (freezeCount >= MAX_FREEZES) return false
        freezeCount++
        availableFreezes.value = freezeCount
        return true
    }

    override fun checkAndAutoUseFreeze(): Boolean = false // not needed for unit tests

    override fun useFreeze(dateString: String): Boolean {
        if (freezeCount <= 0) return false
        if (usedFreezesSet.contains(dateString)) return false
        freezeCount--
        usedFreezesSet.add(dateString)
        availableFreezes.value = freezeCount
        _usedFreezes.value = usedFreezesSet.toSet()
        return true
    }

    override fun getAvailableFreezes(): Int = freezeCount

    override fun getUsedFreezes(): Set<String> = usedFreezesSet.toSet()

    override fun isFreezeActive(dateString: String): Boolean = usedFreezesSet.contains(dateString)

    override fun resetFreezes() {
        freezeCount = 0
        usedFreezesSet.clear()
        availableFreezes.value = 0
        _usedFreezes.value = emptySet()
    }
}
