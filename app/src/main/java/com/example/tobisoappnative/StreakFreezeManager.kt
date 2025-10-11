package com.example.tobisoappnative

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StreakFreezeManager {
    private const val PREFS_NAME = "streak_freeze_prefs"
    private const val KEY_FREEZE_COUNT = "freeze_count"
    private const val KEY_USED_FREEZES = "used_freezes" // Set<String> s daty kdy byly použity
    private const val MAX_FREEZES = 3
    
    private val _availableFreezes = MutableStateFlow(0)
    val availableFreezes: StateFlow<Int> = _availableFreezes
    
    private val _usedFreezes = MutableStateFlow<Set<String>>(emptySet())
    val usedFreezes: StateFlow<Set<String>> = _usedFreezes
    
    fun init(context: Context) {
        val prefs = getPrefs(context)
        _availableFreezes.value = prefs.getInt(KEY_FREEZE_COUNT, 0)
        _usedFreezes.value = prefs.getStringSet(KEY_USED_FREEZES, emptySet()) ?: emptySet()
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Přidá Streak Freeze do inventáře (při nákupu)
     */
    fun addStreakFreeze(context: Context): Boolean {
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_FREEZE_COUNT, 0)
        
        if (currentCount >= MAX_FREEZES) {
            return false // Už má maximum
        }
        
        val newCount = currentCount + 1
        prefs.edit().putInt(KEY_FREEZE_COUNT, newCount).apply()
        _availableFreezes.value = newCount
        
        return true
    }
    
    /**
     * Zkontroluje, zda je potřeba automaticky použít Streak Freeze
     * Zavolá se na začátku aplikace před přidáním dnešní aktivity
     */
    fun checkAndAutoUseFreeze(context: Context): Boolean {
        val streakDays = getStreakDays(context)
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val todayString = today.format(formatter)
        
        // Zkontroluj, zda má k dispozici freeze
        if (_availableFreezes.value <= 0) {
            return false // Nemá žádný freeze
        }
        
        // Zkontroluj pouze včerejšek (nejběžnější případ)
        val yesterday = today.minusDays(1)
        val yesterdayString = yesterday.format(formatter)
        
        // Zkontroluj, zda včera nebyla aktivita a nebyl už použit freeze
        val hasYesterdayActivity = streakDays.contains(yesterdayString)
        val usedFreezes = _usedFreezes.value
        
        if (!hasYesterdayActivity && !usedFreezes.contains(yesterdayString)) {
            // Zkontroluj, zda by freeze zachránil streak
            if (wouldSaveStreak(context, yesterdayString)) {
                println("AUTO-FREEZE: Používám freeze pro včerejšek ($yesterdayString)")
                return useFreeze(context, yesterdayString)
            }
        }
        
        return false
    }
    
    /**
     * Zkontroluje, zda by freeze pro dané datum zachránil/prodloužil streak
     */
    private fun wouldSaveStreak(context: Context, freezeDate: String): Boolean {
        val streakDays = getStreakDays(context).toMutableSet()
        val freezeLocalDate = LocalDate.parse(freezeDate)
        val today = LocalDate.now()
        
        // Pokud by dnes měl aktivitu (očekáváme, že aplikace běží = uživatel je aktivní)
        // a včera chybí aktivita, freeze by prodloužil streak
        val dayBeforeFreeze = freezeLocalDate.minusDays(1)
        val dayAfterFreeze = freezeLocalDate.plusDays(1)
        
        val hasDayBefore = streakDays.contains(dayBeforeFreeze.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        val hasDayAfter = dayAfterFreeze == today // dnes se přidá aktivita
        
        // Freeze má smysl, pokud spojí včerejší aktivitu s dnešní
        return hasDayBefore && hasDayAfter
    }
    
    /**
     * Manuální použití Streak Freeze pro konkrétní datum
     */
    fun useFreeze(context: Context, dateString: String): Boolean {
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_FREEZE_COUNT, 0)
        
        if (currentCount <= 0) {
            return false // Nemá žádný freeze
        }
        
        val usedFreezes = _usedFreezes.value.toMutableSet()
        if (usedFreezes.contains(dateString)) {
            return false // Freeze už byl použit pro toto datum
        }
        
        // Spotřebuj freeze
        val newCount = currentCount - 1
        usedFreezes.add(dateString)
        
        prefs.edit()
            .putInt(KEY_FREEZE_COUNT, newCount)
            .putStringSet(KEY_USED_FREEZES, usedFreezes)
            .apply()
            
        _availableFreezes.value = newCount
        _usedFreezes.value = usedFreezes
        
        println("Streak Freeze použit pro datum: $dateString. Zbývá: $newCount freezes")
        return true
    }
    

    
    /**
     * Získá aktuální počet dostupných freezes
     */
    fun getAvailableFreezes(): Int {
        return _availableFreezes.value
    }
    
    /**
     * Získá data kdy byly freezes použity
     */
    fun getUsedFreezes(): Set<String> {
        return _usedFreezes.value
    }
    
    /**
     * Načte streak data ze StreakScreen
     */
    private fun getStreakDays(context: Context): Set<String> {
        val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
    }
    
    /**
     * Zkontroluje, zda je freeze aktivní pro dané datum
     * (zobrazí se v kalendáři jinak)
     */
    fun isFreezeActive(dateString: String): Boolean {
        return _usedFreezes.value.contains(dateString)
    }
    
    /**
     * Pro debug - resetuje všechny freezes
     */
    fun resetFreezes(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
        _availableFreezes.value = 0
        _usedFreezes.value = emptySet()
    }
}