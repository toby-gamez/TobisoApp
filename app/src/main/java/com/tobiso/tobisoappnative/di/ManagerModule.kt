package com.tobiso.tobisoappnative.di

import com.tobiso.tobisoappnative.BackpackManager
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.ShopManager
import com.tobiso.tobisoappnative.StreakFreezeManager
import com.tobiso.tobisoappnative.manager.IBackpackManager
import com.tobiso.tobisoappnative.manager.IPointsManager
import com.tobiso.tobisoappnative.manager.IShopManager
import com.tobiso.tobisoappnative.manager.IStreakFreezeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides @Singleton
    fun providePointsManager(): IPointsManager = PointsManager.instance

    @Provides @Singleton
    fun provideStreakFreezeManager(): IStreakFreezeManager = StreakFreezeManager.instance

    @Provides @Singleton
    fun provideShopManager(): IShopManager = ShopManager.instance

    @Provides @Singleton
    fun provideBackpackManager(): IBackpackManager = BackpackManager.instance
}
