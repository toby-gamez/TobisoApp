package com.example.tobisoappnative.manager

import kotlinx.coroutines.flow.StateFlow

interface IStreakFreezeManager {
    val availableFreezes: StateFlow<Int>
    val usedFreezes: StateFlow<Set<String>>

    fun addStreakFreeze(): Boolean
    fun checkAndAutoUseFreeze(): Boolean
    fun useFreeze(dateString: String): Boolean
    fun getAvailableFreezes(): Int
    fun getUsedFreezes(): Set<String>
    fun isFreezeActive(dateString: String): Boolean
    fun resetFreezes()
}
