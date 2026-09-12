package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.AttachmentEntity
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.TransferKind
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.data.sync.TransferHooks
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.AttachmentRules
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
        AttachmentRules.requireAllowedMime(mimeType)
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
        val id = dao.insert(entity).toInt()
        val stored = dao.getByIdOnce(id) ?: return id
        TransferHooks.enqueueNow(TransferKind.UPLOAD, stored.syncId)
        return id
    }

    override suspend fun delete(attachment: Attachment) {
        val row = dao.getByIdOnce(attachment.id) ?: dao.getBySyncIdIncludingDeleted(attachment.syncId)
        dao.markDeleted(attachment.id, EntitySync.now())
        if (row != null) TransferHooks.onLocalTombstone(row)
    }

    override suspend fun deleteByParent(parentType: String, parentSyncId: String) {
        val rows = dao.getActiveByParentOnce(parentType, parentSyncId)
        dao.markDeletedByParent(parentType, parentSyncId, EntitySync.now())
        rows.forEach { TransferHooks.onLocalTombstone(it) }
    }

    override suspend fun setTaskImage(taskSyncId: String, mimeType: String, name: String, localPath: String) {
        require(mimeType.startsWith("image/")) { "La imagen de tarea debe ser un MIME de imagen" }
        AttachmentRules.requireAllowedMime(mimeType)
        val now = EntitySync.now()
        val previous = dao.getActiveByParentOnce(AttachmentParentType.TASK, taskSyncId)
            .filter { it.mimeType.startsWith("image/") }
        previous.forEach { dao.markDeleted(it.id, now) }
        previous.forEach { TransferHooks.onLocalTombstone(it) }
        add(AttachmentParentType.TASK, taskSyncId, mimeType, name, localPath)
    }

    override suspend fun clearTaskImages(taskSyncId: String) {
        val now = EntitySync.now()
        val previous = dao.getActiveByParentOnce(AttachmentParentType.TASK, taskSyncId)
            .filter { it.mimeType.startsWith("image/") }
        previous.forEach { dao.markDeleted(it.id, now) }
        previous.forEach { TransferHooks.onLocalTombstone(it) }
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
