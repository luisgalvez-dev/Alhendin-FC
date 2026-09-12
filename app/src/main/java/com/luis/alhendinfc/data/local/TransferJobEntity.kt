package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Cola persistente de transferencias binarias. No es dato deportivo:
 * no viaja en el ZIP de backup.
 */
@Entity(
    tableName = "transfer_job",
    primaryKeys = ["attachmentSyncId", "kind"],
    indices = [Index(value = ["enqueuedAt"])]
)
data class TransferJobEntity(
    val attachmentSyncId: String,
    val kind: String,
    val enqueuedAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
    val hintPath: String? = null
)

object TransferKind {
    const val UPLOAD = "UPLOAD"
    const val DOWNLOAD = "DOWNLOAD"
    const val DELETE_BLOB = "DELETE_BLOB"
    const val REPLICATE = "REPLICATE"
}
