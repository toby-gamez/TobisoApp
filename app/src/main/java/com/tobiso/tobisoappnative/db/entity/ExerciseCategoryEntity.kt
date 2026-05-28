package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "exercise_category",
    primaryKeys = ["exerciseId", "categoryId"],
    indices = [Index(value = ["categoryId"])]
)
data class ExerciseCategoryEntity(
    val exerciseId: Int,
    val categoryId: Int
)
