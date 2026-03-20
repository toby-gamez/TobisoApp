package com.tobiso.tobisoappnative.fake

import com.tobiso.tobisoappnative.manager.IPointsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory fake implementation of [IPointsManager] for unit tests.
 * Has no Android/SharedPreferences dependencies.
 */
class FakePointsManager : IPointsManager {

    private var currentPoints = 0
    private var earnedPoints = 0
    private var multiplier = 1.0f
    private var multiplierEndMs = 0L

    override val totalPoints = MutableStateFlow(0)
    override val lastAddedPoints = MutableStateFlow(0)
    override val lastMilestone = MutableStateFlow<Int?>(null)
    override val lastAchievement = MutableStateFlow<Int?>(null)
    override val activeMultiplier = MutableStateFlow(1.0f)
    override val totalEarnedPoints = MutableStateFlow(0)

    override fun addPoints(amount: Int) {
        val added = (amount * multiplier).toInt()
        currentPoints += added
        earnedPoints += added
        lastAddedPoints.value = added
        totalPoints.value = currentPoints
        totalEarnedPoints.value = earnedPoints
    }

    override fun addPointsForMilestone(amount: Int, milestoneDay: Int) {
        currentPoints += amount
        earnedPoints += amount
        lastAddedPoints.value = amount
        lastMilestone.value = milestoneDay
        totalPoints.value = currentPoints
        totalEarnedPoints.value = earnedPoints
    }

    override fun addPointsForAchievement(amount: Int, achievementPoints: Int) {
        currentPoints += amount
        earnedPoints += amount
        lastAddedPoints.value = amount
        lastAchievement.value = achievementPoints
        totalPoints.value = currentPoints
        totalEarnedPoints.value = earnedPoints
    }

    override fun resetLastAddedPoints() {
        lastAddedPoints.value = 0
        lastMilestone.value = null
        lastAchievement.value = null
    }

    override fun getPoints(): Int = currentPoints

    override fun getTotalEarnedPoints(): Int = earnedPoints

    override fun subtractPoints(amount: Int): Boolean {
        if (currentPoints < amount) return false
        currentPoints -= amount
        totalPoints.value = currentPoints
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
}
