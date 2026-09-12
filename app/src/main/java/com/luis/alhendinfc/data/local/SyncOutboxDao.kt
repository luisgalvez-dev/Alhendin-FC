package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Int>

    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE entityType = :entityType AND entitySyncId = :entitySyncId
        LIMIT 1
        """
    )
    suspend fun find(entityType: String, entitySyncId: String): SyncOutboxEntity?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM sync_outbox
            WHERE entityType = :entityType AND entitySyncId = :entitySyncId
        )
        """
    )
    suspend fun exists(entityType: String, entitySyncId: String): Boolean

    @Upsert
    suspend fun upsert(row: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entitySyncId = :entitySyncId")
    suspend fun delete(entityType: String, entitySyncId: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clear()

    @Query(
        """
        UPDATE sync_outbox SET attempts = attempts + 1, lastError = :error
        WHERE entityType = :entityType AND entitySyncId = :entitySyncId
        """
    )
    suspend fun markAttempt(entityType: String, entitySyncId: String, error: String?)
}
