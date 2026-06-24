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

    private var pointsFloat: Float = 0f
    private var totalEarnedPointsValue: Int = 0
    private var deflationDivisor: Int = 1

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
        deflationDivisor = prefs.getInt(KEY_DEFLATION_DIVISOR, 1)

        val storedFloat = prefs.getFloat(KEY_POINTS_FLOAT, -1f)
        if (storedFloat >= 0f) {
            pointsFloat = storedFloat
        } else {
            // Migrate from old Int-based storage
            pointsFloat = prefs.getInt(KEY_POINTS_LEGACY, 0).toFloat()
            prefs.edit().putFloat(KEY_POINTS_FLOAT, pointsFloat).apply()
        }

        totalEarnedPointsValue = prefs.getInt(KEY_TOTAL_EARNED_POINTS, 0)
        if (pointsFloat > totalEarnedPointsValue) {
            totalEarnedPointsValue = pointsFloat.toInt()
            prefs.edit().putInt(KEY_TOTAL_EARNED_POINTS, totalEarnedPointsValue).apply()
        }
        _totalPoints.update { pointsFloat.toInt() }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        checkActiveMultiplier()
    }

    override fun addPoints(amount: Int) {
        checkActiveMultiplier()
        val deflated = applyDeflation(amount) * _activeMultiplier.value
        pointsFloat += deflated
        totalEarnedPointsValue += deflated.toInt()
        _lastAddedPoints.update { deflated.toInt() }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        saveTotalEarnedPoints()
        checkAndResetIfOverLimit()
        checkPointsAchievements(appContext)
    }

    override fun addPointsForMilestone(amount: Int, milestoneDay: Int) {
        val deflated = applyDeflation(amount)
        pointsFloat += deflated
        totalEarnedPointsValue += deflated.toInt()
        _lastAddedPoints.update { deflated.toInt() }
        _lastMilestone.update { milestoneDay }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        saveTotalEarnedPoints()
        checkAndResetIfOverLimit()
    }

    override fun addPointsForAchievement(amount: Int, achievementPoints: Int) {
        val deflated = applyDeflation(amount)
        pointsFloat += deflated
        totalEarnedPointsValue += deflated.toInt()
        _lastAddedPoints.update { deflated.toInt() }
        _lastAchievement.update { achievementPoints }
        _totalEarnedPoints.update { totalEarnedPointsValue }
        saveTotalEarnedPoints()
        checkAndResetIfOverLimit()
    }

    override fun resetLastAddedPoints() {
        _lastAddedPoints.update { 0 }
        _lastMilestone.update { null }
        _lastAchievement.update { null }
    }

    override fun getPoints(): Int = pointsFloat.toInt()

    override fun getTotalEarnedPoints(): Int = totalEarnedPointsValue

    override fun subtractPoints(amount: Int): Boolean {
        if (pointsFloat < amount) return false
        pointsFloat -= amount
        _totalPoints.update { pointsFloat.toInt() }
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

    private fun applyDeflation(amount: Int): Float = amount.toFloat() / deflationDivisor

    private fun checkAndResetIfOverLimit() {
        if (pointsFloat > 100_000f) {
            pointsFloat = 0f
            deflationDivisor *= 10
            savePoints()
            saveDeflationDivisor()
        }
        _totalPoints.update { pointsFloat.toInt() }
        savePoints()
    }

    private fun savePoints() {
        prefs.edit().putFloat(KEY_POINTS_FLOAT, pointsFloat).apply()
    }

    private fun saveTotalEarnedPoints() {
        prefs.edit().putInt(KEY_TOTAL_EARNED_POINTS, totalEarnedPointsValue).apply()
    }

    private fun saveDeflationDivisor() {
        prefs.edit().putInt(KEY_DEFLATION_DIVISOR, deflationDivisor).apply()
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
        private const val KEY_POINTS_LEGACY = "points"
        private const val KEY_POINTS_FLOAT = "points_float"
        private const val KEY_TOTAL_EARNED_POINTS = "total_earned_points"
        private const val KEY_MULTIPLIER = "active_multiplier"
        private const val KEY_MULTIPLIER_END = "multiplier_end_time"
        private const val KEY_DEFLATION_DIVISOR = "deflation_divisor"

        lateinit var instance: PointsManager
            private set

        fun initialize(context: Context) {
            instance = PointsManager(context.applicationContext)
        }
    }
}
