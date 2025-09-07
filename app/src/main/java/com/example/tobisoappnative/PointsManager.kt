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

    private fun savePoints(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_POINTS, points).apply()
    }

    fun resetLastAddedPoints() {
        _lastAddedPoints.value = 0
    }

    fun getPoints(): Int {
        return points
    }
}
