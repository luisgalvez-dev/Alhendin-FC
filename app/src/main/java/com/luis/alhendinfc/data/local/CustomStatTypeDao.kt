package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomStatTypeDao {

    @Query("SELECT * FROM custom_stat_type WHERE teamId = :teamId ORDER BY sortOrder ASC, id ASC")
    fun getByTeam(teamId: Int): Flow<List<CustomStatTypeEntity>>

    @Query(
        "SELECT * FROM custom_stat_type WHERE teamId = :teamId AND isActive = 1 " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun getActiveByTeam(teamId: Int): Flow<List<CustomStatTypeEntity>>

    @Query("SELECT * FROM custom_stat_type WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CustomStatTypeEntity?

    @Query("SELECT * FROM custom_stat_type ORDER BY id ASC")
    suspend fun getAllOnce(): List<CustomStatTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(entities: List<CustomStatTypeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CustomStatTypeEntity): Long

    @Update
    suspend fun update(entity: CustomStatTypeEntity)

    @Delete
    suspend fun delete(entity: CustomStatTypeEntity)

    @Query("SELECT COUNT(*) FROM match_event WHERE typeCode = :typeCode")
    suspend fun countEventsWithCode(typeCode: String): Int

    @Query("SELECT COUNT(*) FROM custom_stat_type WHERE teamId = :teamId")
    suspend fun countByTeam(teamId: Int): Int

    @Query("SELECT code FROM custom_stat_type WHERE teamId = :teamId")
    suspend fun getCodesByTeam(teamId: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CustomStatTypeEntity>)
}
