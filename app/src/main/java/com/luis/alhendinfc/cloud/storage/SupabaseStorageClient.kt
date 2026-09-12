package com.luis.alhendinfc.cloud.storage

import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.ContentType
import java.io.File
import kotlinx.coroutines.tasks.await

class FirebaseIdTokenProvider(
    private val auth: FirebaseAuth
) : IdTokenProvider {
    @Volatile
    private var forcedOnce: Boolean = false

    override suspend fun currentIdToken(): String? {
        val user = auth.currentUser ?: return null
        val force = !forcedOnce
        val token = user.getIdToken(force).await()?.token?.takeIf { it.isNotBlank() }
        if (token != null) forcedOnce = true
        return token
    }
}

class SupabaseStorageClient(
    url: String,
    publishableKey: String,
    private val bucket: String,
    private val tokenProvider: IdTokenProvider
) : BinaryStorageClient {
    init {
        BinaryStorageConfig.requirePublishableKey(publishableKey)
    }

    private val client = createSupabaseClient(url, publishableKey) {
        httpEngine = OkHttp.create()
        accessToken = { tokenProvider.currentIdToken() }
        install(Storage)
    }

    override val isConfigured: Boolean = true

    override suspend fun upload(remotePath: String, file: File, contentType: String) {
        val mime = contentType.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        client.storage.from(bucket).upload(remotePath, file.readBytes()) {
            upsert = true
            this.contentType = ContentType.parse(mime)
        }
    }

    override suspend fun download(remotePath: String, dest: File) {
        val bytes = client.storage.from(bucket).downloadAuthenticated(remotePath)
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
    }

    override suspend fun delete(remotePath: String) {
        runCatching { client.storage.from(bucket).delete(remotePath) }
            .onFailure { error ->
                if (!looksMissing(error)) throw error
            }
    }

    override suspend fun exists(remotePath: String): Boolean {
        return client.storage.from(bucket).exists(remotePath)
    }

    private fun looksMissing(error: Throwable): Boolean {
        val text = error.message.orEmpty().lowercase()
        return text.contains("not found") || text.contains("404") || text.contains("object not found")
    }
}

object BinaryStorageFactory {
    fun create(
        url: String,
        publishableKey: String,
        bucket: String,
        tokenProvider: IdTokenProvider
    ): BinaryStorageClient {
        if (!BinaryStorageConfig.isConfigured(url, publishableKey)) {
            return UnavailableBinaryStorage()
        }
        BinaryStorageConfig.requirePublishableKey(publishableKey)
        return SupabaseStorageClient(url, publishableKey, bucket, tokenProvider)
    }
}
