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
    @SerialName("isLocal") val isLocal: Boolean? = null,
    // ── Added by the web with school classes / scoped events (all optional → old payloads still parse) ──
    @Serializable(with = LenientStringSerializer::class) val type: String? = null,
    @Serializable(with = LenientStringSerializer::class) val scope: String? = null,
    val classId: Int? = null,
    val className: String? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
    val createdByUserId: Int? = null,
    val createdByName: String? = null,
    /** The admin's own pick; [color] is already the effective color. */
    val customColor: String? = null,
    val themes: List<EventTheme> = emptyList()
){
    // Pomocné funkce pro bezpečný přístup k datům
    fun getTitleSafe(): String = title ?: "Bez názvu"
    fun getColorSafe(): String = color ?: eventType?.defaultColor ?: "#33d17a"

    val eventType: EventType? get() = EventType.parse(type)
    val eventScope: EventScope? get() = EventScope.parse(scope)
    /** True for events a student added (as opposed to admin / local ones). */
    val isStudentCreated: Boolean get() = createdByUserId != null
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