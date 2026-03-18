package com.example.tobisoappnative.manager

import com.example.tobisoappnative.model.ShopItem
import kotlinx.coroutines.flow.StateFlow

interface IShopManager {
    val purchasedItems: StateFlow<Set<Int>>

    fun init()
    fun purchaseItem(item: ShopItem): Boolean
    fun isItemPurchased(itemId: Int): Boolean
    fun canPurchaseStreakFreeze(): Boolean
    fun getPurchasedItemIds(): Set<Int>
    fun usePowerUp(item: ShopItem): Boolean
    fun isOnCooldown(itemId: Int): Boolean
    fun getCooldownTimeLeft(itemId: Int): Long
}
