package com.tobiso.tobisoappnative.manager

import kotlinx.coroutines.flow.StateFlow

interface IPointsManager {
    val totalPoints: StateFlow<Int>
    /** Current balance as Float — use for display so sub-integer values aren't lost. */
    val totalPointsFloat: StateFlow<Float>
    val lastAddedPoints: StateFlow<Int>
    val lastMilestone: StateFlow<Int?>
    val lastAchievement: StateFlow<Int?>
    val lastPrestigeTierPoints: StateFlow<Int?>
    /** Emits the new inflationDivisor when a 100k reset occurs; null otherwise. */
    val lastPointsReset: StateFlow<Int?>
    val activeMultiplier: StateFlow<Float>
    val totalEarnedPoints: StateFlow<Int>

    fun addPoints(amount: Int)
    fun addPointsForMilestone(amount: Int, milestoneDay: Int)
    fun addPointsForAchievement(amount: Int, achievementPoints: Int)
    fun resetLastAddedPoints()
    fun resetLastPrestigeTier()
    fun resetLastPointsReset()
    fun getPoints(): Int
    fun getTotalEarnedPoints(): Int
    fun subtractPoints(amount: Int): Boolean
    fun activateMultiplier(multiplier: Float, durationMinutes: Int)
    fun getMultiplierTimeLeft(): Long
    fun getMultiplierTimeLeftInSeconds(): Long
    fun isMultiplierActive(): Boolean
}
