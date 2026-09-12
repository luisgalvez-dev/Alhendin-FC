package com.luis.alhendinfc.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.luis.alhendinfc.data.local.AttachmentEntity
import com.luis.alhendinfc.data.local.TransferKind
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object TransferHooks {
    @Volatile
    var engine: TransferEngine? = null

    @Volatile
    var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueueUpload(syncId: String) {
        enqueue(TransferKind.UPLOAD, syncId)
    }

    fun enqueueDownload(syncId: String) {
        enqueue(TransferKind.DOWNLOAD, syncId)
    }

    fun enqueueDeleteBlob(syncId: String) {
        enqueue(TransferKind.DELETE_BLOB, syncId)
    }

    fun enqueueReplicate(syncId: String, sourceRemotePath: String) {
        enqueue(TransferKind.REPLICATE, syncId, sourceRemotePath)
    }

    fun enqueue(kind: String, syncId: String, hintPath: String? = null) {
        val eng = engine ?: return
        scope.launch {
            eng.enqueue(kind, syncId, hintPath)
            TransferScheduler.schedule(appContext)
        }
    }

    suspend fun enqueueNow(kind: String, syncId: String, hintPath: String? = null) {
        engine?.enqueue(kind, syncId, hintPath)
        TransferScheduler.schedule(appContext)
    }

    fun cancel(kind: String, syncId: String) {
        val eng = engine ?: return
        scope.launch { eng.cancel(kind, syncId) }
    }

    suspend fun cancelNow(kind: String, syncId: String) {
        engine?.cancel(kind, syncId)
    }

    suspend fun onLocalTombstone(att: AttachmentEntity) {
        cancelNow(TransferKind.UPLOAD, att.syncId)
        cancelNow(TransferKind.DOWNLOAD, att.syncId)
        cancelNow(TransferKind.REPLICATE, att.syncId)
        if (!att.remotePath.isNullOrBlank()) {
            SyncHooks.enqueue(SyncEntityType.ATTACHMENT, att.syncId)
            enqueueNow(TransferKind.DELETE_BLOB, att.syncId)
        }
    }
}

object TransferScheduler {
    const val UNIQUE_WORK = "alhendin-attachment-transfer"

    fun schedule(context: Context?) {
        val ctx = context ?: return
        runCatching {
            val request = OneTimeWorkRequestBuilder<AttachmentTransferWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
