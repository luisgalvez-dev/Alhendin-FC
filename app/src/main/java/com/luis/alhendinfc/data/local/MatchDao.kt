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

    @Query("SELECT * FROM match_table ORDER BY id ASC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    @Query("SELECT * FROM match_player ORDER BY id ASC")
    suspend fun getAllMatchPlayersOnce(): List<MatchPlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchPlayers(rows: List<MatchPlayerEntity>)

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

    @Query("UPDATE match_player SET isOnField = :onField WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun setOnField(matchId: Int, playerId: Int, onField: Boolean)

    @Query("UPDATE match_player SET isOnField = 1 WHERE matchId = :matchId AND callupStatus = 'TITULAR'")
    suspend fun putTitularesOnField(matchId: Int)

    @Query("UPDATE match_player SET isOnField = 0 WHERE matchId = :matchId")
    suspend fun clearOnField(matchId: Int)

    /** Convocatorias en partidos finalizados del equipo (para PJ de temporada). */
    @Query(
        """
        SELECT mp.* FROM match_player mp
        INNER JOIN match_table m ON m.id = mp.matchId
        WHERE m.teamId = :teamId AND m.status = 'FINISHED'
          AND mp.callupStatus IN ('TITULAR', 'SUPLENTE')
        """
    )
    fun getFinishedCallupsByTeam(teamId: Int): Flow<List<MatchPlayerEntity>>

    @Query("UPDATE match_event SET playerId = :keepId WHERE playerId = :dupId")
    suspend fun reassignEventPlayerId(dupId: Int, keepId: Int)

    @Query("UPDATE match_event SET relatedPlayerId = :keepId WHERE relatedPlayerId = :dupId")
    suspend fun reassignEventRelatedPlayerId(dupId: Int, keepId: Int)

    @Query("SELECT * FROM match_player WHERE playerId = :playerId")
    suspend fun getMatchPlayersByPlayer(playerId: Int): List<MatchPlayerEntity>

    @Query("DELETE FROM match_player WHERE id = :id")
    suspend fun deleteMatchPlayerById(id: Int)

    @Query(
        "UPDATE match_player SET playerId = :keepId WHERE id = :rowId"
    )
    suspend fun updateMatchPlayerPlayerId(rowId: Int, keepId: Int)
}
