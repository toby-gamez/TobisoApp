package com.example.tobisoappnative.manager

import com.example.tobisoappnative.model.BackpackCategory
import com.example.tobisoappnative.model.BackpackItem
import com.example.tobisoappnative.model.ShopItem
import kotlinx.coroutines.flow.StateFlow

interface IBackpackManager {
    val backpackItems: StateFlow<List<BackpackItem>>
    val equippedQuote: StateFlow<ShopItem?>
    val equippedPet: StateFlow<ShopItem?>
    val equippedIconPack: StateFlow<ShopItem?>

    fun equipQuote(quote: ShopItem?)
    fun equipPet(pet: ShopItem?)
    fun equipIconPack(iconPack: ShopItem?)
    fun getItemsByCategory(category: BackpackCategory): List<BackpackItem>
    fun refreshItems()
}
