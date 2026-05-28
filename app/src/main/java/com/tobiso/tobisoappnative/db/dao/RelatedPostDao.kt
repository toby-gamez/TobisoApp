package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.RelatedPostEntity

@Dao
interface RelatedPostDao {
    @Query("SELECT * FROM related_posts LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = -1, offset: Int = 0): List<RelatedPostEntity>

    @Query("SELECT * FROM related_posts WHERE postId = :postId LIMIT :limit OFFSET :offset")
    suspend fun getByPostId(postId: Int, limit: Int = -1, offset: Int = 0): List<RelatedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relatedPosts: List<RelatedPostEntity>)

    @Query("DELETE FROM related_posts")
    suspend fun deleteAll()
}
