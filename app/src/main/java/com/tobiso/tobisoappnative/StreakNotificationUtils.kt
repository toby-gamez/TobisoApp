package com.tobiso.tobisoappnative

import android.content.Context
import androidx.work.WorkManager

fun cancelStreakNotifications(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag("streak_17")
    WorkManager.getInstance(context).cancelAllWorkByTag("streak_20")
}

