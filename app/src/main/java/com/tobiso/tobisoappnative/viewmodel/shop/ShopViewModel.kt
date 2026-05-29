package com.tobiso.tobisoappnative.viewmodel.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.AiCreditManager
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.ShopManager
import com.tobiso.tobisoappnative.model.AddAiCreditsRequest
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.ShopItem
import com.tobiso.tobisoappnative.model.ShopItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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

    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage

    private val _showErrorMessage = MutableStateFlow(false)
    val showErrorMessage: StateFlow<Boolean> = _showErrorMessage

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing

    private val _showMysteryBoxOverlay = MutableStateFlow(false)
    val showMysteryBoxOverlay: StateFlow<Boolean> = _showMysteryBoxOverlay

    private val _mysteryBoxRewardText = MutableStateFlow("")
    val mysteryBoxRewardText: StateFlow<String> = _mysteryBoxRewardText

    fun selectItem(item: ShopItem, purchasedItemIds: Set<Int>) {
        _selectedItem.value = item
        when (item.type) {
            ShopItemType.POINTS_MULTIPLIER -> {
                if (ShopManager.instance.isOnCooldown(item.id)) {
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
        _showPurchaseDialog.value = false
        _selectedItem.value = null

        if (item.type == ShopItemType.AI_CREDIT) {
            viewModelScope.launch(Dispatchers.IO) {
                purchaseAiCredits(item)
            }
            return
        }

        if (item.type == ShopItemType.MYSTERY_BOX) {
            val success = ShopManager.instance.purchaseItem(item)
            if (success) {
                val reward = ShopManager.instance.lastMysteryBoxReward.value
                    ?: "Tajemná krabice byla otevřena! 🎁"
                _mysteryBoxRewardText.value = reward
                _showMysteryBoxOverlay.value = true
                ShopManager.instance.clearMysteryBoxReward()
            } else {
                _errorMessage.value = "Nedostatek bodů pro nákup tohoto itemu!"
                _showErrorMessage.value = true
            }
            return
        }

        val success = ShopManager.instance.purchaseItem(item)
        if (success) {
            _successMessage.value = "Nákup proběhl úspěšně! ✅"
            _showSuccessMessage.value = true
        } else {
            _errorMessage.value = "Nedostatek bodů pro nákup tohoto itemu!"
            _showErrorMessage.value = true
        }
    }

    private suspend fun purchaseAiCredits(item: ShopItem) {
        val count = item.creditCount ?: return
        _isPurchasing.value = true

        val pointsDeducted = PointsManager.instance.subtractPoints(item.price)
        if (!pointsDeducted) {
            _errorMessage.value = "Nedostatek bodů pro nákup tohoto itemu!"
            _showErrorMessage.value = true
            _isPurchasing.value = false
            return
        }

        try {
            val manager = AiCreditManager.instance
            val deviceId = manager.deviceId
            val validUntilUtc = (System.currentTimeMillis() / 1000L) + 86400L
            val signature = manager.signCreditRequest(deviceId, count, validUntilUtc)
            val request = AddAiCreditsRequest(
                deviceId = deviceId,
                count = count,
                validUntilUtc = validUntilUtc,
                signature = signature
            )
            val response = ApiClient.apiService.addAiCredits(deviceId, request)
            if (response.success) {
                manager.updateBonusRemaining(response.totalRemainingToday)
                _showSuccessMessage.value = true
            } else {
                PointsManager.instance.addPoints(item.price)
                _errorMessage.value = "Nepodařilo se zakoupit AI kredity. Zkus to znovu."
                _showErrorMessage.value = true
            }
        } catch (e: Exception) {
            PointsManager.instance.addPoints(item.price)
            _errorMessage.value = "Nepodařilo se spojit se serverem. Body vráceny."
            _showErrorMessage.value = true
            Timber.e(e, "Failed to purchase AI credits")
        } finally {
            _isPurchasing.value = false
        }
    }

    fun dismissPurchaseDialog() {
        _showPurchaseDialog.value = false
        _selectedItem.value = null
    }

    fun confirmUsePowerUp() {
        val item = _selectedItem.value ?: return
        val success = ShopManager.instance.usePowerUp(item)
        if (success) {
            _showSuccessMessage.value = true
        } else {
            _errorMessage.value = if (ShopManager.instance.isOnCooldown(item.id))
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

    fun dismissMysteryBoxOverlay() {
        _showMysteryBoxOverlay.value = false
        _mysteryBoxRewardText.value = ""
    }

    fun clearSuccessMessage() {
        _showSuccessMessage.value = false
    }

    fun clearErrorMessage() {
        _showErrorMessage.value = false
    }
}
