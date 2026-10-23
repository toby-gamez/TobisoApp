package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.EventTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    val isLocal: Boolean? = null,
    val type: String? = null,
    val scope: String? = null,
    val classId: Int? = null,
    val className: String? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
    val createdByUserId: Int? = null,
    val createdByName: String? = null,
    val customColor: String? = null,
    val themesJson: String? = null,
    /** null = public/global cache; otherwise the account (see [AuthSession]) these class/group/personal events belong to. */
    val ownerKey: String? = null
)

private val themesJsonCodec = Json { ignoreUnknownKeys = true }

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
    isLocal = isLocal,
    type = type,
    scope = scope,
    classId = classId,
    className = className,
    groupId = groupId,
    groupName = groupName,
    createdByUserId = createdByUserId,
    createdByName = createdByName,
    customColor = customColor,
    themes = themesJson?.let { runCatching { themesJsonCodec.decodeFromString<List<EventTheme>>(it) }.getOrNull() }.orEmpty()
)

fun Event.toEntity(ownerKey: String? = null): EventEntity = EventEntity(
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
    isLocal = isLocal,
    type = type,
    scope = scope,
    classId = classId,
    className = className,
    groupId = groupId,
    groupName = groupName,
    createdByUserId = createdByUserId,
    createdByName = createdByName,
    customColor = customColor,
    themesJson = themes.takeIf { it.isNotEmpty() }?.let { themesJsonCodec.encodeToString(it) },
    ownerKey = ownerKey
)
