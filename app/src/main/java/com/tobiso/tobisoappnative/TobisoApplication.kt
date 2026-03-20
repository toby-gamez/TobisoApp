package com.tobiso.tobisoappnative

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.tobiso.tobisoappnative.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TobisoApplication : Application() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // SecurityConfig musí být inicializován jako první – ostatní třídy ho mohou potřebovat.
        com.tobiso.tobisoappnative.config.SecurityConfig.initialize(this)
        // Initialization order matters: StreakFreezeManager before ShopManager,
        // ShopManager before BackpackManager (BackpackManager reads ShopManager state).
        PointsManager.initialize(this)
        StreakFreezeManager.initialize(this)
        ShopManager.initialize(this)
        IconPackManager.initialize(this)
        BackpackManager.initialize(this)
    }
}
