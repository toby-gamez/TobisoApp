package com.example.tobisoappnative

import android.content.Context
import com.example.tobisoappnative.manager.IShopManager
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShopManager private constructor(context: Context) : IShopManager {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _purchasedItems = MutableStateFlow<Set<Int>>(emptySet())
    override val purchasedItems: StateFlow<Set<Int>> = _purchasedItems

    init {
        loadPurchasedItems()
    }

    override fun init() {
        loadPurchasedItems()
    }

    private fun loadPurchasedItems() {
        val purchasedSet = mutableSetOf<Int>()
        for (entry in prefs.all) {
            if (entry.key.startsWith(KEY_PURCHASED_PREFIX) && entry.value == true) {
                try {
                    val itemId = entry.key.removePrefix(KEY_PURCHASED_PREFIX).toInt()
                    purchasedSet.add(itemId)
                } catch (e: NumberFormatException) {
                    // Ignoruj neplatné klíče
                }
            }
        }
        // Vždycky automaticky přidat "Klasické ikony" balíček (ID 23) jako vlastněný
        if (!purchasedSet.contains(CLASSIC_ICON_PACK_ID)) {
            purchasedSet.add(CLASSIC_ICON_PACK_ID)
            savePurchasedItem(CLASSIC_ICON_PACK_ID)
        }
        _purchasedItems.value = purchasedSet
    }

    private fun savePurchasedItem(itemId: Int) {
        prefs.edit().putBoolean("${KEY_PURCHASED_PREFIX}$itemId", true).apply()
    }

    private fun removePurchasedItem(itemId: Int) {
        prefs.edit().remove("${KEY_PURCHASED_PREFIX}$itemId").apply()
        val currentItems = _purchasedItems.value.toMutableSet()
        currentItems.remove(itemId)
        _purchasedItems.value = currentItems
    }

    override fun purchaseItem(item: ShopItem): Boolean {
        val success = PointsManager.subtractPoints(item.price)
        if (!success) return false

        return when (item.type) {
            ShopItemType.STREAK_FREEZE -> {
                val freezeAdded = StreakFreezeManager.addStreakFreeze()
                if (!freezeAdded) {
                    PointsManager.addPoints(item.price)
                    return false
                }
                true
            }
            else -> {
                if (isItemPurchased(item.id)) {
                    PointsManager.addPoints(item.price)
                    return false
                }
                val currentItems = _purchasedItems.value.toMutableSet()
                currentItems.add(item.id)
                _purchasedItems.value = currentItems
                savePurchasedItem(item.id)
                BackpackManager.refreshItems()
                true
            }
        }
    }

    override fun isItemPurchased(itemId: Int): Boolean = _purchasedItems.value.contains(itemId)

    override fun canPurchaseStreakFreeze(): Boolean = StreakFreezeManager.getAvailableFreezes() < 3

    override fun getPurchasedItemIds(): Set<Int> = _purchasedItems.value

    override fun usePowerUp(item: ShopItem): Boolean {
        if (!isItemPurchased(item.id)) return false
        if (isOnCooldown(item.id)) return false

        if (item.type == ShopItemType.POINTS_MULTIPLIER &&
            item.multiplier != null &&
            item.durationMinutes != null
        ) {
            PointsManager.activateMultiplier(item.multiplier, item.durationMinutes)
            removePurchasedItem(item.id)
            setCooldown(item.id, 180) // 180 minut = 3 hodiny
            BackpackManager.refreshItems()
            return true
        }
        return false
    }

    private fun setCooldown(itemId: Int, cooldownMinutes: Int) {
        val cooldownEnd = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
        prefs.edit().putLong("${KEY_COOLDOWN_PREFIX}$itemId", cooldownEnd).apply()
    }

    override fun isOnCooldown(itemId: Int): Boolean {
        val cooldownEnd = prefs.getLong("${KEY_COOLDOWN_PREFIX}$itemId", 0)
        return System.currentTimeMillis() < cooldownEnd
    }

    override fun getCooldownTimeLeft(itemId: Int): Long {
        val cooldownEnd = prefs.getLong("${KEY_COOLDOWN_PREFIX}$itemId", 0)
        val currentTime = System.currentTimeMillis()
        return if (cooldownEnd > currentTime) (cooldownEnd - currentTime) / (60 * 1000) else 0
    }

    companion object {
        private const val PREFS_NAME = "shop_prefs"
        private const val KEY_PURCHASED_PREFIX = "purchased_"
        private const val KEY_COOLDOWN_PREFIX = "cooldown_"
        private const val CLASSIC_ICON_PACK_ID = 23

        @Volatile private var INSTANCE: ShopManager? = null

        val instance: ShopManager
            get() = INSTANCE ?: error("ShopManager.initialize() must be called before use")

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = ShopManager(context.applicationContext)
                    }
                }
            }
        }

        // Delegations for direct access without .instance
        val purchasedItems get() = instance.purchasedItems

        fun init() = instance.init()
        fun purchaseItem(item: ShopItem) = instance.purchaseItem(item)
        fun isItemPurchased(itemId: Int) = instance.isItemPurchased(itemId)
        fun canPurchaseStreakFreeze() = instance.canPurchaseStreakFreeze()
        fun getPurchasedItemIds() = instance.getPurchasedItemIds()
        fun usePowerUp(item: ShopItem) = instance.usePowerUp(item)
        fun isOnCooldown(itemId: Int) = instance.isOnCooldown(itemId)
        fun getCooldownTimeLeft(itemId: Int) = instance.getCooldownTimeLeft(itemId)
    }
}