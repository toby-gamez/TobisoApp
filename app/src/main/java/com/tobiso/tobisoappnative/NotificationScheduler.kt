package com.tobiso.tobisoappnative

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        schedulePeriodicNotification(context, 17, 0, isCritical = false, "notification_17")
        schedulePeriodicNotification(context, 20, 0, isCritical = true, "notification_20")
        schedulePeriodicEventNotification(context, 18, 0, "tomorrow_events", "events_tomorrow")
        schedulePeriodicEventNotification(context, 6, 30, "today_events", "events_today")
    }

    private fun schedulePeriodicNotification(
        context: Context, hour: Int, minute: Int, isCritical: Boolean, uniqueName: String
    ) {
        val initialDelay = calculateInitialDelay(hour, minute)
        val data = Data.Builder()
            .putInt("hour", hour)
            .putBoolean("critical", isCritical)
            .build()
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun schedulePeriodicEventNotification(
        context: Context, hour: Int, minute: Int, notificationType: String, uniqueName: String
    ) {
        val initialDelay = calculateInitialDelay(hour, minute)
        val data = Data.Builder()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putString("notification_type", notificationType)
            .build()
        val request = PeriodicWorkRequestBuilder<EventNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
