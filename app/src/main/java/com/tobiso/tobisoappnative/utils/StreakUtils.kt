package com.tobiso.tobisoappnative.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.tobiso.tobisoappnative.StreakFreezeManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Centralizované utility pro práci se streak
 * Zahrnuje podporu pro Streak Freeze
 */
object StreakUtils {
    
    /**
     * Získá všechny streak dny (aktivita + freeze dny)
     */
    fun getStreakDays(context: Context): Set<String> {
        val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
    }
    
    /**
     * Získá všechny aktivní dny včetně freeze dnů
     */
    fun getAllActiveDays(context: Context): Set<String> {
        val streakDays = getStreakDays(context)
        val freezeDays = StreakFreezeManager.getUsedFreezes()
        return (streakDays + freezeDays).toSet()
    }
    
    /**
     * Vypočítá aktuální streak (včetně freeze dnů)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentStreak(context: Context): Int {
        val allActiveDays = getAllActiveDays(context)
        
        if (allActiveDays.isEmpty()) {
            return 0
        }

        val sortedDates = allActiveDays.map { LocalDate.parse(it) }.sorted()
        
        var currentStreak = 0
        val today = LocalDate.now()
        val lastRecordedDay = sortedDates.last()

        if (lastRecordedDay == today || lastRecordedDay == today.minusDays(1)) {
            var expectedDate = lastRecordedDay
            for (i in sortedDates.indices.reversed()) {
                if (sortedDates[i] == expectedDate) {
                    currentStreak++
                    expectedDate = expectedDate.minusDays(1)
                } else {
                    break
                }
            }
        }

        return currentStreak
    }
    
    /**
     * Vypočítá maximální streak (včetně freeze dnů)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMaxStreak(context: Context): Int {
        val allActiveDays = getAllActiveDays(context)
        
        if (allActiveDays.isEmpty()) {
            return 0
        }

        val sortedDates = allActiveDays.map { LocalDate.parse(it) }.sorted()

        if (sortedDates.size == 1) {
            return 1
        }

        var maxStreak = 1
        var runningStreak = 1

        for (i in 1 until sortedDates.size) {
            if (sortedDates[i].minusDays(1) == sortedDates[i - 1]) {
                runningStreak++
            } else {
                runningStreak = 1
            }
            if (runningStreak > maxStreak) {
                maxStreak = runningStreak
            }
        }

        return maxStreak
    }
    
    /**
     * Data class pro streak info
     */
    data class StreakInfo(val currentStreak: Int, val maxStreak: Int)
    
    /**
     * Vypočítá aktuální i maximální streak najednou
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateStreaks(context: Context): StreakInfo {
        val allActiveDays = getAllActiveDays(context)
        
        if (allActiveDays.isEmpty()) {
            return StreakInfo(0, 0)
        }

        val sortedDates = allActiveDays.map { LocalDate.parse(it) }.sorted()

        if (sortedDates.size == 1) {
            return StreakInfo(1, 1)
        }

        // Výpočet max streak
        var maxStreak = 1
        var runningStreak = 1

        for (i in 1 until sortedDates.size) {
            if (sortedDates[i].minusDays(1) == sortedDates[i - 1]) {
                runningStreak++
            } else {
                runningStreak = 1
            }
            if (runningStreak > maxStreak) {
                maxStreak = runningStreak
            }
        }

        // Výpočet current streak
        var currentStreak = 0
        val today = LocalDate.now()
        val lastRecordedDay = sortedDates.last()

        if (lastRecordedDay == today || lastRecordedDay == today.minusDays(1)) {
            var expectedDate = lastRecordedDay
            for (i in sortedDates.indices.reversed()) {
                if (sortedDates[i] == expectedDate) {
                    currentStreak++
                    expectedDate = expectedDate.minusDays(1)
                } else {
                    break
                }
            }
        }

        return StreakInfo(currentStreak = currentStreak, maxStreak = maxStreak)
    }
}