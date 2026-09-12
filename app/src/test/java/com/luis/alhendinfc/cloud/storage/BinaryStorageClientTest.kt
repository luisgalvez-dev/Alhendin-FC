package com.luis.alhendinfc.cloud.storage

import com.luis.alhendinfc.BuildConfig
import com.luis.alhendinfc.cloud.StoragePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryStorageClientTest {

    @Test
    fun factoryWithoutUrlOrKey_returnsUnavailable() {
        val client = BinaryStorageFactory.create("", "", "alhendin-files") { "token" }
        assertFalse(client.isConfigured)
        assertTrue(client is UnavailableBinaryStorage)
    }

    @Test
    fun serviceRoleKey_isRejected() {
        val error = runCatching {
            BinaryStorageFactory.create(
                "https://example.supabase.co",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.service_role.secret",
                "alhendin-files"
            ) { "token" }
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("service_role"))
    }

    @Test
    fun buildConfig_doesNotEmbedServiceRole() {
        assertFalse(BuildConfig.SUPABASE_PUBLISHABLE_KEY.contains("service_role", ignoreCase = true))
        assertEquals("alhendin-files", BuildConfig.SUPABASE_BUCKET)
    }

    @Test
    fun remotePath_isPortableObjectPath_notSignedUrl() {
        val path = StoragePath.blobPath("alhendin-dev", "att-sync-1", "foto.jpg")
        assertEquals("workspaces/alhendin-dev/attachments/att-sync-1/foto.jpg", path)
        assertTrue(StoragePath.isPortableObjectPath(path))
        assertFalse(StoragePath.isPortableObjectPath("https://example.supabase.co/storage/v1/object/sign/alhendin-files/$path?token=abc"))
        assertFalse(StoragePath.isPortableObjectPath("gs://bucket/$path"))
    }
}
