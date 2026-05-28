package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.ExercisePostEntity

@Dao
interface ExercisePostDao {
    @Query("SELECT exerciseId FROM exercise_post WHERE postId = :postId")
    suspend fun getExerciseIdsForPost(postId: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ExercisePostEntity>)

    @Query("DELETE FROM exercise_post")
    suspend fun deleteAll()
}
