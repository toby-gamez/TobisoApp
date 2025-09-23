package com.example.tobisoappnative

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 18)
        val minute = intent.getIntExtra("minute", 0)
        val notificationType = intent.getStringExtra("notification_type") ?: "today_events"
        
        val data = Data.Builder()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putString("notification_type", notificationType)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInputData(data)
            .build()
            
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}