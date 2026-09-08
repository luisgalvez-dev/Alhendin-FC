package com.luis.alhendinfc.data.local

import java.util.UUID

object EntitySync {
    fun newSyncId(): String = UUID.randomUUID().toString()

    fun now(): Long = System.currentTimeMillis()

    fun stampInsert(now: Long = now()): SyncStamp =
        SyncStamp(syncId = newSyncId(), createdAt = now, updatedAt = now, deletedAt = null)

    fun stampTombstone(now: Long = now()): SyncStamp =
        SyncStamp(syncId = "", createdAt = 0L, updatedAt = now, deletedAt = now)

    fun isActive(deletedAt: Long?): Boolean = deletedAt == null
}
