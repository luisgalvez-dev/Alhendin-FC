package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM match_table WHERE teamId = :teamId ORDER BY date DESC, id DESC")
    fun getMatchesByTeam(teamId: Int): Flow<List<MatchEntity>>

    @Query("SELECT * FROM match_table WHERE id = :id")
    fun getMatchById(id: Int): Flow<MatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("SELECT * FROM match_player WHERE matchId = :matchId")
    fun getMatchPlayersByMatch(matchId: Int): Flow<List<MatchPlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatchPlayer(mp: MatchPlayerEntity)

    @Query("DELETE FROM match_player WHERE matchId = :matchId")
    suspend fun deleteMatchPlayersByMatch(matchId: Int)

    @Query("DELETE FROM match_player WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun deleteMatchPlayer(matchId: Int, playerId: Int)
}
