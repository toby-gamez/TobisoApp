package com.example.tobisoappnative

import android.content.Context
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BackpackManager {
    private const val PREFS_NAME = "backpack_prefs"
    private const val KEY_EQUIPPED_QUOTE = "equipped_quote"
    private const val KEY_EQUIPPED_PET = "equipped_pet"
    private const val KEY_EQUIPPED_ICON_PACK = "equipped_icon_pack"
    
    private val _backpackItems = MutableStateFlow<List<BackpackItem>>(emptyList())
    val backpackItems: StateFlow<List<BackpackItem>> = _backpackItems
    
    private val _equippedQuote = MutableStateFlow<ShopItem?>(null)
    val equippedQuote: StateFlow<ShopItem?> = _equippedQuote
    
    private val _equippedPet = MutableStateFlow<ShopItem?>(null)
    val equippedPet: StateFlow<ShopItem?> = _equippedPet
    
    private val _equippedIconPack = MutableStateFlow<ShopItem?>(null)
    val equippedIconPack: StateFlow<ShopItem?> = _equippedIconPack
    
    fun init(context: Context) {
        loadBackpackItems(context)
        loadEquippedItems(context)
        IconPackManager.init(context)
    }
    
    // Načte všechny koupené itemy z obchodu
    private fun loadBackpackItems(context: Context) {
        val purchasedItemIds = ShopManager.purchasedItems.value
        val backpackItemsList = mutableListOf<BackpackItem>()
        
        purchasedItemIds.forEach { itemId ->
            val shopItem = ShopData.getItemById(itemId)
            if (shopItem != null) {
                // Pouze permanentní itemy (ne power-upy s cooldownem)
                if (shopItem.type != ShopItemType.POINTS_MULTIPLIER) {
                    backpackItemsList.add(
                        BackpackItem(
                            shopItem = shopItem,
                            purchaseDate = System.currentTimeMillis() // TODO: uložit skutečné datum nákupu
                        )
                    )
                }
            }
        }
        
        // Ujisti se, že "Klasické ikony" jsou vždycky v aktovce
        val classicIconPackId = 23
        val hasClassicIconPack = backpackItemsList.any { it.shopItem.id == classicIconPackId }
        if (!hasClassicIconPack) {
            val classicIconPack = ShopData.getItemById(classicIconPackId)
            if (classicIconPack != null) {
                backpackItemsList.add(
                    BackpackItem(
                        shopItem = classicIconPack,
                        purchaseDate = System.currentTimeMillis()
                    )
                )
            }
        }
        
        _backpackItems.value = backpackItemsList
    }
    
    // Načte vybavené itemy
    private fun loadEquippedItems(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val equippedQuoteId = prefs.getInt(KEY_EQUIPPED_QUOTE, -1)
        if (equippedQuoteId != -1) {
            _equippedQuote.value = ShopData.getItemById(equippedQuoteId)
        }
        
        val equippedPetId = prefs.getInt(KEY_EQUIPPED_PET, -1)
        if (equippedPetId != -1) {
            _equippedPet.value = ShopData.getItemById(equippedPetId)
        }
        
        val equippedIconPackId = prefs.getInt(KEY_EQUIPPED_ICON_PACK, -1)
        if (equippedIconPackId != -1) {
            val iconPack = ShopData.getItemById(equippedIconPackId)
            _equippedIconPack.value = iconPack
            // Synchronizuj s IconPackManagerem
            if (iconPack != null) {
                IconPackManager.setActiveIconPack(context, iconPack)
            }
        } else {
            // Pokud nemá vybavený žádný balíček ikon, nastav "Klasické ikony" jako výchozí
            val classicIconPack = ShopData.getItemById(23) // ID 23 = Klasické ikony
            if (classicIconPack != null) {
                equipIconPack(context, classicIconPack)
            }
        }
    }
    
    // Vybavení citátu
    fun equipQuote(context: Context, quote: ShopItem?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_EQUIPPED_QUOTE, quote?.id ?: -1).apply()
        _equippedQuote.value = quote
    }
    
    // Vybavení zvířátka
    fun equipPet(context: Context, pet: ShopItem?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_EQUIPPED_PET, pet?.id ?: -1).apply()
        _equippedPet.value = pet
    }
    
    // Vybavení balíčku ikon
    fun equipIconPack(context: Context, iconPack: ShopItem?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_EQUIPPED_ICON_PACK, iconPack?.id ?: -1).apply()
        _equippedIconPack.value = iconPack
        // Synchronizace s IconPackManagerem
        IconPackManager.setActiveIconPack(context, iconPack)
    }
    
    // Získání itemů podle kategorie
    fun getItemsByCategory(category: BackpackCategory): List<BackpackItem> {
        return _backpackItems.value.filter { backpackItem ->
            when (category) {
                BackpackCategory.QUOTES -> backpackItem.shopItem.type == ShopItemType.PROFILE_QUOTE
                BackpackCategory.ICON_PACKS -> backpackItem.shopItem.type == ShopItemType.ICON_PACK // Upraveno
                BackpackCategory.PETS -> backpackItem.shopItem.type == ShopItemType.PET
                BackpackCategory.POWER_UPS -> backpackItem.shopItem.type == ShopItemType.POINTS_MULTIPLIER
                BackpackCategory.STREAK_ITEMS -> backpackItem.shopItem.type == ShopItemType.STREAK_FREEZE
            }
        }
    }
    
    // Refresh po novém nákupu
    fun refreshItems(context: Context) {
        loadBackpackItems(context)
    }
}