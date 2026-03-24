package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val json = Json { ignoreUnknownKeys = true }

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val type: String?,
    val configJson: String?,
    val instructionsMarkdown: String? = null,
    val postIdsJson: String? = null,      // kotlinx.serialization-serialized List<Int>?
    val categoryIdsJson: String? = null,  // kotlinx.serialization-serialized List<Int>?
    val isActive: Boolean? = null
)

fun ExerciseEntity.toDomain(): InteractiveExerciseResponse = InteractiveExerciseResponse(
    id = id,
    title = title,
    type = type,
    configJson = configJson,
    instructionsMarkdown = instructionsMarkdown,
    postIds = postIdsJson?.let { json.decodeFromString<List<Int>>(it) },
    categoryIds = categoryIdsJson?.let { json.decodeFromString<List<Int>>(it) },
    isActive = isActive
)

fun InteractiveExerciseResponse.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    title = title,
    type = type,
    configJson = configJson,
    instructionsMarkdown = instructionsMarkdown,
    postIdsJson = postIds?.let { json.encodeToString(kotlinx.serialization.builtins.ListSerializer(serializer<Int>()), it) },
    categoryIdsJson = categoryIds?.let { json.encodeToString(kotlinx.serialization.builtins.ListSerializer(serializer<Int>()), it) },
    isActive = isActive
)
