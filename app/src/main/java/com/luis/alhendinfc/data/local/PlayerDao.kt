package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM player WHERE teamId = :teamId ORDER BY jerseyNumber ASC")
    fun getAllByTeam(teamId: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE id = :id")
    fun getById(id: Int): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): PlayerEntity?

    @Query("SELECT * FROM player ORDER BY id ASC")
    suspend fun getAllOnce(): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Query("SELECT COUNT(*) FROM player WHERE teamId = :teamId")
    suspend fun countByTeam(teamId: Int): Int

    @Query("SELECT * FROM player WHERE teamId = :teamId ORDER BY jerseyNumber ASC, id ASC")
    suspend fun getAllByTeamOnce(teamId: Int): List<PlayerEntity>

    @Query("DELETE FROM player WHERE id = :playerId")
    suspend fun deleteById(playerId: Int)

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)
}
