package com.tobiso.tobisoappnative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tobiso.tobisoappnative.db.entity.AddendumEntity

@Dao
interface AddendumDao {
    @Query("SELECT * FROM addendums LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = -1, offset: Int = 0): List<AddendumEntity>

    @Query("SELECT * FROM addendums WHERE id = :addendumId LIMIT 1")
    suspend fun getById(addendumId: Int): AddendumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addendums: List<AddendumEntity>)

    @Query("DELETE FROM addendums")
    suspend fun deleteAll()
}
