package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = -1, offset: Int = 0): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getById(eventId: Int): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    /** Public cache plus (when [ownerKey] is set) that account's class/group/personal events. */
    @Query("SELECT * FROM events WHERE ownerKey IS NULL OR ownerKey = :ownerKey")
    suspend fun getVisibleTo(ownerKey: String?): List<EventEntity>

    /** Delete only the public remote events; keep local events and per-account caches untouched. */
    @Query("DELETE FROM events WHERE (isLocal = 0 OR isLocal IS NULL) AND ownerKey IS NULL")
    suspend fun deleteRemoteEvents()

    @Query("DELETE FROM events WHERE ownerKey = :ownerKey")
    suspend fun deleteOwnedBy(ownerKey: String)

    @Query("DELETE FROM events WHERE ownerKey IS NOT NULL")
    suspend fun deleteAllOwned()

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
