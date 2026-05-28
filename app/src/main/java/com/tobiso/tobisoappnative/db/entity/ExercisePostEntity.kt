package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "exercise_post",
    primaryKeys = ["exerciseId", "postId"],
    indices = [Index(value = ["postId"])]
)
data class ExercisePostEntity(
    val exerciseId: Int,
    val postId: Int
)
