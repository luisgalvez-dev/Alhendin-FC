package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchEventDao {

    @Query(
        """
        SELECT * FROM match_event
        WHERE matchId = :matchId AND deletedAt IS NULL
        ORDER BY period ASC, minute ASC, id ASC
        """
    )
    fun getEventsByMatch(matchId: Int): Flow<List<MatchEventEntity>>

    @Query(
        """
        SELECT e.* FROM match_event e
        INNER JOIN match_table m ON m.id = e.matchId
        WHERE m.teamId = :teamId AND m.status = 'FINISHED'
          AND e.deletedAt IS NULL AND m.deletedAt IS NULL
        ORDER BY e.createdAt DESC
        """
    )
    fun getEventsByTeam(teamId: Int): Flow<List<MatchEventEntity>>

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM match_event ORDER BY id ASC")
    suspend fun getAllOnce(): List<MatchEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<MatchEventEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: MatchEventEntity): Long

    @Query(
        "UPDATE match_event SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query(
        "UPDATE match_event SET deletedAt = :now, updatedAt = :now WHERE matchId = :matchId AND deletedAt IS NULL"
    )
    suspend fun markDeletedByMatch(matchId: Int, now: Long)
}
