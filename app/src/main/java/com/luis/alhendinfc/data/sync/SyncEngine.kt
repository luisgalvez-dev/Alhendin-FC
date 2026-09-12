package com.luis.alhendinfc.data.sync

import androidx.room.withTransaction
import com.luis.alhendinfc.cloud.CloudConfig
import com.luis.alhendinfc.cloud.CloudDoc
import com.luis.alhendinfc.cloud.CloudStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.EntitySync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncUiStatus {
    SYNCED,
    PENDING,
    OFFLINE,
    UNAVAILABLE
}

class SyncEngine(
    private val db: AlhendinDatabase,
    private val store: CloudStore,
    private val registry: SyncRegistry,
    private val isOnline: () -> Boolean,
    private val schemaVersion: Int = CloudConfig.dataSchemaVersion,
    private val onWonBootstrap: suspend () -> Unit = {}
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SyncUiStatus.SYNCED)
    val status: StateFlow<SyncUiStatus> = _status.asStateFlow()
    private var bootstrapped = false
    private val deferred = mutableListOf<Pair<String, CloudDoc>>()

    suspend fun sync() {
        mutex.withLock {
            if (!isOnline()) {
                _status.value = if (db.syncOutboxDao().count() > 0) SyncUiStatus.PENDING else SyncUiStatus.OFFLINE
                return
            }
            try {
                bootstrapIfNeeded()
                pull()
                retryDeferred()
                pushOutbox()
                val pending = db.syncOutboxDao().count()
                _status.value = if (pending > 0) SyncUiStatus.PENDING else SyncUiStatus.SYNCED
            } catch (_: UnavailableCloudException) {
                _status.value = SyncUiStatus.UNAVAILABLE
            } catch (_: Exception) {
                _status.value = if (isOnline()) SyncUiStatus.PENDING else SyncUiStatus.OFFLINE
            }
        }
    }

    suspend fun applyRemoteCollection(type: String, docs: List<CloudDoc>) {
        mutex.withLock {
            db.withTransaction {
                docs.forEach { doc ->
                    when (applyIncoming(type, doc)) {
                        RemoteApplyResult.DEFERRED -> deferred.add(type to doc)
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun bootstrapIfNeeded() {
        if (bootstrapped) return
        val meta = store.getWorkspace()
        val localData = registry.hasLocalSportsData()
        val cloudInit = meta?.initialized == true
        when {
            !cloudInit -> {
                val claimed = store.claimBootstrap()
                if (claimed) {
                    onWonBootstrap()
                    uploadAll()
                    store.markInitialized(EntitySync.now(), schemaVersion)
                } else {
                    joinInitializedWorkspace()
                }
            }
            cloudInit && !localData -> {
                downloadAll()
            }
            cloudInit && localData -> {
                joinInitializedWorkspace()
            }
        }
        bootstrapped = true
    }

    private suspend fun joinInitializedWorkspace() {
        val remoteIds = allRemoteSyncIds()
        DevSeedSync.retractLocalDemosNotIn(db, remoteIds)
        reconcile()
    }

    private suspend fun allRemoteSyncIds(): Set<String> {
        return SyncEntityType.DOWNLOAD_ORDER.flatMap { type ->
            store.list(type).map { it.id }
        }.toSet()
    }

    private suspend fun uploadAll() {
        SyncEntityType.DOWNLOAD_ORDER.forEach { type ->
            val adapter = registry.adapter(type) ?: return@forEach
            adapter.listLocal().forEach { doc ->
                if (adapter.shouldPush(doc)) {
                    store.put(type, doc.id, doc.data)
                }
            }
        }
    }

    private suspend fun downloadAll() {
        pull()
        retryDeferred()
    }

    private suspend fun reconcile() {
        val remoteIds = mutableMapOf<String, Set<String>>()
        SyncEntityType.DOWNLOAD_ORDER.forEach { type ->
            val adapter = registry.adapter(type) ?: return@forEach
            val docs = store.list(type)
            remoteIds[type] = docs.map { it.id }.toSet()
            db.withTransaction {
                docs.forEach { doc ->
                    when (applyIncoming(type, doc)) {
                        RemoteApplyResult.DEFERRED -> deferred.add(type to doc)
                        else -> Unit
                    }
                }
            }
        }
        retryDeferred()
        SyncEntityType.DOWNLOAD_ORDER.forEach { type ->
            val adapter = registry.adapter(type) ?: return@forEach
            val known = remoteIds[type].orEmpty()
            adapter.listLocal().forEach { local ->
                if (local.id !in known && adapter.shouldPush(local) && !DevSeedSync.isDemoDoc(local)) {
                    SyncHooks.enqueue(type, local.id)
                }
            }
        }
    }

    private suspend fun pull() {
        SyncEntityType.DOWNLOAD_ORDER.forEach { type ->
            val docs = store.list(type)
            db.withTransaction {
                docs.forEach { doc ->
                    when (applyIncoming(type, doc)) {
                        RemoteApplyResult.DEFERRED -> deferred.add(type to doc)
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun retryDeferred() {
        repeat(6) {
            if (deferred.isEmpty()) return
            val batch = deferred.toList()
            deferred.clear()
            var progressed = false
            db.withTransaction {
                batch.forEach { (type, doc) ->
                    when (applyIncoming(type, doc)) {
                        RemoteApplyResult.DEFERRED -> deferred.add(type to doc)
                        else -> progressed = true
                    }
                }
            }
            if (!progressed) return
        }
    }

    /**
     * Un snapshot remoto anterior no pisa un cambio local aún no publicado.
     * Si el remoto es realmente más nuevo, LWW sigue aplicando.
     */
    private suspend fun applyIncoming(type: String, doc: CloudDoc): RemoteApplyResult {
        val adapter = registry.adapter(type) ?: return RemoteApplyResult.IGNORED
        val pending = db.syncOutboxDao().find(type, doc.id)
        if (pending != null) {
            val local = adapter.readLocal(doc.id)
            val localStamp = maxOf(
                local?.updatedAt ?: 0L,
                local?.deletedAt ?: 0L,
                pending.enqueuedAt
            )
            val remoteStamp = maxOf(doc.updatedAt, doc.deletedAt ?: 0L)
            if (remoteStamp <= localStamp) {
                return RemoteApplyResult.IGNORED
            }
        }
        return adapter.applyRemote(doc)
    }

    private suspend fun pushOutbox() {
        val order = SyncEntityType.DOWNLOAD_ORDER.withIndex().associate { it.value to it.index }
        val items = db.syncOutboxDao().getAll().sortedBy { order[it.entityType] ?: Int.MAX_VALUE }
        items.forEach { row ->
            val adapter = registry.adapter(row.entityType) ?: run {
                db.syncOutboxDao().delete(row.entityType, row.entitySyncId)
                return@forEach
            }
            val local = adapter.readLocal(row.entitySyncId)
            if (local == null) {
                db.syncOutboxDao().delete(row.entityType, row.entitySyncId)
                return@forEach
            }
            if (!adapter.shouldPush(local)) {
                return@forEach
            }
            try {
                store.put(row.entityType, row.entitySyncId, local.data)
                db.syncOutboxDao().delete(row.entityType, row.entitySyncId)
            } catch (e: Exception) {
                db.syncOutboxDao().markAttempt(
                    row.entityType,
                    row.entitySyncId,
                    e.message?.take(180)
                )
                throw e
            }
        }
    }
}

class UnavailableCloudException(message: String) : IllegalStateException(message)
