package com.tobiso.tobisoappnative

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PetReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val channelId = "tobiso_pet_channel"
        createNotificationChannel(channelId)

        val equippedPetId = context.getSharedPreferences("backpack_prefs", Context.MODE_PRIVATE)
            .getInt("equipped_pet", -1)

        if (equippedPetId == -1) return Result.success()

        if (!PetManager.isPetInitialized(equippedPetId) || PetManager.isPetDead(equippedPetId)) return Result.success()

        val now = System.currentTimeMillis()
        val hourMs = 60 * 60 * 1000L
        val lastFed = PetManager.getLastFedTime(equippedPetId)
        val lastWatered = PetManager.getLastWateredTime(equippedPetId)

        val hoursSinceFed = (now - lastFed) / hourMs
        val hoursSinceWatered = (now - lastWatered) / hourMs

        val thirstWarning = PetManager.THIRST_WARNING_HOURS
        val hungerWarning = PetManager.HUNGER_WARNING_HOURS

        var title = ""
        var message = ""

        when {
            hoursSinceWatered >= thirstWarning && hoursSinceFed >= hungerWarning -> {
                title = "Tvé zvířátko má hlad a žízeň! 🐾"
                message = "Nakrm a napoj ho, jinak zemře!"
            }
            hoursSinceWatered >= thirstWarning -> {
                title = "Tvé zvířátko má žízeň! 💧"
                message = "Napoj ho, než bude pozdě!"
            }
            hoursSinceFed >= hungerWarning -> {
                title = "Tvé zvířátko má hlad! 🍖"
                message = "Nakrm ho, než bude pozdě!"
            }
        }

        if (title.isEmpty()) return Result.success()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(3001, builder.build())
                }
            } catch (_: SecurityException) {}
        }

        return Result.success()
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Péče o zvířátko"
            val descriptionText = "Připomínky na krmení a napájení zvířátka"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
