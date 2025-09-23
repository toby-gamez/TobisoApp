package com.example.tobisoappnative

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Pomocná třída pro testování notifikací
 */
object NotificationTestHelper {
    
    /**
     * Testuje notifikaci pro dnešní události
     */
    fun testTodayEventsNotification(context: Context) {
        val data = Data.Builder()
            .putInt("hour", 6)
            .putInt("minute", 30)
            .putString("notification_type", "today_events")
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInputData(data)
            .build()
            
        WorkManager.getInstance(context).enqueue(workRequest)
    }
    
    /**
     * Testuje notifikaci pro zítřejší události  
     */
    fun testTomorrowEventsNotification(context: Context) {
        val data = Data.Builder()
            .putInt("hour", 18)
            .putInt("minute", 0)
            .putString("notification_type", "tomorrow_events")
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInputData(data)
            .build()
            
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}