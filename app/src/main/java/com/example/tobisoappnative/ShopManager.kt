package com.example.tobisoappnative

import android.content.Context
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ShopManager {
    private const val PREFS_NAME = "shop_prefs"
    private const val KEY_PURCHASED_PREFIX = "purchased_"
    private const val KEY_COOLDOWN_PREFIX = "cooldown_"
    
    private val _purchasedItems = MutableStateFlow<Set<Int>>(emptySet())
    val purchasedItems: StateFlow<Set<Int>> = _purchasedItems
    
    fun init(context: Context) {
        loadPurchasedItems(context)
    }
    
    // Načtení koupených itemů (jednoduše jen ID)
    private fun loadPurchasedItems(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val purchasedSet = mutableSetOf<Int>()
        
        // Načti všechny koupen items podle klíčů
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
        val classicIconPackId = 23
        if (!purchasedSet.contains(classicIconPackId)) {
            purchasedSet.add(classicIconPackId)
            // Uložit do SharedPreferences pro budoucí použití
            savePurchasedItem(context, classicIconPackId)
        }
        
        _purchasedItems.value = purchasedSet
    }
    
    // Uložení koupeného itemu
    private fun savePurchasedItem(context: Context, itemId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${KEY_PURCHASED_PREFIX}$itemId", true).apply()
    }
    
    // Odebrání koupeného itemu (pro jednorázové použití power-upů)
    private fun removePurchasedItem(context: Context, itemId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("${KEY_PURCHASED_PREFIX}$itemId").apply()
        
        // Aktualizace lokálního state
        val currentItems = _purchasedItems.value.toMutableSet()
        currentItems.remove(itemId)
        _purchasedItems.value = currentItems
    }
    
    // Nákup itemu
    fun purchaseItem(context: Context, item: ShopItem): Boolean {
        // Odečtení bodů přes PointsManager
        val success = PointsManager.subtractPoints(context, item.price)
        if (!success) {
            return false // Nedostatek bodů
        }
        
        // Speciální logika pro různé typy itemů
        when (item.type) {
            ShopItemType.STREAK_FREEZE -> {
                // Pro Streak Freeze přidáme do StreakFreezeManager místo označení jako "koupen"
                val freezeAdded = StreakFreezeManager.addStreakFreeze(context)
                if (!freezeAdded) {
                    // Pokud už má maximum freezes, vrátíme body
                    PointsManager.addPoints(context, item.price)
                    return false
                }
                return true
            }
            else -> {
                // Kontrola, zda už není koupen (pouze pro ostatní typy)
                if (isItemPurchased(item.id)) {
                    // Vrátíme body, protože item už je koupen
                    PointsManager.addPoints(context, item.price)
                    return false
                }
                
                // Označení itemu jako koupeného
                val currentItems = _purchasedItems.value.toMutableSet()
                currentItems.add(item.id)
                _purchasedItems.value = currentItems
                savePurchasedItem(context, item.id)
                
                // Refresh aktovky po nákupu
                BackpackManager.refreshItems(context)
                
                return true
            }
        }
    }
    
    // Kontrola, zda je item již koupen
    fun isItemPurchased(itemId: Int): Boolean {
        return _purchasedItems.value.contains(itemId)
    }
    
    // Speciální kontrola pro Streak Freeze - zda může koupit další
    fun canPurchaseStreakFreeze(): Boolean {
        return StreakFreezeManager.getAvailableFreezes() < 3
    }
    
    // Získání všech koupených itemů (pro budoucí funkcionalitu)
    fun getPurchasedItemIds(): Set<Int> {
        return _purchasedItems.value
    }
    
    // Použití power-upu
    fun usePowerUp(context: Context, item: ShopItem): Boolean {
        if (!isItemPurchased(item.id)) {
            return false // Item není koupen
        }
        
        if (isOnCooldown(context, item.id)) {
            return false // Item je na cooldownu
        }
        
        // Aktivace multiplikátoru bodů
        if (item.type == ShopItemType.POINTS_MULTIPLIER && 
            item.multiplier != null && 
            item.durationMinutes != null) {
            
            PointsManager.activateMultiplier(context, item.multiplier, item.durationMinutes)
            
            // Odebrání power-upu z vlastněných položek (jednorázové použití)
            removePurchasedItem(context, item.id)
            
            // Nastavení 3hodinového cooldownu (i když už power-up nevlastníme)
            setCooldown(context, item.id, 180) // 180 minut = 3 hodiny
            
            // Refresh aktovky po použití
            BackpackManager.refreshItems(context)
            
            return true
        }
        
        return false
    }
    
    // Nastavení cooldownu pro item
    private fun setCooldown(context: Context, itemId: Int, cooldownMinutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cooldownEnd = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
        prefs.edit().putLong("${KEY_COOLDOWN_PREFIX}$itemId", cooldownEnd).apply()
    }
    
    // Kontrola, zda je item na cooldownu
    fun isOnCooldown(context: Context, itemId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cooldownEnd = prefs.getLong("${KEY_COOLDOWN_PREFIX}$itemId", 0)
        return System.currentTimeMillis() < cooldownEnd
    }
    
    // Získání zbývajícího času cooldownu v minutách
    fun getCooldownTimeLeft(context: Context, itemId: Int): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cooldownEnd = prefs.getLong("${KEY_COOLDOWN_PREFIX}$itemId", 0)
        val currentTime = System.currentTimeMillis()
        
        return if (cooldownEnd > currentTime) {
            (cooldownEnd - currentTime) / (60 * 1000) // vrátí v minutách
        } else {
            0
        }
    }
}