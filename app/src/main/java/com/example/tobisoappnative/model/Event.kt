package com.example.tobisoappnative.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Event(
    val id: Int,
    val title: String?,
    val description: String?,
    @SerializedName("startDate")
    val startDate: Date?,
    @SerializedName("endDate")
    val endDate: Date?,
    @SerializedName("isAllDay")
    val isAllDay: Boolean?,
    val location: String?,
    val color: String?,
    @SerializedName("isRecurring")
    val isRecurring: Boolean?,
    @SerializedName("recurrencePattern")
    val recurrencePattern: String?,
    @SerializedName("recurrenceEndDate")
    val recurrenceEndDate: Date?
) {
    // Pomocné funkce pro bezpečný přístup k datům
    fun getTitleSafe(): String = title ?: "Bez názvu"
    fun getColorSafe(): String = color ?: "#33d17a"
    fun getStartDateSafe(): Date = startDate ?: Date()
    fun getEndDateSafe(): Date = endDate ?: getStartDateSafe()  // Pokud je endDate null, použij startDate
    fun isAllDaySafe(): Boolean = isAllDay ?: false
    fun isRecurringSafe(): Boolean = isRecurring ?: false
    
    // Překlad recurrence pattern do češtiny
    fun getRecurrencePatternCzech(): String {
        return when (recurrencePattern?.lowercase()) {
            "daily" -> "denně"
            "weekly" -> "týdně"
            "monthly" -> "měsíčně"
            "yearly" -> "ročně"
            else -> recurrencePattern ?: "neznámé"
        }
    }
}