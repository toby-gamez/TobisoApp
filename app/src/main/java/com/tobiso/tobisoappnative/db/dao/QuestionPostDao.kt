package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.QuestionPostEntity

@Dao
interface QuestionPostDao {
    @Query("SELECT * FROM questions_posts")
    suspend fun getAll(): List<QuestionPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<QuestionPostEntity>)

    @Query("DELETE FROM questions_posts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM questions_posts")
    suspend fun count(): Int
}
