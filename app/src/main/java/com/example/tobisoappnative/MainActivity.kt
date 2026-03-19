package com.example.tobisoappnative

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.work.*
import com.example.tobisoappnative.screens.addTodayToStreak
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        recordAppOpen()

        // DŮLEŽITÉ: Kontrola freeze PŘED přidáním dnešního dne
        StreakFreezeManager.checkAndAutoUseFreeze()

        // Přidat dnešní den do řady (pokud už tam není)
        addTodayToStreak(this)

        val navigateTo = intent?.getStringExtra("navigate_to")
        setContent {
            TobisoApp(navigateTo = navigateTo)
        }

        NotificationScheduler.scheduleAll(this)

        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "update_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }

    private fun recordAppOpen() {
        val prefs = getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString("last_opened_date", today).apply()
    }
}
