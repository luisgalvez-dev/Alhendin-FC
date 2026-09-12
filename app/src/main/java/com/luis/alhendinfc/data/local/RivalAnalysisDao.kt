package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RivalAnalysisDao {

    @Query(
        """
        SELECT * FROM rival_analysis
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun getActiveByClub(opponentClubId: Int): Flow<RivalAnalysisEntity?>

    @Query(
        """
        SELECT * FROM rival_analysis
        WHERE opponentClubId = :opponentClubId AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getActiveByClubOnce(opponentClubId: Int): RivalAnalysisEntity?

    @Query(
        """
        SELECT * FROM rival_analysis
        WHERE opponentClubId = :opponentClubId
        LIMIT 1
        """
    )
    suspend fun getByClubIncludingDeleted(opponentClubId: Int): RivalAnalysisEntity?

    @Query("SELECT * FROM rival_analysis WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): RivalAnalysisEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM rival_analysis ORDER BY id ASC")
    suspend fun getAllOnce(): List<RivalAnalysisEntity>

    @Query("SELECT * FROM rival_analysis WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): RivalAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RivalAnalysisEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<RivalAnalysisEntity>)

    @Update
    suspend fun update(entity: RivalAnalysisEntity)

    @Query(
        "UPDATE rival_analysis SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
