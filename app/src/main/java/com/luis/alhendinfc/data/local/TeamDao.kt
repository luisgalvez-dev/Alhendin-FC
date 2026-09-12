package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM team WHERE deletedAt IS NULL ORDER BY id ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM team WHERE isSelected = 1 AND deletedAt IS NULL LIMIT 1")
    fun getSelectedTeam(): Flow<TeamEntity?>

    @Query("SELECT * FROM team WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): TeamEntity?

    @Query("SELECT * FROM team WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): TeamEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM team ORDER BY id ASC")
    suspend fun getAllOnce(): List<TeamEntity>

    @Query("SELECT * FROM team WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Query(
        "UPDATE team SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query("UPDATE team SET isSelected = 0")
    suspend fun deselectAll()

    @Query("UPDATE team SET isSelected = 1 WHERE id = :teamId")
    suspend fun selectById(teamId: Int)

    @Transaction
    suspend fun setSelectedTeam(teamId: Int) {
        deselectAll()
        selectById(teamId)
    }

    @Query("SELECT COUNT(*) FROM team WHERE deletedAt IS NULL")
    suspend fun getTeamCount(): Int
}
