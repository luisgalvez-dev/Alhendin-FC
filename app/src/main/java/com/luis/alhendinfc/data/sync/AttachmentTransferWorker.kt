package com.luis.alhendinfc.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luis.alhendinfc.cloud.AlhendinCloud

class AttachmentTransferWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val cloud = AlhendinCloud.getInstance(applicationContext)
        return try {
            cloud.transferEngine.reconcile()
            cloud.transferEngine.processAll()
            val remaining = cloud.pendingTransfers()
            if (remaining > 0) Result.retry() else Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
