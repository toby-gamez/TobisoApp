package com.tobiso.tobisoappnative.utils
import timber.log.Timber

import android.content.Context
import com.tobiso.tobisoappnative.PointsManager

fun generatePointsAchievements(): Map<Int, Int> {
    return mapOf(
        10 to 5,      // 10 bodů -> 5 bodů odměna
        20 to 5,
        50 to 10,
        75 to 10,
        100 to 15,
        150 to 15,
        200 to 15,
        300 to 20,
        400 to 20,
        500 to 25,
        600 to 25,
        700 to 25,
        800 to 30,
        900 to 30,
        1000 to 50,   // speciální odměna za 1000
        1500 to 75,
        2000 to 100,
        3000 to 150,
        4000 to 200,
        5000 to 250,
        7500 to 300,
        10000 to 500,
        15000 to 400,
        20000 to 600,
        25000 to 500,
        30000 to 700,
        40000 to 800,
        50000 to 1000,
        60000 to 900,
        75000 to 1200,
        100000 to 2000
    )
}

fun checkPointsAchievements(context: Context) {
    val totalEarnedPoints = PointsManager.instance.getTotalEarnedPoints()
    val achievements = generatePointsAchievements()

    val achievementsPrefs = context.getSharedPreferences("points_achievements", Context.MODE_PRIVATE)

    var newAchievementsFound = false
    achievements.forEach { (requiredPoints, rewardPoints) ->
        if (totalEarnedPoints >= requiredPoints) {
            val achievementKey = "achievement_$requiredPoints"
            val isAlreadyAchieved = achievementsPrefs.getBoolean(achievementKey, false)

            if (!isAlreadyAchieved) {
                newAchievementsFound = true
                Timber.d("🏆 NEW ACHIEVEMENT UNLOCKED: $requiredPoints points - awarding $rewardPoints points")

                PointsManager.instance.addPointsForAchievement(rewardPoints, requiredPoints)
                achievementsPrefs.edit().putBoolean(achievementKey, true).apply()
            }
        }
    }
}
