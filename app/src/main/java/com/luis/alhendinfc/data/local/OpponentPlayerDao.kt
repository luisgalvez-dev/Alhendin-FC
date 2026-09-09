package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentPlayerDao {

    @Query(
        """
        SELECT * FROM opponent_player
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    fun getActiveByClub(opponentClubId: Int): Flow<List<OpponentPlayerEntity>>

    @Query(
        """
        SELECT * FROM opponent_player
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
          AND LOWER(name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    fun searchByClubAndName(opponentClubId: Int, query: String): Flow<List<OpponentPlayerEntity>>

    @Query("SELECT * FROM opponent_player WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): OpponentPlayerEntity?

    @Query("SELECT * FROM opponent_player WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): OpponentPlayerEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM opponent_player ORDER BY id ASC")
    suspend fun getAllOnce(): List<OpponentPlayerEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OpponentPlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<OpponentPlayerEntity>)

    @Update
    suspend fun update(entity: OpponentPlayerEntity)

    @Query(
        "UPDATE opponent_player SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
