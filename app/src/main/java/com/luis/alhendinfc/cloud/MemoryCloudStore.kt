package com.luis.alhendinfc.cloud

import java.util.concurrent.ConcurrentHashMap

class MemoryCloudStore : CloudStore {
    private val collections = ConcurrentHashMap<String, ConcurrentHashMap<String, Map<String, Any?>>>()
    private val members = ConcurrentHashMap<String, Boolean>()
    private val users = ConcurrentHashMap<String, Map<String, Any?>>()
    private val homeLayouts = ConcurrentHashMap<String, String>()
    private val listeners = ConcurrentHashMap<String, MutableList<(List<CloudDoc>) -> Unit>>()
    var failPuts: Boolean = false
    var failListTypes: Set<String> = emptySet()
    private val lock = Any()

    @Volatile
    private var workspace: Map<String, Any?>? = null

    fun addMember(uid: String) {
        members[uid] = true
    }

    fun setWorkspace(initialized: Boolean, initializedAt: Long? = null, schemaVersion: Int? = null) {
        workspace = buildMap {
            put("initialized", initialized)
            if (initializedAt != null) put("initializedAt", initializedAt)
            if (schemaVersion != null) put("dataSchemaVersion", schemaVersion)
        }
    }

    override suspend fun get(collection: String, id: String): CloudDoc? {
        val data = collections[collection]?.get(id) ?: return null
        return CloudDoc(id, data)
    }

    override suspend fun put(collection: String, id: String, data: Map<String, Any?>) {
        if (failPuts) error("firestore put failed")
        val col = collections.getOrPut(collection) { ConcurrentHashMap() }
        col[id] = HashMap(data)
        notify(collection)
    }

    override suspend fun list(collection: String): List<CloudDoc> {
        if (collection in failListTypes) error("PERMISSION_DENIED")
        val col = collections[collection] ?: return emptyList()
        return col.entries.map { CloudDoc(it.key, it.value) }
    }

    override suspend fun getWorkspace(): WorkspaceMeta? {
        val ws = workspace ?: return null
        val initialized = ws["initialized"] as? Boolean ?: false
        val at = when (val v = ws["initializedAt"]) {
            is Number -> v.toLong()
            else -> null
        }
        val schema = when (val v = ws["dataSchemaVersion"]) {
            is Number -> v.toInt()
            else -> null
        }
        return WorkspaceMeta(initialized, at, schema)
    }

    override suspend fun claimBootstrap(): Boolean {
        synchronized(lock) {
            val current = workspace
            if (current?.get("initialized") == true) return false
            if (current?.get("bootstrapClaimed") == true) return false
            workspace = (current ?: emptyMap()) + mapOf("bootstrapClaimed" to true)
            return true
        }
    }

    override suspend fun markInitialized(initializedAt: Long, schemaVersion: Int) {
        synchronized(lock) {
            workspace = (workspace ?: emptyMap()) + mapOf(
                "initialized" to true,
                "initializedAt" to initializedAt,
                "dataSchemaVersion" to schemaVersion,
                "bootstrapClaimed" to true
            )
        }
    }

    override suspend fun isMember(uid: String): Boolean = members[uid] == true

    override suspend fun getUser(uid: String): CloudDoc? {
        val data = users[uid] ?: return null
        return CloudDoc(uid, data)
    }

    override suspend fun putUser(uid: String, data: Map<String, Any?>) {
        users[uid] = HashMap(data)
    }

    override suspend fun getHomeLayout(uid: String): String? = homeLayouts[uid]

    override suspend fun putHomeLayout(uid: String, encoded: String) {
        homeLayouts[uid] = encoded
    }

    override fun listenCollection(collection: String, onDocs: (List<CloudDoc>) -> Unit): CloudListenHandle {
        val list = listeners.getOrPut(collection) { mutableListOf() }
        synchronized(list) { list.add(onDocs) }
        val docs = collections[collection]?.entries?.map { CloudDoc(it.key, it.value) }.orEmpty()
        onDocs(docs)
        return CloudListenHandle {
            synchronized(list) { list.remove(onDocs) }
        }
    }

    private fun notify(collection: String) {
        val list = listeners[collection] ?: return
        val docs = collections[collection]?.entries?.map { CloudDoc(it.key, it.value) }.orEmpty()
        synchronized(list) {
            list.toList().forEach { it(docs) }
        }
    }
}
