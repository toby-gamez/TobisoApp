package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.manager.IPointsManager
import com.tobiso.tobisoappnative.utils.checkPointsAchievements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PointsManager private constructor(context: Context) : IPointsManager {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var points: Int = 0
    private var totalEarnedPointsValue: Int = 0

    private val _lastAddedPoints = MutableStateFlow(0)
    override val lastAddedPoints: StateFlow<Int> = _lastAddedPoints

    private val _totalPoints = MutableStateFlow(0)
    override val totalPoints: StateFlow<Int> = _totalPoints

    private val _lastMilestone = MutableStateFlow<Int?>(null)
    override val lastMilestone: StateFlow<Int?> = _lastMilestone

    private val _lastAchievement = MutableStateFlow<Int?>(null)
    override val lastAchievement: StateFlow<Int?> = _lastAchievement

    private val _activeMultiplier = MutableStateFlow(1.0f)
    override val activeMultiplier: StateFlow<Float> = _activeMultiplier

    private val _totalEarnedPoints = MutableStateFlow(0)
    override val totalEarnedPoints: StateFlow<Int> = _totalEarnedPoints

    init {
        points = prefs.getInt(KEY_POINTS, 0)
        totalEarnedPointsValue = prefs.getInt(KEY_TOTAL_EARNED_POINTS, 0)
        if (points > totalEarnedPointsValue) {
            totalEarnedPointsValue = points
            prefs.edit().putInt(KEY_TOTAL_EARNED_POINTS, totalEarnedPointsValue).apply()
        }
        _totalPoints.update { points }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        checkActiveMultiplier()
    }

    override fun addPoints(amount: Int) {
        checkActiveMultiplier()
        val multipliedAmount = (amount * _activeMultiplier.value).toInt()
        points += multipliedAmount
        totalEarnedPointsValue += multipliedAmount
        _lastAddedPoints.update { multipliedAmount }
        _totalPoints.update { points }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        savePoints()
        saveTotalEarnedPoints()
        checkPointsAchievements(appContext)
    }

    override fun addPointsForMilestone(amount: Int, milestoneDay: Int) {
        points += amount
        totalEarnedPointsValue += amount
        _lastAddedPoints.update { amount }
        _lastMilestone.update { milestoneDay }
        _totalPoints.update { points }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        savePoints()
        saveTotalEarnedPoints()
    }

    override fun addPointsForAchievement(amount: Int, achievementPoints: Int) {
        points += amount
        totalEarnedPointsValue += amount
        _lastAddedPoints.update { amount }
        _lastAchievement.update { achievementPoints }
        _totalPoints.update { points }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        savePoints()
        saveTotalEarnedPoints()
    }

    override fun resetLastAddedPoints() {
        _lastAddedPoints.update { 0 }
        _lastMilestone.update { null }
        _lastAchievement.update { null }
    }

    override fun getPoints(): Int = points

    override fun getTotalEarnedPoints(): Int = totalEarnedPointsValue

    override fun subtractPoints(amount: Int): Boolean {
        if (points < amount) return false
        points -= amount
        _totalPoints.update { points }
        savePoints()
        return true
    }

    override fun activateMultiplier(multiplier: Float, durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit()
            .putFloat(KEY_MULTIPLIER, multiplier)
            .putLong(KEY_MULTIPLIER_END, endTime)
            .apply()
        _activeMultiplier.update { multiplier }
    }

    override fun getMultiplierTimeLeft(): Long {
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        return if (endTime > currentTime) (endTime - currentTime) / (60 * 1000) else 0
    }

    override fun getMultiplierTimeLeftInSeconds(): Long {
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        return if (endTime > currentTime) (endTime - currentTime) / 1000 else 0
    }

    override fun isMultiplierActive(): Boolean {
        checkActiveMultiplier()
        return _activeMultiplier.value > 1.0f
    }

    private fun savePoints() {
        prefs.edit().putInt(KEY_POINTS, points).apply()
    }

    private fun saveTotalEarnedPoints() {
        prefs.edit().putInt(KEY_TOTAL_EARNED_POINTS, totalEarnedPointsValue).apply()
    }

    private fun checkActiveMultiplier() {
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        if (endTime > currentTime) {
            _activeMultiplier.update { prefs.getFloat(KEY_MULTIPLIER, 1.0f) }
        } else {
            _activeMultiplier.update { 1.0f }
            prefs.edit().remove(KEY_MULTIPLIER).remove(KEY_MULTIPLIER_END).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "points_prefs"
        private const val KEY_POINTS = "points"
        private const val KEY_TOTAL_EARNED_POINTS = "total_earned_points"
        private const val KEY_MULTIPLIER = "active_multiplier"
        private const val KEY_MULTIPLIER_END = "multiplier_end_time"

        @Volatile private var INSTANCE: PointsManager? = null

        val instance: PointsManager
            get() = INSTANCE ?: error("PointsManager.initialize() must be called before use")

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = PointsManager(context.applicationContext)
                    }
                }
            }
        }
    }
}
