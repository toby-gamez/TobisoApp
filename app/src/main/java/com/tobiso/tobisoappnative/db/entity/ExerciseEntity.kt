package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse

private val gson = Gson()

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val type: String?,
    val configJson: String?,
    val instructionsMarkdown: String? = null,
    val postIdsJson: String? = null,      // Gson-serialized List<Int>?
    val categoryIdsJson: String? = null,  // Gson-serialized List<Int>?
    val isActive: Boolean? = null
)

fun ExerciseEntity.toDomain(): InteractiveExerciseResponse = InteractiveExerciseResponse(
    id = id,
    title = title,
    type = type,
    configJson = configJson,
    instructionsMarkdown = instructionsMarkdown,
    postIds = postIdsJson?.let { gson.fromJson(it, Array<Int>::class.java)?.toList() },
    categoryIds = categoryIdsJson?.let { gson.fromJson(it, Array<Int>::class.java)?.toList() },
    isActive = isActive
)

fun InteractiveExerciseResponse.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    title = title,
    type = type,
    configJson = configJson,
    instructionsMarkdown = instructionsMarkdown,
    postIdsJson = postIds?.let { gson.toJson(it) },
    categoryIdsJson = categoryIds?.let { gson.toJson(it) },
    isActive = isActive
)
