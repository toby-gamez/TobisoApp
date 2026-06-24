package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.data.ShopData
import com.tobiso.tobisoappnative.manager.IShopManager
import com.tobiso.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class ShopManager private constructor(context: Context) : IShopManager {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _purchasedItems = MutableStateFlow<Set<Int>>(emptySet())
    override val purchasedItems: StateFlow<Set<Int>> = _purchasedItems

    private val _lastMysteryBoxReward = MutableStateFlow<String?>(null)
    val lastMysteryBoxReward: StateFlow<String?> = _lastMysteryBoxReward

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
        if (!purchasedSet.contains(ShopData.CLASSIC_ICON_PACK_ID)) {
            purchasedSet.add(ShopData.CLASSIC_ICON_PACK_ID)
            savePurchasedItem(ShopData.CLASSIC_ICON_PACK_ID)
        }
        _purchasedItems.update { purchasedSet }
    }

    private fun savePurchasedItem(itemId: Int) {
        prefs.edit()
            .putBoolean("${KEY_PURCHASED_PREFIX}$itemId", true)
            .putLong("${KEY_PURCHASE_DATE_PREFIX}$itemId", System.currentTimeMillis())
            .apply()
    }

    override fun getPurchaseDate(itemId: Int): Long {
        return prefs.getLong("${KEY_PURCHASE_DATE_PREFIX}$itemId", 0L)
    }

    private fun removePurchasedItem(itemId: Int) {
        prefs.edit().remove("${KEY_PURCHASED_PREFIX}$itemId").apply()
        val currentItems = _purchasedItems.value.toMutableSet()
        currentItems.remove(itemId)
        _purchasedItems.update { currentItems }
    }

    override fun purchaseItem(item: ShopItem): Boolean {
        val success = PointsManager.instance.subtractPoints(item.price)
        if (!success) return false

        return when (item.type) {
            ShopItemType.STREAK_FREEZE -> {
                val freezeAdded = StreakFreezeManager.instance.addStreakFreeze()
                if (!freezeAdded) {
                    PointsManager.instance.addPoints(item.price)
                    return false
                }
                true
            }
            ShopItemType.MYSTERY_BOX -> {
                rollMysteryBoxReward()
                true
            }
            ShopItemType.PET_FOOD -> {
                PetManager.addFood(PetManager.FOOD_QUANTITY)
                true
            }
            ShopItemType.PET_WATER -> {
                PetManager.addWater(PetManager.WATER_QUANTITY)
                true
            }
            else -> {
                if (isItemPurchased(item.id)) {
                    PointsManager.instance.addPoints(item.price)
                    return false
                }
                val currentItems = _purchasedItems.value.toMutableSet()
                currentItems.add(item.id)
                _purchasedItems.update { currentItems }
                savePurchasedItem(item.id)
                BackpackManager.instance.refreshItems()
                true
            }
        }
    }

    override fun isItemPurchased(itemId: Int): Boolean = _purchasedItems.value.contains(itemId)

    override fun canPurchaseStreakFreeze(): Boolean = StreakFreezeManager.instance.getAvailableFreezes() < 3

    override fun getPurchasedItemIds(): Set<Int> = _purchasedItems.value

    override fun usePowerUp(item: ShopItem): Boolean {
        if (!isItemPurchased(item.id)) return false
        if (isOnCooldown(item.id)) return false

        if (item.type == ShopItemType.POINTS_MULTIPLIER &&
            item.multiplier != null &&
            item.durationMinutes != null
        ) {
            PointsManager.instance.activateMultiplier(item.multiplier, item.durationMinutes)
            removePurchasedItem(item.id)
            setCooldown(item.id, 180) // 180 minut = 3 hodiny
            BackpackManager.instance.refreshItems()
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

    fun clearMysteryBoxReward() {
        _lastMysteryBoxReward.value = null
    }

    private fun awardTrophy(trophyId: Int): Boolean {
        if (isItemPurchased(trophyId)) return false
        val currentItems = _purchasedItems.value.toMutableSet()
        currentItems.add(trophyId)
        _purchasedItems.update { currentItems }
        savePurchasedItem(trophyId)
        BackpackManager.instance.refreshItems()
        return true
    }

    private fun rollMysteryBoxReward() {
        val roll = Random.nextInt(100)
        val rewardText: String = when {
            roll < 29 -> {
                PointsManager.instance.addPoints(30)
                "Získal/a jsi 30 bodů! 🌟"
            }
            roll < 47 -> {
                PointsManager.instance.addPoints(50)
                "Získal/a jsi 50 bodů! ⭐"
            }
            roll < 61 -> {
                val added = StreakFreezeManager.instance.addStreakFreeze()
                if (added) "Získal/a jsi Zmražení řady! 🛡️"
                else {
                    PointsManager.instance.addPoints(30)
                    "Získal/a jsi 30 bodů! 🌟"
                }
            }
            roll < 71 -> {
                PointsManager.instance.addPoints(75)
                "Získal/a jsi 75 bodů! 💫"
            }
            roll < 79 -> {
                PointsManager.instance.addPoints(100)
                "Získal/a jsi 100 bodů! 🎉"
            }
            roll < 88 -> {
                val added = StreakFreezeManager.instance.addStreakFreeze()
                if (added) "Získal/a jsi Zmražení řady! 🛡️"
                else {
                    PointsManager.instance.addPoints(50)
                    "Získal/a jsi 50 bodů! ⭐"
                }
            }
            roll < 92 -> {
                PointsManager.instance.addPoints(150)
                "Získal/a jsi 150 bodů! 🎊"
            }
            roll < 95 -> {
                // Bronze trophy (3%) – fallback to points if already owned
                if (awardTrophy(ShopData.BRONZE_TROPHY_ID))
                    "Bronzová trofej! 🥉 Exkluzivní předmět z Tajemné krabice!"
                else {
                    PointsManager.instance.addPoints(50)
                    "Získal/a jsi 50 bodů! ⭐"
                }
            }
            roll < 97 -> {
                // Silver trophy (2%) – fallback to points if already owned
                if (awardTrophy(ShopData.SILVER_TROPHY_ID))
                    "Stříbrná trofej! 🥈 Vzácný exkluzivní předmět z Tajemné krabice!"
                else {
                    PointsManager.instance.addPoints(75)
                    "Získal/a jsi 75 bodů! 💫"
                }
            }
            roll < 98 -> {
                // Golden Day (1%)
                PointsManager.instance.activateMultiplier(2.0f, 24 * 60)
                "Zlatý den! Získáváš 2x body po celých 24 hodin! ✨"
            }
            else -> {
                // Gold trophy (1%) – fallback to random pet, then points
                if (awardTrophy(ShopData.GOLD_TROPHY_ID)) {
                    "ZLATÁ TROFEJ! 🏆 Legendární exkluzivní předmět z Tajemné krabice!"
                } else {
                    val availablePets = ShopData.getShopItems()
                        .filter { it.type == ShopItemType.PET && !isItemPurchased(it.id) }
                    if (availablePets.isNotEmpty()) {
                        val randomPet = availablePets.random()
                        awardTrophy(randomPet.id)
                        "Získal/a jsi vzácný mazlíček ${randomPet.name}! 🎁"
                    } else {
                        PointsManager.instance.addPoints(100)
                        "Získal/a jsi 100 bodů! 🎉"
                    }
                }
            }
        }
        _lastMysteryBoxReward.value = rewardText
    }

    companion object {
        private const val PREFS_NAME = "shop_prefs"
        private const val KEY_PURCHASED_PREFIX = "purchased_"
        private const val KEY_PURCHASE_DATE_PREFIX = "purchased_date_"
        private const val KEY_COOLDOWN_PREFIX = "cooldown_"

        lateinit var instance: ShopManager
            private set

        fun initialize(context: Context) {
            instance = ShopManager(context.applicationContext)
        }
    }
}