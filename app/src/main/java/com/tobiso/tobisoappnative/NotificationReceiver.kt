package com.tobiso.tobisoappnative

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 17)
        val isCritical = intent.getBooleanExtra("critical", false)
        val data = Data.Builder()
            .putInt("hour", hour)
            .putBoolean("critical", isCritical)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

