package com.example.tobisoappnative.viewmodel.backpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tobisoappnative.BackpackManager
import com.example.tobisoappnative.model.BackpackItem
import com.example.tobisoappnative.model.ShopItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BackpackViewModel : ViewModel() {

    private val _selectedItem = MutableStateFlow<BackpackItem?>(null)
    val selectedItem: StateFlow<BackpackItem?> = _selectedItem

    private val _showItemDialog = MutableStateFlow(false)
    val showItemDialog: StateFlow<Boolean> = _showItemDialog

    private val _showSuccessMessage = MutableStateFlow(false)
    val showSuccessMessage: StateFlow<Boolean> = _showSuccessMessage

    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage

    fun selectItem(item: BackpackItem) {
        _selectedItem.value = item
        _showItemDialog.value = true
    }

    fun dismissDialog() {
        _showItemDialog.value = false
        _selectedItem.value = null
    }

    fun equipItem(item: BackpackItem) {
        val message = when (item.shopItem.type) {
            ShopItemType.PROFILE_QUOTE -> {
                BackpackManager.equipQuote(item.shopItem)
                "Citát byl nasazen!"
            }
            ShopItemType.PET -> {
                BackpackManager.equipPet(item.shopItem)
                "Zvířátko bylo nasazeno!"
            }
            ShopItemType.ICON_PACK -> {
                BackpackManager.equipIconPack(item.shopItem)
                "Balíček ikon byl aktivován!"
            }
            else -> "Item byl použit!"
        }
        _successMessage.value = message
        _showSuccessMessage.value = true
        _showItemDialog.value = false
    }

    fun unequipItem(item: BackpackItem) {
        val message = when (item.shopItem.type) {
            ShopItemType.PROFILE_QUOTE -> {
                BackpackManager.equipQuote(null)
                "Citát byl odstraněn"
            }
            ShopItemType.PET -> {
                BackpackManager.equipPet(null)
                "Zvířátko bylo odstraněno"
            }
            ShopItemType.ICON_PACK -> {
                BackpackManager.equipIconPack(null)
                "Balíček ikon byl deaktivován"
            }
            else -> "Item byl odstraněn"
        }
        _successMessage.value = message
        _showSuccessMessage.value = true
        _showItemDialog.value = false
    }

    fun clearSuccessMessage() {
        _showSuccessMessage.value = false
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            BackpackViewModel() as T
    }
}
