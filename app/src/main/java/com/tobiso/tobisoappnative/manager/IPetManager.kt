package com.tobiso.tobisoappnative.manager

import com.tobiso.tobisoappnative.model.ShopItem
import kotlinx.coroutines.flow.StateFlow

enum class PetHealth {
    ALIVE,
    DEAD_HUNGER,
    DEAD_THIRST
}

enum class GrowthStage(val displayName: String, val emoji: String) {
    BABY("Mládě", "🍼"),
    ADOLESCENT("Dospívající", "🌱"),
    ADULT("Dospělý", "🌳"),
    FULLY_GROWN("Plně dospělý", "👑")
}

interface IPetManager {
    val foodCount: StateFlow<Int>
    val waterCount: StateFlow<Int>

    fun initialize(context: android.content.Context)
    fun addFood(amount: Int)
    fun addWater(amount: Int)
    fun useFood(): Boolean
    fun useWater(): Boolean
    fun initializePet(petId: Int)
    fun isPetInitialized(petId: Int): Boolean
    fun feedPet(petId: Int): Boolean
    fun waterPet(petId: Int): Boolean
    fun checkPetStatus(petId: Int): PetHealth
    fun revivePet(petId: Int): Boolean
    fun getGrowthLevel(petId: Int): Float
    fun getGrowthStage(petId: Int): GrowthStage
    fun isPetDead(petId: Int): Boolean
    fun getPetHealth(petId: Int): PetHealth
    fun getPetGrowthDays(petId: Int): Int
    fun canFeedPet(petId: Int): Boolean
    fun canWaterPet(petId: Int): Boolean
    fun getTimeUntilNextFeed(petId: Int): Long
    fun getTimeUntilNextWater(petId: Int): Long
    fun getLastFedTime(petId: Int): Long
    fun getLastWateredTime(petId: Int): Long
}
