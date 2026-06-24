package com.tobiso.tobisoappnative.fake

import com.tobiso.tobisoappnative.manager.IPointsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory fake implementation of [IPointsManager] for unit tests.
 * Has no Android/SharedPreferences dependencies.
 */
class FakePointsManager : IPointsManager {

    private var currentPointsFloat = 0f
    private var earnedPoints = 0
    private var multiplier = 1.0f
    private var multiplierEndMs = 0L
    private var inflationDivisor = 1

    override val totalPoints = MutableStateFlow(0)
    override val lastAddedPoints = MutableStateFlow(0)
    override val lastMilestone = MutableStateFlow<Int?>(null)
    override val lastAchievement = MutableStateFlow<Int?>(null)
    override val activeMultiplier = MutableStateFlow(1.0f)
    override val totalEarnedPoints = MutableStateFlow(0)

    override fun addPoints(amount: Int) {
        val deflated = amount.toFloat() / inflationDivisor * multiplier
        currentPointsFloat += deflated
        earnedPoints += deflated.toInt()
        lastAddedPoints.value = deflated.toInt()
        totalEarnedPoints.value = earnedPoints
        checkAndResetIfOverLimit()
    }

    override fun addPointsForMilestone(amount: Int, milestoneDay: Int) {
        val deflated = amount.toFloat() / inflationDivisor
        currentPointsFloat += deflated
        earnedPoints += deflated.toInt()
        lastAddedPoints.value = deflated.toInt()
        lastMilestone.value = milestoneDay
        totalEarnedPoints.value = earnedPoints
        checkAndResetIfOverLimit()
    }

    override fun addPointsForAchievement(amount: Int, achievementPoints: Int) {
        val deflated = amount.toFloat() / inflationDivisor
        currentPointsFloat += deflated
        earnedPoints += deflated.toInt()
        lastAddedPoints.value = deflated.toInt()
        lastAchievement.value = achievementPoints
        totalEarnedPoints.value = earnedPoints
        checkAndResetIfOverLimit()
    }

    override fun resetLastAddedPoints() {
        lastAddedPoints.value = 0
        lastMilestone.value = null
        lastAchievement.value = null
    }

    override fun getPoints(): Int = currentPointsFloat.toInt()

    override fun getTotalEarnedPoints(): Int = earnedPoints

    override fun subtractPoints(amount: Int): Boolean {
        if (currentPointsFloat < amount) return false
        currentPointsFloat -= amount
        totalPoints.value = currentPointsFloat.toInt()
        return true
    }

    override fun activateMultiplier(multiplier: Float, durationMinutes: Int) {
        this.multiplier = multiplier
        this.multiplierEndMs = System.currentTimeMillis() + durationMinutes * 60_000L
        activeMultiplier.value = multiplier
    }

    override fun getMultiplierTimeLeft(): Long {
        val remaining = multiplierEndMs - System.currentTimeMillis()
        return if (remaining > 0) remaining / 60_000L else 0L
    }

    override fun getMultiplierTimeLeftInSeconds(): Long {
        val remaining = multiplierEndMs - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1_000L else 0L
    }

    override fun isMultiplierActive(): Boolean = multiplierEndMs > System.currentTimeMillis()

    fun getInflationDivisor(): Int = inflationDivisor

    private fun checkAndResetIfOverLimit() {
        if (currentPointsFloat > 100_000f) {
            currentPointsFloat = 0f
            inflationDivisor *= 10
        }
        totalPoints.value = currentPointsFloat.toInt()
    }
}
