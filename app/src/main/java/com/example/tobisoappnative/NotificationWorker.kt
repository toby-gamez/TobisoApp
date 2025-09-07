package com.example.tobisoappnative

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val hour = inputData.getInt("hour", 17)
        val isCritical = inputData.getBoolean("critical", false)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val streakDays = getStreakDays(context)
        if (!streakDays.contains(today)) {
            showNotification(hour, isCritical)
        }
        return Result.success()
    }

    private fun showNotification(hour: Int, isCritical: Boolean) {
        val channelId = if (isCritical) "tobiso_app_channel_critical" else "tobiso_app_channel"
        val notificationId = if (isCritical) 2001 else 2000
        createNotificationChannel(channelId, isCritical)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isCritical) "JEŠTĚ SES NEUČIL" else "Kámo, je 5 a ty ses neučil")
            .setContentText(if (isCritical) "NECHOVEJ SE JAKO FREGMATIK A BEŽ SE VZDĚLÁVAT!" else "Chápu, že není večer, ale ted jsi nejčilejší, tak se do toho pust.")
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (isCritical) {
            builder.color = Color.RED
        }
        // Kontrola oprávnění pro notifikace (Android 13+)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (hasPermission) {
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (_: SecurityException) {
                // Oprávnění nebylo uděleno, notifikace nebude zobrazena
            }
        }
    }
    private fun createNotificationChannel(channelId: String, isCritical: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isCritical) "Tobiso App Critical" else "Tobiso App Channel"
            val descriptionText = if (isCritical) "Kritická upozornění na streak" else "Běžná upozornění na streak"
            val importance = if (isCritical) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                if (isCritical) {
                    enableLights(true)
                    lightColor = Color.RED
                }
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

// Pomocná funkce pro získání streaku (převzato z kódu StreakScreen)
fun getStreakDays(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("days", emptySet()) ?: emptySet()
}
