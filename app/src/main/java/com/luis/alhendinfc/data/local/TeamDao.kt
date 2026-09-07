package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM team ORDER BY id ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM team WHERE isSelected = 1 LIMIT 1")
    fun getSelectedTeam(): Flow<TeamEntity?>

    @Query("SELECT * FROM team ORDER BY id ASC")
    suspend fun getAllOnce(): List<TeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Insert
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    @Query("UPDATE team SET isSelected = 0")
    suspend fun deselectAll()

    @Query("UPDATE team SET isSelected = 1 WHERE id = :teamId")
    suspend fun selectById(teamId: Int)

    @Transaction
    suspend fun setSelectedTeam(teamId: Int) {
        deselectAll()
        selectById(teamId)
    }

    @Query("SELECT COUNT(*) FROM team")
    suspend fun getTeamCount(): Int
}
