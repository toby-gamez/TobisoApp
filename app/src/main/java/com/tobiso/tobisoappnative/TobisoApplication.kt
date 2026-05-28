package com.tobiso.tobisoappnative

import android.app.Application
import com.tobiso.tobisoappnative.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TobisoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        installCrashHandler()
        reportPreviousCrash()

        try {
            com.tobiso.tobisoappnative.config.SecurityConfig.initialize(this)
            PointsManager.initialize(this)
            StreakFreezeManager.initialize(this)
            ShopManager.initialize(this)
            IconPackManager.initialize(this)
            BackpackManager.initialize(this)
            AiCreditManager.initialize(this)
            QuestionProgressManager.initialize(this)
        } catch (e: Exception) {
            Timber.e(e, "Critical initialization error – app may not function correctly")
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                getSharedPreferences("crash_prefs", MODE_PRIVATE).edit()
                    .putString("last_crash", throwable.stackTraceToString())
                    .putString("last_crash_type", "${throwable.javaClass.name}: ${throwable.message}")
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun reportPreviousCrash() {
        try {
            val prefs = getSharedPreferences("crash_prefs", MODE_PRIVATE)
            val lastCrash = prefs.getString("last_crash", null) ?: return
            val lastCrashType = prefs.getString("last_crash_type", "Unknown crash") ?: "Unknown crash"
            val lastCrashTime = prefs.getLong("last_crash_time", 0L)
            Timber.w("Previous crash at %s: %s", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(lastCrashTime), lastCrashType)
            Timber.w("Stack trace:\n%s", lastCrash)
            prefs.edit().remove("last_crash").remove("last_crash_type").remove("last_crash_time").apply()
        } catch (_: Exception) {}
    }
}
