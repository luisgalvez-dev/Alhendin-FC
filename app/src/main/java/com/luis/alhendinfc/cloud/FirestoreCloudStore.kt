package com.luis.alhendinfc.cloud

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.luis.alhendinfc.data.sync.UnavailableCloudException
import kotlinx.coroutines.tasks.await

class FirestoreCloudStore(
    private val firestore: FirebaseFirestore,
    private val workspaceId: String
) : CloudStore {

    private val workspaceRef get() = firestore.collection("workspaces").document(workspaceId)

    override suspend fun get(collection: String, id: String): CloudDoc? {
        val snap = workspaceRef.collection(collection).document(id).get().await()
        if (!snap.exists()) return null
        return CloudDoc(snap.id, snap.data.orEmpty())
    }

    override suspend fun put(collection: String, id: String, data: Map<String, Any?>) {
        workspaceRef.collection(collection).document(id).set(data).await()
    }

    override suspend fun list(collection: String): List<CloudDoc> {
        val snap = workspaceRef.collection(collection).get().await()
        return snap.documents.map { CloudDoc(it.id, it.data.orEmpty()) }
    }

    override suspend fun getWorkspace(): WorkspaceMeta? {
        val snap = workspaceRef.get().await()
        if (!snap.exists()) return null
        val initialized = snap.getBoolean("initialized") ?: false
        val at = snap.getLong("initializedAt")
        val schema = snap.getLong("dataSchemaVersion")?.toInt()
        return WorkspaceMeta(initialized, at, schema)
    }

    override suspend fun claimBootstrap(): Boolean {
        return firestore.runTransaction { tx ->
            val snap = tx.get(workspaceRef)
            if (snap.getBoolean("initialized") == true) return@runTransaction false
            if (snap.getBoolean("bootstrapClaimed") == true) return@runTransaction false
            tx.set(workspaceRef, mapOf("bootstrapClaimed" to true), SetOptions.merge())
            true
        }.await()
    }

    override suspend fun markInitialized(initializedAt: Long, schemaVersion: Int) {
        workspaceRef.set(
            mapOf(
                "initialized" to true,
                "initializedAt" to initializedAt,
                "dataSchemaVersion" to schemaVersion,
                "bootstrapClaimed" to true
            ),
            SetOptions.merge()
        ).await()
    }

    override suspend fun isMember(uid: String): Boolean {
        val snap = workspaceRef.collection("members").document(uid).get().await()
        return snap.exists()
    }

    override suspend fun getUser(uid: String): CloudDoc? {
        val snap = firestore.collection("users").document(uid).get().await()
        if (!snap.exists()) return null
        return CloudDoc(uid, snap.data.orEmpty())
    }

    override suspend fun putUser(uid: String, data: Map<String, Any?>) {
        firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    override suspend fun getHomeLayout(uid: String): String? {
        val snap = firestore.collection("users").document(uid)
            .collection("preferences").document("homeLayout").get().await()
        if (!snap.exists()) return null
        return snap.getString("encoded")
    }

    override suspend fun putHomeLayout(uid: String, encoded: String) {
        firestore.collection("users").document(uid)
            .collection("preferences").document("homeLayout")
            .set(mapOf("encoded" to encoded)).await()
    }

    override fun listenCollection(collection: String, onDocs: (List<CloudDoc>) -> Unit): CloudListenHandle {
        val registration: ListenerRegistration = workspaceRef.collection(collection)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val docs = snap.documents.map { CloudDoc(it.id, it.data.orEmpty()) }
                onDocs(docs)
            }
        return CloudListenHandle { registration.remove() }
    }
}

class UnavailableCloudStore(private val reason: String = FirebaseAvailability.NOT_CONFIGURED) : CloudStore {
    private fun fail(): Nothing = throw UnavailableCloudException(reason)

    override suspend fun get(collection: String, id: String) = fail()
    override suspend fun put(collection: String, id: String, data: Map<String, Any?>) = fail()
    override suspend fun list(collection: String) = fail()
    override suspend fun getWorkspace() = fail()
    override suspend fun claimBootstrap() = fail()
    override suspend fun markInitialized(initializedAt: Long, schemaVersion: Int) = fail()
    override suspend fun isMember(uid: String) = fail()
    override suspend fun getUser(uid: String) = fail()
    override suspend fun putUser(uid: String, data: Map<String, Any?>) = fail()
    override suspend fun getHomeLayout(uid: String) = fail()
    override suspend fun putHomeLayout(uid: String, encoded: String) = fail()
    override fun listenCollection(collection: String, onDocs: (List<CloudDoc>) -> Unit): CloudListenHandle {
        return CloudListenHandle { }
    }
}
