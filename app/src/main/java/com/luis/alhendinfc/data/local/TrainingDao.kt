package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {

    @Query(
        """
        SELECT * FROM training
        WHERE teamId = :teamId AND deletedAt IS NULL
        ORDER BY dateEpochDay DESC, id DESC
        """
    )
    fun getByTeam(teamId: Int): Flow<List<TrainingEntity>>

    @Query(
        """
        SELECT * FROM training
        WHERE teamId = :teamId AND dateEpochDay = :epochDay AND deletedAt IS NULL
        ORDER BY id ASC
        """
    )
    suspend fun getActiveByTeamAndDay(teamId: Int, epochDay: Long): List<TrainingEntity>

    @Query("SELECT * FROM training WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getById(id: Int): Flow<TrainingEntity?>

    @Query("SELECT * FROM training WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): TrainingEntity?

    @Query("SELECT * FROM training WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): TrainingEntity?

    @Query("SELECT * FROM training ORDER BY id ASC")
    suspend fun getAllOnce(): List<TrainingEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TrainingEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<TrainingEntity>)

    @Update
    suspend fun update(entity: TrainingEntity)

    @Query(
        "UPDATE training SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
