package com.example.tobisoappnative.model

// Item v aktovce - reference na ShopItem
data class BackpackItem(
    val shopItem: ShopItem,
    val purchaseDate: Long,
    val isEquipped: Boolean = false, // Pro budoucí použití - aktivní citát/ikona/zvíře
    val customName: String? = null // Pro personalizaci
)

// Kategorie itemů v aktovce
enum class BackpackCategory(val displayName: String) {
    QUOTES("Citáty"),
    ICONS("Ikony předmětů"),
    PETS("Zvířátka"),
    POWER_UPS("Power-upy"),
    STREAK_ITEMS("Řada")
}