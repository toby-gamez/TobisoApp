package com.example.tobisoappnative

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PointsManager {
    private const val PREFS_NAME = "points_prefs"
    private const val KEY_POINTS = "points"
    private var points: Int = 0
    private val _lastAddedPoints = MutableStateFlow(0)
    val lastAddedPoints: StateFlow<Int> = _lastAddedPoints
    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints
    private val _lastMilestone = MutableStateFlow<Int?>(null)
    val lastMilestone: StateFlow<Int?> = _lastMilestone

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        points = prefs.getInt(KEY_POINTS, 0)
        _totalPoints.value = points
    }

    fun addPoints(context: Context, amount: Int) {
        points += amount
        _lastAddedPoints.value = amount
        _totalPoints.value = points
        savePoints(context)
    }

    fun addPointsForMilestone(context: Context, amount: Int, milestoneDay: Int) {
        println("=== POINTS MANAGER DEBUG ===")
        println("Adding $amount points for milestone $milestoneDay days")
        println("Points before: $points")
        
        points += amount
        _lastAddedPoints.value = amount
        _lastMilestone.value = milestoneDay
        _totalPoints.value = points
        savePoints(context)
        
        println("Points after: $points")
        println("LastAddedPoints set to: $amount")
        println("LastMilestone set to: $milestoneDay")
        println("TotalPoints set to: $points")
        println("=== END POINTS MANAGER DEBUG ===")
    }

    private fun savePoints(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_POINTS, points).apply()
    }

    fun resetLastAddedPoints() {
        _lastAddedPoints.value = 0
        _lastMilestone.value = null
    }

    fun getPoints(): Int {
        return points
    }
}
