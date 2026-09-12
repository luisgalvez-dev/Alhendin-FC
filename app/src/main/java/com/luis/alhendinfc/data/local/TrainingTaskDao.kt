package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingTaskDao {

    @Query(
        """
        SELECT * FROM training_task
        WHERE trainingId = :trainingId AND deletedAt IS NULL
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun getActiveByTraining(trainingId: Int): Flow<List<TrainingTaskEntity>>

    @Query(
        """
        SELECT * FROM training_task
        WHERE trainingId = :trainingId AND deletedAt IS NULL
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getActiveByTrainingOnce(trainingId: Int): List<TrainingTaskEntity>

    @Query(
        """
        SELECT * FROM training_task
        WHERE trainingId = :trainingId AND taskId = :taskId
        LIMIT 1
        """
    )
    suspend fun getByTrainingAndTaskIncludingDeleted(trainingId: Int, taskId: Int): TrainingTaskEntity?

    @Query("SELECT * FROM training_task ORDER BY id ASC")
    suspend fun getAllOnce(): List<TrainingTaskEntity>

    @Query("SELECT * FROM training_task WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): TrainingTaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TrainingTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<TrainingTaskEntity>)

    @Update
    suspend fun update(entity: TrainingTaskEntity)

    @Query(
        "UPDATE training_task SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query(
        """
        UPDATE training_task SET deletedAt = :now, updatedAt = :now
        WHERE trainingId = :trainingId AND deletedAt IS NULL
        """
    )
    suspend fun markDeletedByTraining(trainingId: Int, now: Long)
}
