package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Event
import java.util.Date

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Int,
    val title: String? = null,
    val description: String? = null,
    val startDate: Long? = null,          // Date stored as epoch milliseconds
    val endDate: Long? = null,
    val isAllDay: Boolean? = null,
    val location: String? = null,
    val color: String? = null,
    val isRecurring: Boolean? = null,
    val recurrencePattern: String? = null,
    val recurrenceEndDate: Long? = null,
    val isLocal: Boolean? = null
)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    title = title,
    description = description,
    startDate = startDate?.let { Date(it) },
    endDate = endDate?.let { Date(it) },
    isAllDay = isAllDay,
    location = location,
    color = color,
    isRecurring = isRecurring,
    recurrencePattern = recurrencePattern,
    recurrenceEndDate = recurrenceEndDate?.let { Date(it) },
    isLocal = isLocal
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    description = description,
    startDate = startDate?.time,
    endDate = endDate?.time,
    isAllDay = isAllDay,
    location = location,
    color = color,
    isRecurring = isRecurring,
    recurrencePattern = recurrencePattern,
    recurrenceEndDate = recurrenceEndDate?.time,
    isLocal = isLocal
)
