package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getById(eventId: Int): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    /** Delete only remote events; keep local events untouched. */
    @Query("DELETE FROM events WHERE isLocal = 0 OR isLocal IS NULL")
    suspend fun deleteRemoteEvents()

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
