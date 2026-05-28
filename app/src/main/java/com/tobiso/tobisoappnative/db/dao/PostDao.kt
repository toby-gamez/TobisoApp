package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM posts LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = -1, offset: Int = 0): List<PostEntity>

    @Query("SELECT * FROM posts WHERE categoryId = :categoryId LIMIT :limit OFFSET :offset")
    suspend fun getByCategory(categoryId: Int, limit: Int = -1, offset: Int = 0): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getById(postId: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

}
