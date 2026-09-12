package com.luis.alhendinfc.cloud.storage

import java.io.File

/**
 * Capa de binarios. TransferEngine no conoce Firebase Storage ni Supabase.
 * [remotePath] es el object path portable, nunca una URL firmada.
 */
interface BinaryStorageClient {
    val isConfigured: Boolean
    suspend fun upload(remotePath: String, file: File, contentType: String)
    suspend fun download(remotePath: String, dest: File)
    suspend fun delete(remotePath: String)
    suspend fun exists(remotePath: String): Boolean
}

fun interface IdTokenProvider {
    suspend fun currentIdToken(): String?
}

object BinaryStorageConfig {
    const val DEFAULT_BUCKET = "alhendin-files"
    const val ERROR_NOT_CONFIGURED = "Storage no configurado"
    const val ERROR_NO_TOKEN = "Sin token Firebase"

    fun isConfigured(url: String?, publishableKey: String?): Boolean =
        !url.isNullOrBlank() && !publishableKey.isNullOrBlank()

    fun requirePublishableKey(key: String) {
        require(!key.contains("service_role", ignoreCase = true)) {
            "No se admite service_role en el cliente Android"
        }
    }
}

class UnavailableBinaryStorage(
    private val reason: String = BinaryStorageConfig.ERROR_NOT_CONFIGURED
) : BinaryStorageClient {
    override val isConfigured: Boolean = false
    override suspend fun upload(remotePath: String, file: File, contentType: String) = fail()
    override suspend fun download(remotePath: String, dest: File) = fail()
    override suspend fun delete(remotePath: String) = fail()
    override suspend fun exists(remotePath: String) = fail()
    private fun fail(): Nothing = throw IllegalStateException(reason)
}

class MemoryBinaryStorage : BinaryStorageClient {
    private val blobs = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    var failBlobs: Boolean = false
    var uploadedPaths: Int = 0
        private set

    override val isConfigured: Boolean = true

    override suspend fun upload(remotePath: String, file: File, contentType: String) {
        if (failBlobs) error("storage upload failed")
        blobs[remotePath] = file.readBytes()
        uploadedPaths += 1
    }

    override suspend fun download(remotePath: String, dest: File) {
        if (failBlobs) error("storage download failed")
        val bytes = blobs[remotePath] ?: error("blob missing: $remotePath")
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
    }

    override suspend fun delete(remotePath: String) {
        if (failBlobs) error("storage delete failed")
        blobs.remove(remotePath)
    }

    override suspend fun exists(remotePath: String): Boolean = blobs.containsKey(remotePath)

    fun blobCount(): Int = blobs.size
}
