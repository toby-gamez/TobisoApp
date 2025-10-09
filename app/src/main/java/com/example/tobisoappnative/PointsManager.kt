package com.example.tobisoappnative

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PointsManager {
    private const val PREFS_NAME = "points_prefs"
    private const val KEY_POINTS = "points"
    private const val KEY_TOTAL_EARNED_POINTS = "total_earned_points"
    private const val KEY_MULTIPLIER = "active_multiplier"
    private const val KEY_MULTIPLIER_END = "multiplier_end_time"
    
    private var points: Int = 0
    private var totalEarnedPointsValue: Int = 0
    private val _lastAddedPoints = MutableStateFlow(0)
    val lastAddedPoints: StateFlow<Int> = _lastAddedPoints
    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints
    private val _lastMilestone = MutableStateFlow<Int?>(null)
    val lastMilestone: StateFlow<Int?> = _lastMilestone
    private val _activeMultiplier = MutableStateFlow(1.0f)
    val activeMultiplier: StateFlow<Float> = _activeMultiplier
    private val _totalEarnedPoints = MutableStateFlow(0)
    val totalEarnedPoints: StateFlow<Int> = _totalEarnedPoints

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        points = prefs.getInt(KEY_POINTS, 0)
        totalEarnedPointsValue = prefs.getInt(KEY_TOTAL_EARNED_POINTS, 0)
        
        // Pokud jsou současné body větší než totalEarnedPointsValue, nastaví se totalEarnedPointsValue na points
        if (points > totalEarnedPointsValue) {
            totalEarnedPointsValue = points
            saveTotalEarnedPoints(context)
        }
        
        _totalPoints.value = points
        _totalEarnedPoints.value = totalEarnedPointsValue
        
        // Kontrola aktivního multiplikátoru
        checkActiveMultiplier(context)
    }

    fun addPoints(context: Context, amount: Int) {
        // Kontrola aktivního multiplikátoru
        checkActiveMultiplier(context)
        
        // Aplikace multiplikátoru
        val multipliedAmount = (amount * _activeMultiplier.value).toInt()
        
        points += multipliedAmount
        totalEarnedPointsValue += multipliedAmount
        _lastAddedPoints.value = multipliedAmount
        _totalPoints.value = points
        _totalEarnedPoints.value = totalEarnedPointsValue
        savePoints(context)
        saveTotalEarnedPoints(context)
    }

    fun addPointsForMilestone(context: Context, amount: Int, milestoneDay: Int) {
        println("=== POINTS MANAGER DEBUG ===")
        println("Adding $amount points for milestone $milestoneDay days")
        println("Points before: $points")
        
        points += amount
        totalEarnedPointsValue += amount
        _lastAddedPoints.value = amount
        _lastMilestone.value = milestoneDay
        _totalPoints.value = points
        _totalEarnedPoints.value = totalEarnedPointsValue
        savePoints(context)
        saveTotalEarnedPoints(context)
        
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
    
    private fun saveTotalEarnedPoints(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TOTAL_EARNED_POINTS, totalEarnedPointsValue).apply()
    }

    fun resetLastAddedPoints() {
        _lastAddedPoints.value = 0
        _lastMilestone.value = null
    }

    fun getPoints(): Int {
        return points
    }
    
    fun getTotalEarnedPoints(): Int {
        return totalEarnedPointsValue
    }
    
    fun subtractPoints(context: Context, amount: Int): Boolean {
        if (points < amount) {
            return false // Nedostatek bodů
        }
        
        points -= amount
        _totalPoints.value = points
        savePoints(context)
        return true
    }
    
    // Aktivace multiplikátoru bodů
    fun activateMultiplier(context: Context, multiplier: Float, durationMinutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        
        prefs.edit()
            .putFloat(KEY_MULTIPLIER, multiplier)
            .putLong(KEY_MULTIPLIER_END, endTime)
            .apply()
            
        _activeMultiplier.value = multiplier
    }
    
    // Kontrola, zda je multiplikátor stále aktivní
    private fun checkActiveMultiplier(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        
        if (endTime > currentTime) {
            // Multiplikátor je stále aktivní
            val multiplier = prefs.getFloat(KEY_MULTIPLIER, 1.0f)
            _activeMultiplier.value = multiplier
        } else {
            // Multiplikátor vypršel
            _activeMultiplier.value = 1.0f
            prefs.edit()
                .remove(KEY_MULTIPLIER)
                .remove(KEY_MULTIPLIER_END)
                .apply()
        }
    }
    
    // Získání zbývajícího času multiplikátoru v minutách
    fun getMultiplierTimeLeft(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        
        return if (endTime > currentTime) {
            (endTime - currentTime) / (60 * 1000) // vrátí v minutách
        } else {
            0
        }
    }
    
    // Získání zbývajícího času multiplikátoru v sekundách pro přesné zobrazení
    fun getMultiplierTimeLeftInSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_MULTIPLIER_END, 0)
        val currentTime = System.currentTimeMillis()
        
        return if (endTime > currentTime) {
            (endTime - currentTime) / 1000 // vrátí v sekundách
        } else {
            0
        }
    }
    
    // Kontrola, zda je multiplikátor aktivní
    fun isMultiplierActive(context: Context): Boolean {
        checkActiveMultiplier(context)
        return _activeMultiplier.value > 1.0f
    }
}
