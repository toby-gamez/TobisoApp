package com.tobiso.tobisoappnative.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tobiso.tobisoappnative.PointsManager
import kotlinx.coroutines.delay

/**
 * Observes [PointsManager] and shows the full-screen celebration overlays (points, streak milestone, achievement,
 * prestige tier, points reset). Only one is visible at a time, highest priority first.
 */
@Composable
fun PointsOverlayHost() {
    val lastAddedPoints by PointsManager.instance.lastAddedPoints.collectAsStateWithLifecycle()
    val lastMilestone by PointsManager.instance.lastMilestone.collectAsStateWithLifecycle()
    val lastAchievement by PointsManager.instance.lastAchievement.collectAsStateWithLifecycle()
    val lastPrestigeTierPoints by PointsManager.instance.lastPrestigeTierPoints.collectAsStateWithLifecycle()
    val lastPointsReset by PointsManager.instance.lastPointsReset.collectAsStateWithLifecycle()
    var showOverlay by remember { mutableStateOf(false) }
    var overlayPoints by remember { mutableStateOf(0) }
    var showMilestoneOverlay by remember { mutableStateOf(false) }
    var showAchievementOverlay by remember { mutableStateOf(false) }
    var showPrestigeOverlay by remember { mutableStateOf(false) }
    var showResetOverlay by remember { mutableStateOf(false) }
    var resetDeflationDivisor by remember { mutableStateOf(10) }
    var milestoneDay by remember { mutableStateOf(0) }
    var achievementPoints by remember { mutableStateOf(0) }
    var prestigeTierForOverlay by remember { mutableStateOf(PrestigeTier.NONE) }

    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()
    val totalEarnedPoints by PointsManager.instance.totalEarnedPoints.collectAsStateWithLifecycle()

    // LaunchedEffect pro milník overlay (priorita)
    LaunchedEffect(lastMilestone) {
        val capturedMilestone = lastMilestone ?: return@LaunchedEffect
        val capturedPoints = lastAddedPoints

        overlayPoints = capturedPoints
        milestoneDay = capturedMilestone
        showMilestoneOverlay = true
        delay(3000)
        showMilestoneOverlay = false
        PointsManager.instance.resetLastAddedPoints()
    }

    // LaunchedEffect pro achievement overlay (priorita)
    LaunchedEffect(lastAchievement) {
        val capturedAchievement = lastAchievement ?: return@LaunchedEffect
        val capturedPoints = lastAddedPoints

        overlayPoints = capturedPoints
        achievementPoints = capturedAchievement
        showAchievementOverlay = true
        delay(3000)
        showAchievementOverlay = false
        PointsManager.instance.resetLastAddedPoints()
    }

    // LaunchedEffect pro reset bodů (absolutní nejvyšší priorita)
    LaunchedEffect(lastPointsReset) {
        val divisor = lastPointsReset ?: return@LaunchedEffect
        resetDeflationDivisor = divisor
        showResetOverlay = true
        delay(5200)
        showResetOverlay = false
        PointsManager.instance.resetLastPointsReset()
    }

    // LaunchedEffect pro prestige tier overlay (nejvyšší priorita)
    LaunchedEffect(lastPrestigeTierPoints) {
        val threshold = lastPrestigeTierPoints ?: return@LaunchedEffect
        val tier = getPrestigeTier(threshold)
        if (tier == PrestigeTier.NONE) return@LaunchedEffect
        prestigeTierForOverlay = tier
        showPrestigeOverlay = true
        delay(5000)
        showPrestigeOverlay = false
        PointsManager.instance.resetLastPrestigeTier()
    }

    // LaunchedEffect pro běžný overlay (body s přičítáním)
    LaunchedEffect(lastAddedPoints) {
        if (lastAddedPoints == 0 || lastMilestone != null || lastAchievement != null) return@LaunchedEffect
        val capturedPoints = lastAddedPoints

        overlayPoints = capturedPoints
        showOverlay = true
        delay(2500)
        showOverlay = false
        PointsManager.instance.resetLastAddedPoints()
    }

    // Overlays pro body, milníky, achievementy, prestiž a reset
                    if (showResetOverlay) {
                        FullScreenPointsResetOverlay(newDeflationDivisor = resetDeflationDivisor)
                    } else if (showPrestigeOverlay) {
                        FullScreenPrestigeTierOverlay(
                            tier = prestigeTierForOverlay,
                            totalEarnedPoints = totalEarnedPoints
                        )
                    } else if (showMilestoneOverlay) {
                        FullScreenMilestoneOverlay(
                            points = overlayPoints,
                            totalPointsFloat = totalPointsFloat,
                            milestoneDay = milestoneDay
                        )
                    } else if (showAchievementOverlay) {
                        FullScreenAchievementOverlay(
                            points = overlayPoints,
                            totalPointsFloat = totalPointsFloat,
                            achievementPoints = achievementPoints
                        )
                    } else if (showOverlay) {
                        FullScreenPointsOverlay(
                            points = overlayPoints,
                            totalPointsFloat = totalPointsFloat
                        )
                    }
}
