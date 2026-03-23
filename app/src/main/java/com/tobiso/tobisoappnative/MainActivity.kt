package com.tobiso.tobisoappnative

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.*
import com.tobiso.tobisoappnative.screens.addTodayToStreak
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Timber.w("POST_NOTIFICATIONS permission denied – push notifications will not work")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun recordAppOpen() {
        val prefs = getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDate.now().toString()
        } else {
            val calendar = java.util.Calendar.getInstance()
            "%04d-%02d-%02d".format(
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
        prefs.edit().putString("last_opened_date", today).apply()
    }
}
