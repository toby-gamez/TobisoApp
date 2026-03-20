package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.RelatedPostEntity

@Dao
interface RelatedPostDao {
    @Query("SELECT * FROM related_posts")
    suspend fun getAll(): List<RelatedPostEntity>

    @Query("SELECT * FROM related_posts WHERE postId = :postId")
    suspend fun getByPostId(postId: Int): List<RelatedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relatedPosts: List<RelatedPostEntity>)

    @Query("DELETE FROM related_posts")
    suspend fun deleteAll()
}
