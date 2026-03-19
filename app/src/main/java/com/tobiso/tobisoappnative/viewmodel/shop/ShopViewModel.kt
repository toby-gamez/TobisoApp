package com.tobiso.tobisoappnative.viewmodel.shop

import androidx.lifecycle.ViewModel
import com.tobiso.tobisoappnative.ShopManager
import com.tobiso.tobisoappnative.model.ShopItem
import com.tobiso.tobisoappnative.model.ShopItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor() : ViewModel() {

    private val _selectedItem = MutableStateFlow<ShopItem?>(null)
    val selectedItem: StateFlow<ShopItem?> = _selectedItem

    private val _showPurchaseDialog = MutableStateFlow(false)
    val showPurchaseDialog: StateFlow<Boolean> = _showPurchaseDialog

    private val _showUsePowerUpDialog = MutableStateFlow(false)
    val showUsePowerUpDialog: StateFlow<Boolean> = _showUsePowerUpDialog

    private val _showSuccessMessage = MutableStateFlow(false)
    val showSuccessMessage: StateFlow<Boolean> = _showSuccessMessage

    private val _showErrorMessage = MutableStateFlow(false)
    val showErrorMessage: StateFlow<Boolean> = _showErrorMessage

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun selectItem(item: ShopItem, purchasedItemIds: Set<Int>) {
        _selectedItem.value = item
        when (item.type) {
            ShopItemType.POINTS_MULTIPLIER -> {
                if (ShopManager.isOnCooldown(item.id)) {
                    // Na cooldownu - nezobrazuj dialog
                    return
                } else if (purchasedItemIds.contains(item.id)) {
                    _showUsePowerUpDialog.value = true
                } else {
                    _showPurchaseDialog.value = true
                }
            }
            ShopItemType.STREAK_FREEZE -> {
                _showPurchaseDialog.value = true
            }
            else -> {
                _showPurchaseDialog.value = true
            }
        }
    }

    fun confirmPurchase() {
        val item = _selectedItem.value ?: return
        val success = ShopManager.purchaseItem(item)
        if (success) {
            _showSuccessMessage.value = true
        } else {
            _errorMessage.value = "Nedostatek bodů pro nákup tohoto itemu!"
            _showErrorMessage.value = true
        }
        _showPurchaseDialog.value = false
        _selectedItem.value = null
    }

    fun dismissPurchaseDialog() {
        _showPurchaseDialog.value = false
        _selectedItem.value = null
    }

    fun confirmUsePowerUp() {
        val item = _selectedItem.value ?: return
        val success = ShopManager.usePowerUp(item)
        if (success) {
            _showSuccessMessage.value = true
        } else {
            _errorMessage.value = if (ShopManager.isOnCooldown(item.id))
                "Power-up je na cooldownu!" else "Chyba při aktivaci power-upu!"
            _showErrorMessage.value = true
        }
        _showUsePowerUpDialog.value = false
        _selectedItem.value = null
    }

    fun dismissUsePowerUpDialog() {
        _showUsePowerUpDialog.value = false
        _selectedItem.value = null
    }

    fun clearSuccessMessage() {
        _showSuccessMessage.value = false
    }

    fun clearErrorMessage() {
        _showErrorMessage.value = false
    }
}
