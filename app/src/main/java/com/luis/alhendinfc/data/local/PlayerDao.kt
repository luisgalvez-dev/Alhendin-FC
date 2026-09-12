package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query(
        "SELECT * FROM player WHERE teamId = :teamId AND deletedAt IS NULL ORDER BY jerseyNumber ASC"
    )
    fun getAllByTeam(teamId: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Int): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): PlayerEntity?

    @Query("SELECT * FROM player WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): PlayerEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM player ORDER BY id ASC")
    suspend fun getAllOnce(): List<PlayerEntity>

    @Query("SELECT * FROM player WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Query("SELECT COUNT(*) FROM player WHERE teamId = :teamId AND deletedAt IS NULL")
    suspend fun countByTeam(teamId: Int): Int

    @Query(
        "SELECT * FROM player WHERE teamId = :teamId AND deletedAt IS NULL ORDER BY jerseyNumber ASC, id ASC"
    )
    suspend fun getAllByTeamOnce(teamId: Int): List<PlayerEntity>

    @Update
    suspend fun update(player: PlayerEntity)

    @Query(
        "UPDATE player SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
