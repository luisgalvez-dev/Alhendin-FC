package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchEventDao {

    @Query("SELECT * FROM match_event WHERE matchId = :matchId ORDER BY period ASC, minute ASC, id ASC")
    fun getEventsByMatch(matchId: Int): Flow<List<MatchEventEntity>>

    @Query(
        """
        SELECT e.* FROM match_event e
        INNER JOIN match_table m ON m.id = e.matchId
        WHERE m.teamId = :teamId
        ORDER BY e.createdAt DESC
        """
    )
    fun getEventsByTeam(teamId: Int): Flow<List<MatchEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: MatchEventEntity): Long

    @Delete
    suspend fun delete(event: MatchEventEntity)

    @Query("DELETE FROM match_event WHERE matchId = :matchId")
    suspend fun deleteByMatch(matchId: Int)

    @Query("DELETE FROM match_event WHERE id = :id")
    suspend fun deleteById(id: Int)
}
