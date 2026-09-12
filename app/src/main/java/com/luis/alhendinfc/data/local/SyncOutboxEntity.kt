package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_outbox",
    primaryKeys = ["entityType", "entitySyncId"],
    indices = [Index(value = ["enqueuedAt"])]
)
data class SyncOutboxEntity(
    val entityType: String,
    val entitySyncId: String,
    val enqueuedAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null
)
