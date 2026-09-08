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

    @Query(
        """
        SELECT * FROM match_table WHERE teamId = :teamId
        ORDER BY CASE WHEN dateEpochDay IS NULL THEN 1 ELSE 0 END,
                 dateEpochDay DESC,
                 id DESC
        """
    )
    fun getMatchesByTeam(teamId: Int): Flow<List<MatchEntity>>

    @Query(
        """
        SELECT * FROM match_table WHERE teamId = :teamId
        ORDER BY CASE WHEN dateEpochDay IS NULL THEN 1 ELSE 0 END,
                 dateEpochDay DESC,
                 id DESC
        """
    )
    suspend fun getMatchesByTeamOnce(teamId: Int): List<MatchEntity>

    @Query("SELECT * FROM match_table WHERE id = :id")
    fun getMatchById(id: Int): Flow<MatchEntity?>

    @Query("SELECT * FROM match_table WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): MatchEntity?

    @Query("SELECT * FROM match_table ORDER BY id ASC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    @Query("SELECT * FROM match_player ORDER BY id ASC")
    suspend fun getAllMatchPlayersOnce(): List<MatchPlayerEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatchPlayers(rows: List<MatchPlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("SELECT * FROM match_player WHERE matchId = :matchId")
    fun getMatchPlayersByMatch(matchId: Int): Flow<List<MatchPlayerEntity>>

    @Query("SELECT * FROM match_player WHERE matchId = :matchId AND playerId = :playerId LIMIT 1")
    suspend fun getMatchPlayerOnce(matchId: Int, playerId: Int): MatchPlayerEntity?

    /**
     * Alta o cambio de convocatoria. En conflicto (matchId, playerId) no toca
     * id, syncId ni createdAt.
     */
    @Query(
        """
        INSERT INTO match_player (
            matchId, playerId, callupStatus, isOnField, syncId, createdAt, updatedAt, deletedAt
        ) VALUES (
            :matchId, :playerId, :callupStatus, :isOnField, :syncId, :createdAt, :updatedAt, :deletedAt
        )
        ON CONFLICT(matchId, playerId) DO UPDATE SET
            callupStatus = excluded.callupStatus,
            isOnField = excluded.isOnField,
            updatedAt = excluded.updatedAt
        """
    )
    suspend fun upsertMatchPlayerPreservingIdentity(
        matchId: Int,
        playerId: Int,
        callupStatus: String,
        isOnField: Boolean,
        syncId: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?
    )

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

    @Query(
        """
        SELECT mp.* FROM match_player mp
        INNER JOIN match_table m ON m.id = mp.matchId
        WHERE m.teamId = :teamId AND m.status = 'FINISHED'
          AND mp.callupStatus IN ('TITULAR', 'SUPLENTE')
        """
    )
    fun getFinishedCallupsByTeam(teamId: Int): Flow<List<MatchPlayerEntity>>

    @Query(
        "UPDATE match_event SET playerId = :keepId, updatedAt = :updatedAt WHERE playerId = :dupId"
    )
    suspend fun reassignEventPlayerId(dupId: Int, keepId: Int, updatedAt: Long)

    @Query(
        "UPDATE match_event SET relatedPlayerId = :keepId, updatedAt = :updatedAt WHERE relatedPlayerId = :dupId"
    )
    suspend fun reassignEventRelatedPlayerId(dupId: Int, keepId: Int, updatedAt: Long)

    @Query("SELECT * FROM match_player WHERE playerId = :playerId")
    suspend fun getMatchPlayersByPlayer(playerId: Int): List<MatchPlayerEntity>

    @Query("DELETE FROM match_player WHERE id = :id")
    suspend fun deleteMatchPlayerById(id: Int)

    @Query("UPDATE match_player SET playerId = :keepId, updatedAt = :updatedAt WHERE id = :rowId")
    suspend fun updateMatchPlayerPlayerId(rowId: Int, keepId: Int, updatedAt: Long)

    @Query(
        """
        UPDATE match_table SET
            status = 'LIVE',
            homeScore = 0,
            awayScore = 0,
            livePeriod = 1,
            liveElapsedSeconds = 0,
            liveClockRunning = 0,
            liveClockAnchorWallMs = 0,
            fieldSecondsJson = '',
            fieldPositionsJson = '',
            updatedAt = :updatedAt
        WHERE id = :matchId AND status = 'OPEN'
        """
    )
    suspend fun markOpenToLive(matchId: Int, updatedAt: Long)

    @Query(
        """
        UPDATE match_table SET
            status = 'FINISHED',
            homeScore = :homeScore,
            awayScore = :awayScore,
            livePeriod = :livePeriod,
            liveElapsedSeconds = :liveElapsedSeconds,
            liveClockRunning = 0,
            liveClockAnchorWallMs = 0,
            fieldSecondsJson = :fieldSecondsJson,
            fieldPositionsJson = :fieldPositionsJson,
            updatedAt = :updatedAt
        WHERE id = :matchId AND status != 'FINISHED'
        """
    )
    suspend fun markFinished(
        matchId: Int,
        homeScore: Int,
        awayScore: Int,
        livePeriod: Int,
        liveElapsedSeconds: Int,
        fieldSecondsJson: String,
        fieldPositionsJson: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE match_table SET fieldPositionsJson = :fieldPositionsJson
        WHERE id = :matchId AND status = 'LIVE'
        """
    )
    suspend fun updateFieldPositions(matchId: Int, fieldPositionsJson: String)

    @Query(
        """
        UPDATE match_table SET
            liveElapsedSeconds = :elapsedSeconds,
            liveClockRunning = :running,
            liveClockAnchorWallMs = :anchorWallMs,
            livePeriod = :period,
            fieldSecondsJson = :fieldSecondsJson
        WHERE id = :matchId AND status = 'LIVE'
        """
    )
    suspend fun updateLiveClock(
        matchId: Int,
        elapsedSeconds: Int,
        running: Boolean,
        anchorWallMs: Long,
        period: Int,
        fieldSecondsJson: String
    )

    @Query(
        """
        UPDATE match_table SET homeScore = :homeScore, awayScore = :awayScore
        WHERE id = :matchId AND status = 'LIVE'
        """
    )
    suspend fun updateLiveScore(matchId: Int, homeScore: Int, awayScore: Int)
}
