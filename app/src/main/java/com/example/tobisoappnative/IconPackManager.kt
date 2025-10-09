package com.example.tobisoappnative

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object IconPackManager {
    private const val PREFS_NAME = "backpack_prefs"
    private const val KEY_ACTIVE_ICON_PACK = "equipped_icon_pack"
    
    private val _activeIconPack = MutableStateFlow<ShopItem?>(null)
    val activeIconPack: StateFlow<ShopItem?> = _activeIconPack
    
    // Defaultní Material Icons pro každý předmět
    private val defaultIcons = mapOf(
        "Mluvnice" to Icons.Default.Spellcheck,
        "Literatura" to Icons.Default.MenuBook,
        "Sloh" to Icons.Default.Description,
        "Hudební výchova" to Icons.Default.LibraryMusic,
        "Matematika" to Icons.Default.Calculate,
        "Chemie" to Icons.Default.Science,
        "Fyzika" to Icons.Default.PrecisionManufacturing,
        "Přírodopis" to Icons.Default.Eco,
        "Zeměpis" to Icons.Default.Public
    )
    
    // Mapování názvů Material ikon na skutečné ikony
    private val materialIconsMap = mapOf(
        "edit" to Icons.Default.Edit,
        "library_books" to Icons.Default.LibraryBooks,
        "article" to Icons.Default.Article,
        "music_note" to Icons.Default.MusicNote,
        "functions" to Icons.Default.Functions,
        "biotech" to Icons.Default.Biotech,
        "bolt" to Icons.Default.Bolt,
        "local_florist" to Icons.Default.LocalFlorist,
        "language" to Icons.Default.Language
    )
    
    fun init(context: Context) {
        loadActiveIconPack(context)
    }
    
    private fun loadActiveIconPack(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activePackId = prefs.getInt(KEY_ACTIVE_ICON_PACK, -1)
        
        if (activePackId != -1) {
            _activeIconPack.value = ShopData.getItemById(activePackId)
        }
    }
    
    // Nastavení aktivního balíčku ikon
    fun setActiveIconPack(context: Context, iconPack: ShopItem?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ACTIVE_ICON_PACK, iconPack?.id ?: -1).apply()
        _activeIconPack.value = iconPack
    }
    
    // Získání ikony pro předmět podle aktivního balíčku
    fun getSubjectIcon(subjectName: String): Any {
        val activePack = _activeIconPack.value
        
        if (activePack?.subjectIcons != null) {
            val subjectIcon = activePack.subjectIcons.find { it.subjectName == subjectName }
            if (subjectIcon != null) {
                return when (subjectIcon.iconType) {
                    IconPackType.EMOJI -> subjectIcon.icon // Vrátí emoji string
                    IconPackType.MATERIAL_ICONS -> {
                        // Vrátí Material Icon
                        materialIconsMap[subjectIcon.icon] ?: defaultIcons[subjectName] ?: Icons.Default.Book
                    }
                    IconPackType.CUSTOM_ICONS -> subjectIcon.icon // Pro budoucí custom ikony
                }
            }
        }
        
        // Fallback na defaultní Material ikonu
        return defaultIcons[subjectName] ?: Icons.Default.Book
    }
    
    // Zkontroluje, zda je typ ikony emoji
    fun isEmojiIcon(subjectName: String): Boolean {
        val activePack = _activeIconPack.value
        if (activePack?.subjectIcons != null) {
            val subjectIcon = activePack.subjectIcons.find { it.subjectName == subjectName }
            return subjectIcon?.iconType == IconPackType.EMOJI
        }
        return false
    }
    
    // Získá všechny dostupné balíčky ikon (koupené)
    fun getAvailableIconPacks(): List<ShopItem> {
        val purchasedItems = ShopManager.purchasedItems.value
        return purchasedItems.mapNotNull { itemId ->
            val item = ShopData.getItemById(itemId)
            if (item?.type == ShopItemType.ICON_PACK) item else null
        }
    }
    
    // Zkontroluje, zda je balíček ikon aktivní
    fun isIconPackActive(iconPackId: Int): Boolean {
        return _activeIconPack.value?.id == iconPackId
    }
}