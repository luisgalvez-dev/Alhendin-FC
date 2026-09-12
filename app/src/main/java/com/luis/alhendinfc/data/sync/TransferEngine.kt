package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.cloud.StoragePath
import com.luis.alhendinfc.cloud.storage.BinaryStorageClient
import com.luis.alhendinfc.cloud.storage.BinaryStorageConfig
import com.luis.alhendinfc.cloud.storage.IdTokenProvider
import com.luis.alhendinfc.data.files.LocalFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.TransferJobEntity
import com.luis.alhendinfc.data.local.TransferKind
import java.io.File

/**
 * Transferencias binarias persistentes. No usa la outbox estructurada.
 * Upload: blob primero, metadata Firestore solo con remotePath.
 * Download: escribe localPath sin updatedAt ni outbox.
 */
class TransferEngine(
    private val db: AlhendinDatabase,
    private val blobs: BinaryStorageClient,
    private val files: LocalFileStore,
    private val workspaceId: String,
    private val isOnline: () -> Boolean,
    private val tokenProvider: IdTokenProvider
) {
    suspend fun enqueue(kind: String, attachmentSyncId: String, hintPath: String? = null) {
        if (attachmentSyncId.isBlank()) return
        val existing = db.transferJobDao().find(attachmentSyncId, kind)
        db.transferJobDao().upsert(
            TransferJobEntity(
                attachmentSyncId = attachmentSyncId,
                kind = kind,
                enqueuedAt = existing?.enqueuedAt ?: EntitySync.now(),
                attempts = existing?.attempts ?: 0,
                lastError = existing?.lastError,
                hintPath = hintPath ?: existing?.hintPath
            )
        )
    }

    suspend fun cancel(kind: String, attachmentSyncId: String) {
        db.transferJobDao().delete(attachmentSyncId, kind)
    }

    suspend fun reconcile() {
        db.attachmentDao().getAllOnce().forEach { att ->
            if (att.syncId.isBlank()) return@forEach
            val hasLocal = !att.localPath.isNullOrBlank() && files.exists(att.localPath)
            val hasRemote = !att.remotePath.isNullOrBlank()
            when {
                att.deletedAt != null && hasRemote -> enqueue(TransferKind.DELETE_BLOB, att.syncId)
                att.deletedAt == null && hasLocal && !hasRemote -> enqueue(TransferKind.UPLOAD, att.syncId)
                att.deletedAt == null && !hasLocal && hasRemote -> enqueue(TransferKind.DOWNLOAD, att.syncId)
            }
        }
    }

    suspend fun processAll() {
        if (!isOnline()) return
        db.transferJobDao().getAll().forEach { job ->
            try {
                when {
                    !blobs.isConfigured -> {
                        db.transferJobDao().markAttempt(
                            job.attachmentSyncId,
                            job.kind,
                            BinaryStorageConfig.ERROR_NOT_CONFIGURED
                        )
                    }
                    tokenProvider.currentIdToken().isNullOrBlank() -> {
                        db.transferJobDao().markAttempt(
                            job.attachmentSyncId,
                            job.kind,
                            BinaryStorageConfig.ERROR_NO_TOKEN
                        )
                    }
                    else -> when (job.kind) {
                        TransferKind.UPLOAD -> processUpload(job)
                        TransferKind.DOWNLOAD -> processDownload(job)
                        TransferKind.DELETE_BLOB -> processDeleteBlob(job)
                        TransferKind.REPLICATE -> processReplicate(job)
                    }
                }
            } catch (e: Exception) {
                db.transferJobDao().markAttempt(
                    job.attachmentSyncId,
                    job.kind,
                    e.message?.take(180)
                )
            }
        }
    }

    private suspend fun processUpload(job: TransferJobEntity) {
        val att = db.attachmentDao().getBySyncIdIncludingDeleted(job.attachmentSyncId)
        if (att == null) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            return
        }
        if (att.deletedAt != null) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            if (!att.remotePath.isNullOrBlank()) enqueue(TransferKind.DELETE_BLOB, att.syncId)
            return
        }
        val local = att.localPath
        if (local.isNullOrBlank() || !files.exists(local)) {
            error("fichero local ausente")
        }
        val remotePath = att.remotePath?.takeIf { it.isNotBlank() }
            ?: StoragePath.blobPath(workspaceId, att.syncId, att.name)
        if (!blobs.exists(remotePath)) {
            blobs.upload(remotePath, File(local), att.mimeType)
        }
        db.attachmentDao().setRemotePath(att.syncId, remotePath)
        SyncHooks.enqueue(SyncEntityType.ATTACHMENT, att.syncId)
        db.transferJobDao().delete(job.attachmentSyncId, job.kind)
    }

    private suspend fun processDownload(job: TransferJobEntity) {
        val att = db.attachmentDao().getBySyncIdIncludingDeleted(job.attachmentSyncId)
        if (att == null || att.deletedAt != null) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            return
        }
        if (!att.localPath.isNullOrBlank() && files.exists(att.localPath)) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            return
        }
        val remotePath = att.remotePath?.takeIf { it.isNotBlank() } ?: error("remotePath ausente")
        val tmp = File.createTempFile("att-${att.syncId}", ".part")
        try {
            blobs.download(remotePath, tmp)
            val path = tmp.inputStream().use { files.importStream(it, att.name.ifBlank { "file" }, att.syncId) }
            db.attachmentDao().setLocalPath(att.syncId, path)
        } finally {
            tmp.delete()
        }
        db.transferJobDao().delete(job.attachmentSyncId, job.kind)
    }

    private suspend fun processDeleteBlob(job: TransferJobEntity) {
        val att = db.attachmentDao().getBySyncIdIncludingDeleted(job.attachmentSyncId)
        val remotePath = att?.remotePath?.takeIf { it.isNotBlank() }
        if (remotePath.isNullOrBlank()) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            return
        }
        blobs.delete(remotePath)
        db.transferJobDao().delete(job.attachmentSyncId, job.kind)
    }

    private suspend fun processReplicate(job: TransferJobEntity) {
        val att = db.attachmentDao().getBySyncIdIncludingDeleted(job.attachmentSyncId)
        if (att == null || att.deletedAt != null) {
            db.transferJobDao().delete(job.attachmentSyncId, job.kind)
            return
        }
        val source = job.hintPath?.takeIf { it.isNotBlank() } ?: error("hintPath ausente")
        if (att.localPath.isNullOrBlank() || !files.exists(att.localPath)) {
            val tmp = File.createTempFile("att-rep-${att.syncId}", ".part")
            try {
                blobs.download(source, tmp)
                val path = tmp.inputStream().use { files.importStream(it, att.name.ifBlank { "file" }, att.syncId) }
                db.attachmentDao().setLocalPath(att.syncId, path)
            } finally {
                tmp.delete()
            }
        }
        db.transferJobDao().delete(job.attachmentSyncId, job.kind)
        enqueue(TransferKind.UPLOAD, att.syncId)
        processUpload(
            TransferJobEntity(
                attachmentSyncId = att.syncId,
                kind = TransferKind.UPLOAD,
                enqueuedAt = EntitySync.now()
            )
        )
    }
}
