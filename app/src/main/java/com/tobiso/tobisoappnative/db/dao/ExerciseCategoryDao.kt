package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.ExerciseCategoryEntity

@Dao
interface ExerciseCategoryDao {
    @Query("SELECT categoryId FROM exercise_category WHERE exerciseId = :exerciseId")
    suspend fun getCategoryIdsForExercise(exerciseId: Int): List<Int>

    @Query("SELECT categoryId FROM exercise_category")
    suspend fun getAllCategoryIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ExerciseCategoryEntity>)

    @Query("DELETE FROM exercise_category")
    suspend fun deleteAll()
}
