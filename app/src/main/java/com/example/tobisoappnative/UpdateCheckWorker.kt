package com.example.tobisoappnative

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val currentVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        val latestVersion = try {
            fetchLatestVersionFromGithub()
        } catch (e: Exception) {
            null
        }
        if (latestVersion != null && latestVersion != currentVersion) {
            sendUpdateNotification(latestVersion)
        }
        return Result.success()
    }

    private fun sendUpdateNotification(latestVersion: String) {
        val channelId = "update_check_channel"
        createNotificationChannel(channelId)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Dostupná nová verze aplikace")
            .setContentText("Je dostupná verze $latestVersion. Otevři aplikaci pro aktualizaci.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33) {
            NotificationManagerCompat.from(context).notify(3000, builder.build())
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Update Check"
            val descriptionText = "Oznámení o nové verzi aplikace"
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

suspend fun fetchLatestVersionFromGithub(): String? = withContext(Dispatchers.IO) {
    val url = URL("https://api.github.com/repos/toby-gamez/TobisoAppNative/releases/latest")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    connection.connect()
    if (connection.responseCode != 200) {
        return@withContext null
    }
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(response)
    val tag = json.getString("tag_name")
    return@withContext tag.removePrefix("v")
}

