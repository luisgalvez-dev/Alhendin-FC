package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomStatTypeDao {

    @Query(
        "SELECT * FROM custom_stat_type WHERE teamId = :teamId AND deletedAt IS NULL ORDER BY sortOrder ASC, id ASC"
    )
    fun getByTeam(teamId: Int): Flow<List<CustomStatTypeEntity>>

    @Query(
        "SELECT * FROM custom_stat_type WHERE teamId = :teamId AND isActive = 1 AND deletedAt IS NULL " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun getActiveByTeam(teamId: Int): Flow<List<CustomStatTypeEntity>>

    @Query("SELECT * FROM custom_stat_type WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: Int): CustomStatTypeEntity?

    @Query(
        "SELECT * FROM custom_stat_type WHERE teamId = :teamId AND code = :code LIMIT 1"
    )
    suspend fun getByTeamAndCodeIncludingDeleted(teamId: Int, code: String): CustomStatTypeEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM custom_stat_type ORDER BY id ASC")
    suspend fun getAllOnce(): List<CustomStatTypeEntity>

    @Query("SELECT * FROM custom_stat_type WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): CustomStatTypeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<CustomStatTypeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CustomStatTypeEntity): Long

    @Update
    suspend fun update(entity: CustomStatTypeEntity)

    @Query(
        "UPDATE custom_stat_type SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query(
        """
        SELECT COUNT(*) FROM match_event e
        INNER JOIN match_table m ON m.id = e.matchId
        WHERE e.typeCode = :typeCode AND m.teamId = :teamId
          AND e.deletedAt IS NULL AND m.deletedAt IS NULL
        """
    )
    suspend fun countEventsWithCode(typeCode: String, teamId: Int): Int

    @Query("SELECT COUNT(*) FROM custom_stat_type WHERE teamId = :teamId AND deletedAt IS NULL")
    suspend fun countByTeam(teamId: Int): Int

    @Query("SELECT code FROM custom_stat_type WHERE teamId = :teamId")
    suspend fun getCodesByTeam(teamId: Int): List<String>
}
