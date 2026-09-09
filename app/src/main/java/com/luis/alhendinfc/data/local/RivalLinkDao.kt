package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RivalLinkDao {

    @Query(
        """
        SELECT * FROM rival_link
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun getActiveByClub(opponentClubId: Int): Flow<List<RivalLinkEntity>>

    @Query(
        """
        SELECT * FROM rival_link
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getActiveByClubOnce(opponentClubId: Int): List<RivalLinkEntity>

    @Query("SELECT * FROM rival_link WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): RivalLinkEntity?

    @Query("SELECT * FROM rival_link WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): RivalLinkEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM rival_link ORDER BY id ASC")
    suspend fun getAllOnce(): List<RivalLinkEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RivalLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<RivalLinkEntity>)

    @Update
    suspend fun update(entity: RivalLinkEntity)

    @Query(
        "UPDATE rival_link SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
