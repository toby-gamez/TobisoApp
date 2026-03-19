package com.tobiso.tobisoappnative.model

// Kategorie obchodu
enum class ShopCategory(val displayName: String) {
    STREAK("Řada"),
    QUOTES("Citáty"),
    SUBJECTS("Balíčky ikon"),
    POWER_UPS("Power-upy"),
    PETS("Zvířátka")
}

// Typ itemu v obchodě
enum class ShopItemType {
    STREAK_FREEZE,
    PROFILE_QUOTE,
    SUBJECT_ICON,
    ICON_PACK, // Nový typ pro balíčky ikon
    POINTS_MULTIPLIER,
    PET
}

// Typ balíčku ikon
enum class IconPackType {
    EMOJI,
    MATERIAL_ICONS,
    CUSTOM_ICONS
}

// Definice jednotlivých ikon v balíčku
data class SubjectIcon(
    val subjectName: String, // Název předmětu (např. "Matematika")
    val icon: String, // Emoji nebo název Material ikony
    val iconType: IconPackType
)

// Item v obchodě
data class ShopItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val type: ShopItemType,
    val iconRes: Int? = null, // Pro ikony předmětů (starý systém)
    val imageUrl: String? = null, // Pro zvířátka (URL obrázky)
    val petIcon: String? = null, // Pro zvířátka (emoji ikony)
    val quote: String? = null, // Pro citáty
    val durationMinutes: Int? = null, // Pro power-upy v minutách
    val multiplier: Float? = null, // Pro power-upy (1.5x, 2x, 3x)
    val cooldownMinutes: Int? = null, // Cooldown mezi použitím power-upů
    val powerUpIcon: String? = null, // Vizuální element pro power-upy
    // Nové vlastnosti pro balíčky ikon
    val iconPackType: IconPackType? = null, // Typ balíčku ikon
    val subjectIcons: List<SubjectIcon>? = null // Seznam ikon v balíčku
)

// Koupen item
data class PurchasedItem(
    val itemId: Int,
    val item: ShopItem,
    val purchaseDate: Long,
    val isActive: Boolean = true,
    val expiresAt: Long? = null // Pro power-upy s omezenou dobou
)