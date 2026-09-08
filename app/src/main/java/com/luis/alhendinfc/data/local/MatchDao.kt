package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query(
        """
        SELECT * FROM match_table WHERE teamId = :teamId AND deletedAt IS NULL
        ORDER BY CASE WHEN dateEpochDay IS NULL THEN 1 ELSE 0 END,
                 dateEpochDay DESC,
                 id DESC
        """
    )
    fun getMatchesByTeam(teamId: Int): Flow<List<MatchEntity>>

    @Query(
        """
        SELECT * FROM match_table WHERE teamId = :teamId AND deletedAt IS NULL
        ORDER BY CASE WHEN dateEpochDay IS NULL THEN 1 ELSE 0 END,
                 dateEpochDay DESC,
                 id DESC
        """
    )
    suspend fun getMatchesByTeamOnce(teamId: Int): List<MatchEntity>

    @Query("SELECT * FROM match_table WHERE id = :id AND deletedAt IS NULL")
    fun getMatchById(id: Int): Flow<MatchEntity?>

    @Query("SELECT * FROM match_table WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): MatchEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM match_table ORDER BY id ASC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    /** Incluye tombstones. Uso interno / backup / futura sync. */
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

    @Query(
        "UPDATE match_table SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query("SELECT * FROM match_player WHERE matchId = :matchId AND deletedAt IS NULL")
    fun getMatchPlayersByMatch(matchId: Int): Flow<List<MatchPlayerEntity>>

    @Query(
        "SELECT * FROM match_player WHERE matchId = :matchId AND playerId = :playerId LIMIT 1"
    )
    suspend fun getMatchPlayerOnce(matchId: Int, playerId: Int): MatchPlayerEntity?

    /**
     * Alta, cambio o reactivación de convocatoria. En conflicto (matchId, playerId)
     * no toca id, syncId ni createdAt. Sí puede limpiar deletedAt al reactivar.
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
            updatedAt = excluded.updatedAt,
            deletedAt = excluded.deletedAt
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

    @Query(
        "UPDATE match_player SET deletedAt = :now, updatedAt = :now WHERE matchId = :matchId AND deletedAt IS NULL"
    )
    suspend fun markDeletedPlayersByMatch(matchId: Int, now: Long)

    @Query(
        """
        UPDATE match_player SET deletedAt = :now, updatedAt = :now
        WHERE matchId = :matchId AND playerId = :playerId AND deletedAt IS NULL
        """
    )
    suspend fun markDeletedPlayer(matchId: Int, playerId: Int, now: Long)

    @Query(
        "UPDATE match_player SET isOnField = :onField WHERE matchId = :matchId AND playerId = :playerId AND deletedAt IS NULL"
    )
    suspend fun setOnField(matchId: Int, playerId: Int, onField: Boolean)

    @Query(
        "UPDATE match_player SET isOnField = 1 WHERE matchId = :matchId AND callupStatus = 'TITULAR' AND deletedAt IS NULL"
    )
    suspend fun putTitularesOnField(matchId: Int)

    @Query(
        "UPDATE match_player SET isOnField = 0 WHERE matchId = :matchId AND deletedAt IS NULL"
    )
    suspend fun clearOnField(matchId: Int)

    @Query(
        """
        SELECT mp.* FROM match_player mp
        INNER JOIN match_table m ON m.id = mp.matchId
        WHERE m.teamId = :teamId AND m.status = 'FINISHED'
          AND m.deletedAt IS NULL AND mp.deletedAt IS NULL
          AND mp.callupStatus IN ('TITULAR', 'SUPLENTE')
        """
    )
    fun getFinishedCallupsByTeam(teamId: Int): Flow<List<MatchPlayerEntity>>

    @Query(
        "UPDATE match_event SET playerId = :keepId, updatedAt = :updatedAt WHERE playerId = :dupId AND deletedAt IS NULL"
    )
    suspend fun reassignEventPlayerId(dupId: Int, keepId: Int, updatedAt: Long)

    @Query(
        "UPDATE match_event SET relatedPlayerId = :keepId, updatedAt = :updatedAt WHERE relatedPlayerId = :dupId AND deletedAt IS NULL"
    )
    suspend fun reassignEventRelatedPlayerId(dupId: Int, keepId: Int, updatedAt: Long)

    @Query("SELECT * FROM match_player WHERE playerId = :playerId")
    suspend fun getMatchPlayersByPlayer(playerId: Int): List<MatchPlayerEntity>

    @Query(
        "UPDATE match_player SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeletedPlayerById(id: Int, now: Long)

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
        WHERE id = :matchId AND status = 'OPEN' AND deletedAt IS NULL
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
        WHERE id = :matchId AND status != 'FINISHED' AND deletedAt IS NULL
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
        WHERE id = :matchId AND status = 'LIVE' AND deletedAt IS NULL
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
        WHERE id = :matchId AND status = 'LIVE' AND deletedAt IS NULL
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
        WHERE id = :matchId AND status = 'LIVE' AND deletedAt IS NULL
        """
    )
    suspend fun updateLiveScore(matchId: Int, homeScore: Int, awayScore: Int)
}
