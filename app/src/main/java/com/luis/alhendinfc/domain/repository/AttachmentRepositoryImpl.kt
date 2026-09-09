package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.AttachmentEntity
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttachmentRepositoryImpl(
    private val dao: AttachmentDao
) : AttachmentRepository {

    override fun getActiveByParent(parentType: String, parentSyncId: String): Flow<List<Attachment>> =
        dao.getActiveByParent(parentType, parentSyncId).map { list -> list.map { it.toDomain() } }

    override fun getActiveByType(parentType: String): Flow<List<Attachment>> =
        dao.getActiveByType(parentType).map { list -> list.map { it.toDomain() } }

    override suspend fun add(
        parentType: String,
        parentSyncId: String,
        mimeType: String,
        name: String,
        localPath: String,
        syncId: String
    ): Int {
        require(parentType in AttachmentParentType.KNOWN) { "parentType no soportado: $parentType" }
        require(parentSyncId.isNotBlank()) { "parentSyncId vacío" }
        require(localPath.isNotBlank()) { "localPath vacío" }
        val now = EntitySync.now()
        val entity = EntityWrites.attachmentForInsert(
            AttachmentEntity(
                syncId = syncId,
                parentType = parentType,
                parentSyncId = parentSyncId,
                mimeType = mimeType,
                name = name,
                localPath = localPath,
                remotePath = null
            ),
            now
        )
        return dao.insert(entity).toInt()
    }

    override suspend fun delete(attachment: Attachment) {
        // Tombstone de metadata. El fichero local se conserva para no romper revive/sync/backup.
        dao.markDeleted(attachment.id, EntitySync.now())
    }

    override suspend fun deleteByParent(parentType: String, parentSyncId: String) {
        dao.markDeletedByParent(parentType, parentSyncId, EntitySync.now())
    }

    override suspend fun setTaskImage(taskSyncId: String, mimeType: String, name: String, localPath: String) {
        require(mimeType.startsWith("image/")) { "La imagen de tarea debe ser un MIME de imagen" }
        val now = EntitySync.now()
        dao.getActiveByParentOnce(AttachmentParentType.TASK, taskSyncId)
            .filter { it.mimeType.startsWith("image/") }
            .forEach { dao.markDeleted(it.id, now) }
        add(AttachmentParentType.TASK, taskSyncId, mimeType, name, localPath)
    }

    override suspend fun clearTaskImages(taskSyncId: String) {
        val now = EntitySync.now()
        dao.getActiveByParentOnce(AttachmentParentType.TASK, taskSyncId)
            .filter { it.mimeType.startsWith("image/") }
            .forEach { dao.markDeleted(it.id, now) }
    }

    private fun AttachmentEntity.toDomain() = Attachment(
        id = id,
        syncId = syncId,
        parentType = parentType,
        parentSyncId = parentSyncId,
        mimeType = mimeType,
        name = name,
        localPath = localPath,
        remotePath = remotePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
