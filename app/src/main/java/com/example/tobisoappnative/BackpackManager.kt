package com.example.tobisoappnative

import android.content.Context
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.manager.IBackpackManager
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BackpackManager private constructor(context: Context) : IBackpackManager {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _backpackItems = MutableStateFlow<List<BackpackItem>>(emptyList())
    override val backpackItems: StateFlow<List<BackpackItem>> = _backpackItems

    private val _equippedQuote = MutableStateFlow<ShopItem?>(null)
    override val equippedQuote: StateFlow<ShopItem?> = _equippedQuote

    private val _equippedPet = MutableStateFlow<ShopItem?>(null)
    override val equippedPet: StateFlow<ShopItem?> = _equippedPet

    private val _equippedIconPack = MutableStateFlow<ShopItem?>(null)
    override val equippedIconPack: StateFlow<ShopItem?> = _equippedIconPack

    init {
        loadBackpackItems()
        loadEquippedItems()
        IconPackManager.initialize(appContext)
    }

    private fun loadBackpackItems() {
        val purchasedItemIds = ShopManager.purchasedItems.value
        val backpackItemsList = mutableListOf<BackpackItem>()

        purchasedItemIds.forEach { itemId ->
            val shopItem = ShopData.getItemById(itemId)
            if (shopItem != null && shopItem.type != ShopItemType.POINTS_MULTIPLIER) {
                backpackItemsList.add(BackpackItem(shopItem = shopItem, purchaseDate = System.currentTimeMillis()))
            }
        }

        // Ujisti se, že "Klasické ikony" jsou vždycky v aktovce
        val hasClassicIconPack = backpackItemsList.any { it.shopItem.id == CLASSIC_ICON_PACK_ID }
        if (!hasClassicIconPack) {
            ShopData.getItemById(CLASSIC_ICON_PACK_ID)?.let {
                backpackItemsList.add(BackpackItem(shopItem = it, purchaseDate = System.currentTimeMillis()))
            }
        }

        _backpackItems.value = backpackItemsList
    }

    private fun loadEquippedItems() {
        val equippedQuoteId = prefs.getInt(KEY_EQUIPPED_QUOTE, -1)
        if (equippedQuoteId != -1) _equippedQuote.value = ShopData.getItemById(equippedQuoteId)

        val equippedPetId = prefs.getInt(KEY_EQUIPPED_PET, -1)
        if (equippedPetId != -1) _equippedPet.value = ShopData.getItemById(equippedPetId)

        val equippedIconPackId = prefs.getInt(KEY_EQUIPPED_ICON_PACK, -1)
        if (equippedIconPackId != -1) {
            val iconPack = ShopData.getItemById(equippedIconPackId)
            _equippedIconPack.value = iconPack
            if (iconPack != null) IconPackManager.setActiveIconPack(iconPack)
        } else {
            ShopData.getItemById(CLASSIC_ICON_PACK_ID)?.let { equipIconPack(it) }
        }
    }

    override fun equipQuote(quote: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_QUOTE, quote?.id ?: -1).apply()
        _equippedQuote.value = quote
    }

    override fun equipPet(pet: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_PET, pet?.id ?: -1).apply()
        _equippedPet.value = pet
    }

    override fun equipIconPack(iconPack: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_ICON_PACK, iconPack?.id ?: -1).apply()
        _equippedIconPack.value = iconPack
        IconPackManager.setActiveIconPack(iconPack)
    }

    override fun getItemsByCategory(category: BackpackCategory): List<BackpackItem> {
        return _backpackItems.value.filter { backpackItem ->
            when (category) {
                BackpackCategory.QUOTES -> backpackItem.shopItem.type == ShopItemType.PROFILE_QUOTE
                BackpackCategory.ICON_PACKS -> backpackItem.shopItem.type == ShopItemType.ICON_PACK
                BackpackCategory.PETS -> backpackItem.shopItem.type == ShopItemType.PET
                BackpackCategory.POWER_UPS -> backpackItem.shopItem.type == ShopItemType.POINTS_MULTIPLIER
                BackpackCategory.STREAK_ITEMS -> backpackItem.shopItem.type == ShopItemType.STREAK_FREEZE
            }
        }
    }

    override fun refreshItems() {
        loadBackpackItems()
    }

    companion object {
        private const val PREFS_NAME = "backpack_prefs"
        private const val KEY_EQUIPPED_QUOTE = "equipped_quote"
        private const val KEY_EQUIPPED_PET = "equipped_pet"
        private const val KEY_EQUIPPED_ICON_PACK = "equipped_icon_pack"
        private const val CLASSIC_ICON_PACK_ID = 23

        @Volatile private var INSTANCE: BackpackManager? = null

        val instance: BackpackManager
            get() = INSTANCE ?: error("BackpackManager.initialize() must be called before use")

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = BackpackManager(context.applicationContext)
                    }
                }
            }
        }

        // Delegations for direct access without .instance
        val backpackItems get() = instance.backpackItems
        val equippedQuote get() = instance.equippedQuote
        val equippedPet get() = instance.equippedPet
        val equippedIconPack get() = instance.equippedIconPack

        fun equipQuote(quote: ShopItem?) = instance.equipQuote(quote)
        fun equipPet(pet: ShopItem?) = instance.equipPet(pet)
        fun equipIconPack(iconPack: ShopItem?) = instance.equipIconPack(iconPack)
        fun getItemsByCategory(category: BackpackCategory) = instance.getItemsByCategory(category)
        fun refreshItems() = instance.refreshItems()
    }
}