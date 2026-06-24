package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.manager.GrowthStage
import com.tobiso.tobisoappnative.manager.IPetManager
import com.tobiso.tobisoappnative.manager.PetHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PetManager : IPetManager {

    private const val PREFS_NAME = "pet_prefs"
    private const val KEY_FOOD = "pet_food"
    private const val KEY_WATER = "pet_water"
    private const val KEY_LAST_FED = "last_fed_"
    private const val KEY_LAST_WATERED = "last_watered_"
    private const val KEY_ACQUIRED = "acquired_"
    private const val KEY_GROWTH_DAYS = "growth_days_"
    private const val KEY_IS_DEAD = "is_dead_"
    private const val KEY_DEATH_CAUSE = "death_cause_"

    private const val THIRST_DEATH_HOURS = 24L
    private const val HUNGER_DEATH_HOURS = 72L
    private const val FEED_COOLDOWN_HOURS = 6L
    private const val WATER_COOLDOWN_HOURS = 3L
    const val REVIVE_COST = 30
    const val FOOD_QUANTITY = 5
    const val WATER_QUANTITY = 5
    const val THIRST_WARNING_HOURS = 16L
    const val HUNGER_WARNING_HOURS = 48L

    private val PET_GROWTH_DAYS = mapOf(
        40 to 15,
        41 to 20,
        42 to 12,
        43 to 30,
        44 to 18,
        45 to 22,
        46 to 25,
        48 to 10
    )

    private lateinit var appContext: Context
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _foodCount = MutableStateFlow(0)
    override val foodCount: StateFlow<Int> = _foodCount

    private val _waterCount = MutableStateFlow(0)
    override val waterCount: StateFlow<Int> = _waterCount

    override fun initialize(context: Context) {
        appContext = context.applicationContext
        _foodCount.value = prefs.getInt(KEY_FOOD, 0)
        _waterCount.value = prefs.getInt(KEY_WATER, 0)
    }

    override fun addFood(amount: Int) {
        val current = _foodCount.value
        _foodCount.value = current + amount
        prefs.edit().putInt(KEY_FOOD, _foodCount.value).apply()
    }

    override fun addWater(amount: Int) {
        val current = _waterCount.value
        _waterCount.value = current + amount
        prefs.edit().putInt(KEY_WATER, _waterCount.value).apply()
    }

    override fun useFood(): Boolean {
        if (_foodCount.value <= 0) return false
        _foodCount.value -= 1
        prefs.edit().putInt(KEY_FOOD, _foodCount.value).apply()
        return true
    }

    override fun useWater(): Boolean {
        if (_waterCount.value <= 0) return false
        _waterCount.value -= 1
        prefs.edit().putInt(KEY_WATER, _waterCount.value).apply()
        return true
    }

    override fun initializePet(petId: Int) {
        if (isPetInitialized(petId)) return
        val now = System.currentTimeMillis()
        val growthDays = PET_GROWTH_DAYS[petId] ?: 15
        prefs.edit()
            .putLong("${KEY_ACQUIRED}$petId", now)
            .putInt("${KEY_GROWTH_DAYS}$petId", growthDays)
            .putLong("${KEY_LAST_FED}$petId", now)
            .putLong("${KEY_LAST_WATERED}$petId", now)
            .apply()
    }

    override fun isPetInitialized(petId: Int): Boolean {
        return prefs.contains("${KEY_ACQUIRED}$petId")
    }

    override fun canFeedPet(petId: Int): Boolean {
        if (!isPetInitialized(petId)) return false
        if (isPetDead(petId)) return false
        val lastFed = prefs.getLong("${KEY_LAST_FED}$petId", 0L)
        val elapsed = System.currentTimeMillis() - lastFed
        return elapsed >= FEED_COOLDOWN_HOURS * 60 * 60 * 1000L
    }

    override fun canWaterPet(petId: Int): Boolean {
        if (!isPetInitialized(petId)) return false
        if (isPetDead(petId)) return false
        val lastWatered = prefs.getLong("${KEY_LAST_WATERED}$petId", 0L)
        val elapsed = System.currentTimeMillis() - lastWatered
        return elapsed >= WATER_COOLDOWN_HOURS * 60 * 60 * 1000L
    }

    override fun getTimeUntilNextFeed(petId: Int): Long {
        val lastFed = prefs.getLong("${KEY_LAST_FED}$petId", 0L)
        val cooldownMs = FEED_COOLDOWN_HOURS * 60 * 60 * 1000L
        val nextFeed = lastFed + cooldownMs
        return (nextFeed - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun getTimeUntilNextWater(petId: Int): Long {
        val lastWatered = prefs.getLong("${KEY_LAST_WATERED}$petId", 0L)
        val cooldownMs = WATER_COOLDOWN_HOURS * 60 * 60 * 1000L
        val nextWater = lastWatered + cooldownMs
        return (nextWater - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun getLastFedTime(petId: Int): Long {
        return prefs.getLong("${KEY_LAST_FED}$petId", 0L)
    }

    override fun getLastWateredTime(petId: Int): Long {
        return prefs.getLong("${KEY_LAST_WATERED}$petId", 0L)
    }

    override fun feedPet(petId: Int): Boolean {
        if (isPetDead(petId)) return false
        if (!canFeedPet(petId)) return false
        if (!useFood()) return false
        prefs.edit().putLong("${KEY_LAST_FED}$petId", System.currentTimeMillis()).apply()
        return true
    }

    override fun waterPet(petId: Int): Boolean {
        if (isPetDead(petId)) return false
        if (!canWaterPet(petId)) return false
        if (!useWater()) return false
        prefs.edit().putLong("${KEY_LAST_WATERED}$petId", System.currentTimeMillis()).apply()
        return true
    }

    override fun checkPetStatus(petId: Int): PetHealth {
        if (!isPetInitialized(petId)) return PetHealth.ALIVE

        val isDead = prefs.getBoolean("${KEY_IS_DEAD}$petId", false)
        if (isDead) {
            val cause = prefs.getString("${KEY_DEATH_CAUSE}$petId", "hunger")
            return if (cause == "thirst") PetHealth.DEAD_THIRST else PetHealth.DEAD_HUNGER
        }

        val now = System.currentTimeMillis()
        val lastWatered = prefs.getLong("${KEY_LAST_WATERED}$petId", now)
        val lastFed = prefs.getLong("${KEY_LAST_FED}$petId", now)
        val hourMs = 60 * 60 * 1000L

        if (now - lastWatered > THIRST_DEATH_HOURS * hourMs) {
            prefs.edit()
                .putBoolean("${KEY_IS_DEAD}$petId", true)
                .putString("${KEY_DEATH_CAUSE}$petId", "thirst")
                .apply()
            return PetHealth.DEAD_THIRST
        }

        if (now - lastFed > HUNGER_DEATH_HOURS * hourMs) {
            prefs.edit()
                .putBoolean("${KEY_IS_DEAD}$petId", true)
                .putString("${KEY_DEATH_CAUSE}$petId", "hunger")
                .apply()
            return PetHealth.DEAD_HUNGER
        }

        return PetHealth.ALIVE
    }

    override fun revivePet(petId: Int): Boolean {
        if (!isPetInitialized(petId)) return false
        if (!PointsManager.instance.subtractPoints(REVIVE_COST)) return false
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("${KEY_IS_DEAD}$petId", false)
            .remove("${KEY_DEATH_CAUSE}$petId")
            .putLong("${KEY_LAST_FED}$petId", now)
            .putLong("${KEY_LAST_WATERED}$petId", now)
            .apply()
        return true
    }

    override fun getGrowthLevel(petId: Int): Float {
        if (!isPetInitialized(petId)) return 0f
        val acquired = prefs.getLong("${KEY_ACQUIRED}$petId", 0L)
        if (acquired == 0L) return 0f
        val growthDays = prefs.getInt("${KEY_GROWTH_DAYS}$petId", 10)
        val elapsed = System.currentTimeMillis() - acquired
        val totalGrowth = growthDays * 24L * 60 * 60 * 1000L
        return (elapsed.toFloat() / totalGrowth).coerceIn(0f, 1f)
    }

    override fun getGrowthStage(petId: Int): GrowthStage {
        if (!isPetInitialized(petId)) return GrowthStage.BABY
        val level = getGrowthLevel(petId)
        return when {
            level >= 1f -> GrowthStage.FULLY_GROWN
            level >= 0.66f -> GrowthStage.ADULT
            level >= 0.33f -> GrowthStage.ADOLESCENT
            else -> GrowthStage.BABY
        }
    }

    override fun isPetDead(petId: Int): Boolean {
        return prefs.getBoolean("${KEY_IS_DEAD}$petId", false)
    }

    override fun getPetHealth(petId: Int): PetHealth {
        return checkPetStatus(petId)
    }

    override fun getPetGrowthDays(petId: Int): Int {
        return PET_GROWTH_DAYS[petId] ?: 15
    }
}
