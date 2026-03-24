package com.tobiso.tobisoappnative.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Event(
    val id: Int,
    val title: String? = null,
    val description: String? = null,
    @SerialName("startDate") @Serializable(with = DateSerializer::class) val startDate: Date? = null,
    @SerialName("endDate") @Serializable(with = DateSerializer::class) val endDate: Date? = null,
    @SerialName("isAllDay") val isAllDay: Boolean? = null,
    val location: String? = null,
    val color: String? = null,
    @SerialName("isRecurring") val isRecurring: Boolean? = null,
    @SerialName("recurrencePattern") val recurrencePattern: String? = null,
    @SerialName("recurrenceEndDate") @Serializable(with = DateSerializer::class) val recurrenceEndDate: Date? = null,
    @SerialName("isLocal") val isLocal: Boolean? = null
){
    // Pomocné funkce pro bezpečný přístup k datům
    fun getTitleSafe(): String = title ?: "Bez názvu"
    fun getColorSafe(): String = color ?: "#33d17a"
    fun getStartDateSafe(): Date = startDate ?: Date()
    fun getEndDateSafe(): Date = endDate ?: getStartDateSafe()  // Pokud je endDate null, použij startDate
    fun isAllDaySafe(): Boolean = isAllDay ?: false
    fun isRecurringSafe(): Boolean = isRecurring ?: false
    fun isLocalSafe(): Boolean = isLocal ?: false
    
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