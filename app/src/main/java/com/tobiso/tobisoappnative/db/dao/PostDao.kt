package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE categoryId = :categoryId")
    suspend fun getByCategory(categoryId: Int): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getById(postId: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int
}
