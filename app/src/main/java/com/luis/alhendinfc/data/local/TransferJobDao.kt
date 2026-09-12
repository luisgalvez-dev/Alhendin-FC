package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferJobDao {

    @Query("SELECT * FROM transfer_job ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<TransferJobEntity>

    @Query("SELECT COUNT(*) FROM transfer_job")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transfer_job")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM transfer_job
        WHERE attachmentSyncId = :attachmentSyncId AND kind = :kind
        LIMIT 1
        """
    )
    suspend fun find(attachmentSyncId: String, kind: String): TransferJobEntity?

    @Query(
        """
        SELECT * FROM transfer_job
        WHERE attachmentSyncId = :attachmentSyncId
        ORDER BY enqueuedAt ASC
        """
    )
    fun observeByAttachment(attachmentSyncId: String): Flow<List<TransferJobEntity>>

    @Upsert
    suspend fun upsert(row: TransferJobEntity)

    @Query("DELETE FROM transfer_job WHERE attachmentSyncId = :attachmentSyncId AND kind = :kind")
    suspend fun delete(attachmentSyncId: String, kind: String)

    @Query("DELETE FROM transfer_job WHERE attachmentSyncId = :attachmentSyncId AND kind = :kind")
    suspend fun cancel(attachmentSyncId: String, kind: String)

    @Query(
        """
        UPDATE transfer_job SET attempts = attempts + 1, lastError = :error
        WHERE attachmentSyncId = :attachmentSyncId AND kind = :kind
        """
    )
    suspend fun markAttempt(attachmentSyncId: String, kind: String, error: String?)
}
