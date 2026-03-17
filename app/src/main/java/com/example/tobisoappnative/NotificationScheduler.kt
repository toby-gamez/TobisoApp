package com.example.tobisoappnative

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Zodpovídá za plánování všech notifikačních alarmů aplikace.
 */
object NotificationScheduler {

    fun scheduleAll(context: Context) {
        scheduleNotification(context, 17, 0, false) // běžná notifikace v 17:00
        scheduleNotification(context, 20, 0, true)  // kritická notifikace ve 20:00
        scheduleEventNotification(context, 18, 0, "tomorrow_events") // notifikace v 18:00 pro zítřejší události
        scheduleEventNotification(context, 6, 30, "today_events")    // notifikace v 6:30 pro dnešní události
    }

    fun scheduleNotification(context: Context, hour: Int, minute: Int, isCritical: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("hour", hour)
            putExtra("critical", isCritical)
        }
        val requestCode = if (isCritical) 2001 else 2000
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleEventNotification(context: Context, hour: Int, minute: Int, notificationType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("notification_type", notificationType)
        }
        val requestCode = when (notificationType) {
            "tomorrow_events" -> 3000
            "today_events" -> 3001
            else -> 3999
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
