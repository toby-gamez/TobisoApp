package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.FeedbackEntity

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM pending_feedback ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<FeedbackEntity>

    @Insert
    suspend fun insert(feedback: FeedbackEntity)

    @Query("DELETE FROM pending_feedback WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_feedback SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("SELECT COUNT(*) FROM pending_feedback")
    suspend fun count(): Int
}
