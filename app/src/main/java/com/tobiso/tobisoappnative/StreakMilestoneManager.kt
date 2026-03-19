package com.tobiso.tobisoappnative

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.tobiso.tobisoappnative.utils.StreakUtils

/**
 * Zodpovídá za kontrolu a udělování odměn za streak milníky.
 */
object StreakMilestoneManager {

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkStreakMilestones(context: Context) {
        val streakInfo = StreakUtils.calculateStreaks(context)
        val currentStreak = streakInfo.currentStreak

        println("=== STREAK MILESTONES DEBUG ===")
        println("Current streak: $currentStreak days (včetně freezes)")
        println("Max streak: ${streakInfo.maxStreak} days")

        val allMilestones = generateStreakMilestones(currentStreak)

        val milestonesPrefs = context.getSharedPreferences("streak_milestones", Context.MODE_PRIVATE)

        val achievedMilestones = milestonesPrefs.all.keys.filter { it.startsWith("milestone_") }
        println("Already achieved milestones: $achievedMilestones")

        var newMilestonesFound = false
        allMilestones.forEach { (days, points) ->
            if (currentStreak >= days) {
                val milestoneKey = "milestone_$days"
                val isAlreadyAchieved = milestonesPrefs.getBoolean(milestoneKey, false)

                println("Checking milestone $days days: already achieved = $isAlreadyAchieved")

                if (!isAlreadyAchieved) {
                    newMilestonesFound = true
                    println("🎉 NEW MILESTONE ACHIEVED: $days days - awarding $points points")

                    PointsManager.addPointsForMilestone(points, days)
                    milestonesPrefs.edit().putBoolean(milestoneKey, true).apply()

                    println("Points added and milestone marked as achieved")
                } else {
                    println("Milestone $days already achieved, skipping")
                }
            }
        }

        if (!newMilestonesFound) {
            println("No new milestones found for current streak: $currentStreak")
        }
        println("=== END STREAK MILESTONES DEBUG ===")
    }

    private fun generateStreakMilestones(maxStreak: Int): Map<Int, Int> {
        val milestones = mutableMapOf<Int, Int>()

        val specialMilestones = mapOf(
            7 to 15,
            14 to 15,
            30 to 15,
            60 to 15,
            100 to 15,
            183 to 30,
            365 to 50,
            548 to 30,
            730 to 100,
            913 to 75,
            1095 to 150,
            1460 to 200,
            1826 to 250
        )

        milestones.putAll(specialMilestones)

        var current = 25
        while (current <= maxStreak + 100) {
            if (!specialMilestones.containsKey(current)) {
                milestones[current] = 15
            }
            current += 25
        }

        return milestones.toSortedMap()
    }
}
