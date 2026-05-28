package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = -1, offset: Int = 0): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE postId = :postId LIMIT :limit OFFSET :offset")
    suspend fun getByPostId(postId: Int, limit: Int = -1, offset: Int = 0): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

}
