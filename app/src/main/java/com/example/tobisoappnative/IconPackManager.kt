package com.example.tobisoappnative

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IconPackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        "language" to Icons.Default.Language,
        "spellcheck" to Icons.Default.Spellcheck,
        "menu_book" to Icons.Default.MenuBook,
        "description" to Icons.Default.Description,
        "library_music" to Icons.Default.LibraryMusic,
        "calculate" to Icons.Default.Calculate,
        "science" to Icons.Default.Science,
        "precision_manufacturing" to Icons.Default.PrecisionManufacturing,
        "eco" to Icons.Default.Eco,
        "public" to Icons.Default.Public
    )

    init {
        loadActiveIconPack()
    }

    private fun loadActiveIconPack() {
        val activePackId = prefs.getInt(KEY_ACTIVE_ICON_PACK, -1)
        if (activePackId != -1) {
            _activeIconPack.value = ShopData.getItemById(activePackId)
        } else {
            ShopData.getItemById(CLASSIC_ICON_PACK_ID)?.let { setActiveIconPack(it) }
        }
    }

    fun setActiveIconPack(iconPack: ShopItem?) {
        prefs.edit().putInt(KEY_ACTIVE_ICON_PACK, iconPack?.id ?: -1).apply()
        _activeIconPack.value = iconPack
    }

    fun getSubjectIcon(subjectName: String): Any {
        val activePack = _activeIconPack.value
        if (activePack?.subjectIcons != null) {
            val subjectIcon = activePack.subjectIcons.find { it.subjectName == subjectName }
            if (subjectIcon != null) {
                return when (subjectIcon.iconType) {
                    IconPackType.EMOJI -> subjectIcon.icon
                    IconPackType.MATERIAL_ICONS -> materialIconsMap[subjectIcon.icon] ?: defaultIcons[subjectName] ?: Icons.Default.Book
                    IconPackType.CUSTOM_ICONS -> subjectIcon.icon
                }
            }
        }
        return defaultIcons[subjectName] ?: Icons.Default.Book
    }

    fun isEmojiIcon(subjectName: String): Boolean {
        val activePack = _activeIconPack.value
        if (activePack?.subjectIcons != null) {
            val subjectIcon = activePack.subjectIcons.find { it.subjectName == subjectName }
            return subjectIcon?.iconType == IconPackType.EMOJI
        }
        return false
    }

    fun getAvailableIconPacks(): List<ShopItem> {
        return ShopManager.purchasedItems.value.mapNotNull { itemId ->
            ShopData.getItemById(itemId)?.takeIf { it.type == ShopItemType.ICON_PACK }
        }
    }

    fun isIconPackActive(iconPackId: Int): Boolean = _activeIconPack.value?.id == iconPackId

    companion object {
        private const val PREFS_NAME = "backpack_prefs"
        private const val KEY_ACTIVE_ICON_PACK = "equipped_icon_pack"
        private const val CLASSIC_ICON_PACK_ID = 23

        @Volatile private var INSTANCE: IconPackManager? = null

        val instance: IconPackManager
            get() = INSTANCE ?: error("IconPackManager.initialize() must be called before use")

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = IconPackManager(context.applicationContext)
                    }
                }
            }
        }

        // Delegations for direct access without .instance
        val activeIconPack get() = instance.activeIconPack

        fun setActiveIconPack(iconPack: ShopItem?) = instance.setActiveIconPack(iconPack)
        fun getSubjectIcon(subjectName: String) = instance.getSubjectIcon(subjectName)
        fun isEmojiIcon(subjectName: String) = instance.isEmojiIcon(subjectName)
        fun getAvailableIconPacks() = instance.getAvailableIconPacks()
        fun isIconPackActive(iconPackId: Int) = instance.isIconPackActive(iconPackId)
    }
}