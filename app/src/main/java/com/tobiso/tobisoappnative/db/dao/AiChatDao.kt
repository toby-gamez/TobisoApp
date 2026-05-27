package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.AiChatMessageEntity
import com.tobiso.tobisoappnative.db.entity.AiChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {

    @Insert
    suspend fun insertSession(session: AiChatSessionEntity): Long

    @Query("UPDATE ai_chat_sessions SET lastMessageAt = :t, lastMessagePreview = :preview, messageCount = :count WHERE id = :id")
    suspend fun updateSession(id: Long, t: Long, preview: String, count: Int)

    @Insert
    suspend fun insertMessage(message: AiChatMessageEntity)

    @Query("SELECT * FROM ai_chat_sessions ORDER BY lastMessageAt DESC")
    fun getSessions(): Flow<List<AiChatSessionEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: Long): List<AiChatMessageEntity>

    @Query("DELETE FROM ai_chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}
