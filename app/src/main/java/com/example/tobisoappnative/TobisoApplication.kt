package com.example.tobisoappnative

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi

class TobisoApplication : Application() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        // Initialization order matters: StreakFreezeManager before ShopManager,
        // ShopManager before BackpackManager (BackpackManager reads ShopManager state).
        PointsManager.initialize(this)
        StreakFreezeManager.initialize(this)
        ShopManager.initialize(this)
        IconPackManager.initialize(this)
        BackpackManager.initialize(this)
    }
}
