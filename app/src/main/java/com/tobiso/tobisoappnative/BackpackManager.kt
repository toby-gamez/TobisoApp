package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.data.ShopData
import com.tobiso.tobisoappnative.manager.IBackpackManager
import com.tobiso.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

    private val _equippedTheme = MutableStateFlow<ShopItem?>(null)
    override val equippedTheme: StateFlow<ShopItem?> = _equippedTheme

    init {
        loadBackpackItems()
        loadEquippedItems()
        IconPackManager.initialize(appContext)
    }

    private fun loadBackpackItems() {
        val purchasedItemIds = ShopManager.instance.purchasedItems.value
        val backpackItemsList = mutableListOf<BackpackItem>()

        purchasedItemIds.forEach { itemId ->
            val shopItem = ShopData.getItemById(itemId)
            if (shopItem != null && shopItem.type != ShopItemType.POINTS_MULTIPLIER && shopItem.type != ShopItemType.MYSTERY_BOX && shopItem.type != ShopItemType.PET_FOOD && shopItem.type != ShopItemType.PET_WATER) {
                val purchaseDate = ShopManager.instance.getPurchaseDate(itemId)
                    .takeIf { it > 0L } ?: System.currentTimeMillis()
                backpackItemsList.add(BackpackItem(shopItem = shopItem, purchaseDate = purchaseDate))
            }
        }

        val hasClassicIconPack = backpackItemsList.any { it.shopItem.id == ShopData.CLASSIC_ICON_PACK_ID }
        if (!hasClassicIconPack) {
            val classicPurchaseDate = ShopManager.instance.getPurchaseDate(ShopData.CLASSIC_ICON_PACK_ID)
                .takeIf { it > 0L } ?: System.currentTimeMillis()
            ShopData.getItemById(ShopData.CLASSIC_ICON_PACK_ID)?.let {
                backpackItemsList.add(BackpackItem(shopItem = it, purchaseDate = classicPurchaseDate))
            }
        }

        _backpackItems.update { backpackItemsList }
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
            ShopData.getItemById(ShopData.CLASSIC_ICON_PACK_ID)?.let { equipIconPack(it) }
        }

        val equippedThemeId = prefs.getInt(KEY_EQUIPPED_THEME, -1)
        if (equippedThemeId != -1) _equippedTheme.value = ShopData.getItemById(equippedThemeId)
    }

    override fun equipQuote(quote: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_QUOTE, quote?.id ?: -1).apply()
        _equippedQuote.value = quote
    }

    override fun equipPet(pet: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_PET, pet?.id ?: -1).apply()
        _equippedPet.value = pet
        if (pet != null && !PetManager.isPetInitialized(pet.id)) {
            PetManager.initializePet(pet.id)
        }
    }

    override fun equipIconPack(iconPack: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_ICON_PACK, iconPack?.id ?: -1).apply()
        _equippedIconPack.value = iconPack
        IconPackManager.setActiveIconPack(iconPack)
    }

    override fun equipTheme(theme: ShopItem?) {
        prefs.edit().putInt(KEY_EQUIPPED_THEME, theme?.id ?: -1).apply()
        _equippedTheme.value = theme
    }

    override fun getItemsByCategory(category: BackpackCategory): List<BackpackItem> {
        return _backpackItems.value.filter { backpackItem ->
            when (category) {
                BackpackCategory.QUOTES -> backpackItem.shopItem.type == ShopItemType.PROFILE_QUOTE
                BackpackCategory.ICON_PACKS -> backpackItem.shopItem.type == ShopItemType.ICON_PACK
                BackpackCategory.PETS -> backpackItem.shopItem.type == ShopItemType.PET
                BackpackCategory.POWER_UPS -> backpackItem.shopItem.type == ShopItemType.POINTS_MULTIPLIER
                BackpackCategory.STREAK_ITEMS -> backpackItem.shopItem.type == ShopItemType.STREAK_FREEZE
                BackpackCategory.PROFILE_THEMES -> backpackItem.shopItem.type == ShopItemType.PROFILE_THEME
                BackpackCategory.TROPHIES -> backpackItem.shopItem.type == ShopItemType.TROPHY
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
        private const val KEY_EQUIPPED_THEME = "equipped_theme"

        lateinit var instance: BackpackManager
            private set

        fun initialize(context: Context) {
            instance = BackpackManager(context.applicationContext)
        }
    }
}