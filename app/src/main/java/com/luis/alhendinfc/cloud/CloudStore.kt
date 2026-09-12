package com.luis.alhendinfc.cloud

data class CloudDoc(
    val id: String,
    val data: Map<String, Any?>
) {
    fun str(key: String): String = data[key] as? String ?: ""

    fun strOrNull(key: String): String? {
        val v = data[key] as? String ?: return null
        return v.ifBlank { null }
    }

    fun bool(key: String, default: Boolean = false): Boolean = data[key] as? Boolean ?: default

    fun long(key: String, default: Long = 0L): Long = when (val v = data[key]) {
        is Long -> v
        is Number -> v.toLong()
        else -> default
    }

    fun longOrNull(key: String): Long? = when (val v = data[key]) {
        null -> null
        is Long -> v
        is Number -> v.toLong()
        else -> null
    }

    fun int(key: String, default: Int = 0): Int = when (val v = data[key]) {
        is Int -> v
        is Number -> v.toInt()
        else -> default
    }

    fun intOrNull(key: String): Int? = when (val v = data[key]) {
        null -> null
        is Int -> v
        is Number -> v.toInt()
        else -> null
    }

    val updatedAt: Long get() = long("updatedAt")
    val deletedAt: Long? get() = longOrNull("deletedAt")
    val createdAt: Long get() = long("createdAt")
}

data class WorkspaceMeta(
    val initialized: Boolean,
    val initializedAt: Long?,
    val dataSchemaVersion: Int?
)

fun interface CloudListenHandle {
    fun close()
}

interface CloudStore {
    suspend fun get(collection: String, id: String): CloudDoc?
    suspend fun put(collection: String, id: String, data: Map<String, Any?>)
    suspend fun list(collection: String): List<CloudDoc>
    suspend fun getWorkspace(): WorkspaceMeta?
    /** Reserva el bootstrap. No marca [WorkspaceMeta.initialized]. */
    suspend fun claimBootstrap(): Boolean
    suspend fun markInitialized(initializedAt: Long, schemaVersion: Int)
    suspend fun isMember(uid: String): Boolean
    suspend fun getUser(uid: String): CloudDoc?
    suspend fun putUser(uid: String, data: Map<String, Any?>)
    suspend fun getHomeLayout(uid: String): String?
    suspend fun putHomeLayout(uid: String, encoded: String)
    fun listenCollection(collection: String, onDocs: (List<CloudDoc>) -> Unit): CloudListenHandle
}
