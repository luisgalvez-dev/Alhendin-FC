package com.luis.alhendinfc.data.sync

import androidx.room.withTransaction
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.SyncOutboxEntity

class SyncWriteGate(private val db: AlhendinDatabase) {
    suspend fun <T> local(type: String, syncId: String, block: suspend () -> T): T {
        return db.withTransaction {
            val result = block()
            if (syncId.isNotBlank()) {
                db.syncOutboxDao().upsert(
                    SyncOutboxEntity(
                        entityType = type,
                        entitySyncId = syncId,
                        enqueuedAt = EntitySync.now(),
                        attempts = 0,
                        lastError = null
                    )
                )
            }
            result
        }
    }

    suspend fun enqueue(type: String, syncId: String) {
        if (syncId.isBlank()) return
        db.syncOutboxDao().upsert(
            SyncOutboxEntity(
                entityType = type,
                entitySyncId = syncId,
                enqueuedAt = EntitySync.now(),
                attempts = 0,
                lastError = null
            )
        )
    }

    suspend fun <T> localMany(items: List<Pair<String, String>>, block: suspend () -> T): T {
        return db.withTransaction {
            val result = block()
            items.forEach { (type, syncId) ->
                if (syncId.isNotBlank()) {
                    db.syncOutboxDao().upsert(
                        SyncOutboxEntity(
                            entityType = type,
                            entitySyncId = syncId,
                            enqueuedAt = EntitySync.now(),
                            attempts = 0,
                            lastError = null
                        )
                    )
                }
            }
            result
        }
    }

    suspend fun <T> applyRemote(block: suspend () -> T): T = db.withTransaction { block() }
}

object SyncHooks {
    @Volatile
    var gate: SyncWriteGate? = null

    suspend fun <T> local(type: String, syncId: String, block: suspend () -> T): T {
        val g = gate
        return if (g == null) block() else g.local(type, syncId, block)
    }

    suspend fun enqueue(type: String, syncId: String) {
        gate?.enqueue(type, syncId)
    }

    suspend fun <T> localMany(items: List<Pair<String, String>>, block: suspend () -> T): T {
        val g = gate
        return if (g == null) block() else g.localMany(items, block)
    }

    suspend fun <T> applyRemote(block: suspend () -> T): T {
        val g = gate
        return if (g == null) block() else g.applyRemote(block)
    }
}
