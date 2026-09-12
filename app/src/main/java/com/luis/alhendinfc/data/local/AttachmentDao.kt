package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query(
        """
        SELECT * FROM attachment
        WHERE parentType = :parentType AND parentSyncId = :parentSyncId AND deletedAt IS NULL
        ORDER BY id ASC
        """
    )
    fun getActiveByParent(parentType: String, parentSyncId: String): Flow<List<AttachmentEntity>>

    @Query(
        """
        SELECT * FROM attachment
        WHERE parentType = :parentType AND parentSyncId = :parentSyncId AND deletedAt IS NULL
        ORDER BY id ASC
        """
    )
    suspend fun getActiveByParentOnce(parentType: String, parentSyncId: String): List<AttachmentEntity>

    @Query(
        """
        SELECT * FROM attachment
        WHERE parentType = :parentType AND deletedAt IS NULL
        ORDER BY id ASC
        """
    )
    fun getActiveByType(parentType: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): AttachmentEntity?

    @Query("SELECT * FROM attachment WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): AttachmentEntity?

    @Query("SELECT * FROM attachment ORDER BY id ASC")
    suspend fun getAllOnce(): List<AttachmentEntity>

    @Query(
        """
        SELECT * FROM attachment
        WHERE deletedAt IS NULL AND localPath IS NOT NULL AND localPath != ''
        ORDER BY id ASC
        """
    )
    suspend fun getActiveWithFilesOnce(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<AttachmentEntity>)

    @Update
    suspend fun update(entity: AttachmentEntity)

    @Query("UPDATE attachment SET localPath = :localPath WHERE syncId = :syncId")
    suspend fun setLocalPath(syncId: String, localPath: String?)

    @Query("UPDATE attachment SET remotePath = :remotePath WHERE syncId = :syncId")
    suspend fun setRemotePath(syncId: String, remotePath: String?)

    @Query(
        "UPDATE attachment SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)

    @Query(
        """
        UPDATE attachment SET deletedAt = :now, updatedAt = :now
        WHERE parentType = :parentType AND parentSyncId = :parentSyncId AND deletedAt IS NULL
        """
    )
    suspend fun markDeletedByParent(parentType: String, parentSyncId: String, now: Long)
}
