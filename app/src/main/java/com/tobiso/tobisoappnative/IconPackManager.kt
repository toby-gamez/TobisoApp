package com.tobiso.tobisoappnative

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.tobiso.tobisoappnative.data.ShopData
import com.tobiso.tobisoappnative.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IconPackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeIconPack = MutableStateFlow<ShopItem?>(null)
    val activeIconPack: StateFlow<ShopItem?> = _activeIconPack

    // Defaultní Material Icons pro každý předmět
    private val defaultIcons = mapOf(
        "Mluvnice" to Icons.Rounded.Spellcheck,
        "Literatura" to Icons.Rounded.MenuBook,
        "Sloh" to Icons.Rounded.Description,
        "Hudební výchova" to Icons.Rounded.LibraryMusic,
        "Matematika" to Icons.Rounded.Calculate,
        "Chemie" to Icons.Rounded.Science,
        "Fyzika" to Icons.Rounded.PrecisionManufacturing,
        "Přírodopis" to Icons.Rounded.Eco,
        "Zeměpis" to Icons.Rounded.Public
    )

    // Mapování názvů Material ikon na skutečné ikony
    private val materialIconsMap = mapOf(
        "edit" to Icons.Rounded.Edit,
        "library_books" to Icons.Rounded.LibraryBooks,
        "article" to Icons.Rounded.Article,
        "music_note" to Icons.Rounded.MusicNote,
        "functions" to Icons.Rounded.Functions,
        "biotech" to Icons.Rounded.Biotech,
        "bolt" to Icons.Rounded.Bolt,
        "local_florist" to Icons.Rounded.LocalFlorist,
        "language" to Icons.Rounded.Language,
        "spellcheck" to Icons.Rounded.Spellcheck,
        "menu_book" to Icons.Rounded.MenuBook,
        "description" to Icons.Rounded.Description,
        "library_music" to Icons.Rounded.LibraryMusic,
        "calculate" to Icons.Rounded.Calculate,
        "science" to Icons.Rounded.Science,
        "precision_manufacturing" to Icons.Rounded.PrecisionManufacturing,
        "eco" to Icons.Rounded.Eco,
        "public" to Icons.Rounded.Public
    )

    init {
        loadActiveIconPack()
    }

    private fun loadActiveIconPack() {
        val activePackId = prefs.getInt(KEY_ACTIVE_ICON_PACK, -1)
        if (activePackId != -1) {
            _activeIconPack.value = ShopData.getItemById(activePackId)
        } else {
            ShopData.getItemById(ShopData.CLASSIC_ICON_PACK_ID)?.let { setActiveIconPack(it) }
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
                    IconPackType.MATERIAL_ICONS -> materialIconsMap[subjectIcon.icon] ?: defaultIcons[subjectName] ?: Icons.Rounded.Book
                    IconPackType.CUSTOM_ICONS -> subjectIcon.icon
                }
            }
        }
        return defaultIcons[subjectName] ?: Icons.Rounded.Book
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
        return ShopManager.instance.purchasedItems.value.mapNotNull { itemId ->
            ShopData.getItemById(itemId)?.takeIf { it.type == ShopItemType.ICON_PACK }
        }
    }

    fun isIconPackActive(iconPackId: Int): Boolean = _activeIconPack.value?.id == iconPackId

    companion object {
        private const val PREFS_NAME = "backpack_prefs"
        private const val KEY_ACTIVE_ICON_PACK = "equipped_icon_pack"

        lateinit var instance: IconPackManager
            private set

        fun initialize(context: Context) {
            instance = IconPackManager(context.applicationContext)
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