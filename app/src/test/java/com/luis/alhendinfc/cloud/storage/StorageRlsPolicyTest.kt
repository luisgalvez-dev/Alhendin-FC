package com.luis.alhendinfc.cloud.storage

import com.luis.alhendinfc.cloud.StoragePath
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageRlsPolicyTest {

    private val devPath = StoragePath.blobPath("alhendin-dev", "att-1", "foto.jpg")
    private val prodPath = StoragePath.blobPath("alhendin", "att-1", "foto.jpg")

    @Test
    fun authenticatedDev_canAccessDev() {
        assertTrue(
            StorageObjectAccess.allows(
                objectName = devPath,
                role = "authenticated",
                workspaces = listOf("alhendin-dev")
            )
        )
    }

    @Test
    fun authenticatedDev_cannotAccessProduction() {
        assertFalse(
            StorageObjectAccess.allows(
                objectName = prodPath,
                role = "authenticated",
                workspaces = listOf("alhendin-dev")
            )
        )
    }

    @Test
    fun authenticatedWithoutWorkspaces_denied() {
        assertFalse(
            StorageObjectAccess.allows(
                objectName = devPath,
                role = "authenticated",
                workspaces = null
            )
        )
        assertFalse(
            StorageObjectAccess.allows(
                objectName = devPath,
                role = "authenticated",
                workspaces = emptyList()
            )
        )
    }

    @Test
    fun anon_denied() {
        assertFalse(
            StorageObjectAccess.allows(
                objectName = devPath,
                role = null,
                workspaces = listOf("alhendin-dev")
            )
        )
        assertFalse(
            StorageObjectAccess.allows(
                objectName = devPath,
                role = "anon",
                workspaces = listOf("alhendin-dev")
            )
        )
    }

    @Test
    fun pathOutsideAttachments_denied() {
        assertFalse(
            StorageObjectAccess.allows(
                objectName = "workspaces/alhendin-dev/other/att-1/foto.jpg",
                role = "authenticated",
                workspaces = listOf("alhendin-dev")
            )
        )
        assertFalse(
            StorageObjectAccess.allows(
                objectName = "private/alhendin-dev/attachments/att-1/foto.jpg",
                role = "authenticated",
                workspaces = listOf("alhendin-dev")
            )
        )
        assertFalse(
            StorageObjectAccess.allows(
                objectName = "foto.jpg",
                role = "authenticated",
                workspaces = listOf("alhendin-dev")
            )
        )
    }

    @Test
    fun sqlPolicies_useClaimMembership_notBlanketWorkspaces() {
        val sql = readPoliciesSql()
        assertTrue(sql.contains("bucket_id = 'alhendin-files'"))
        assertTrue(sql.contains("auth.jwt()->>'role', '') = 'authenticated'"))
        assertTrue(sql.contains("https://securetoken.google.com/alhendinfc"))
        assertTrue(sql.contains("auth.jwt()->>'aud', '') = 'alhendinfc'"))
        assertTrue(sql.contains("split_part(name, '/', 3) = 'attachments'"))
        assertTrue(sql.contains("auth.jwt()->'alhendin_workspaces'"))
        assertFalse(
            sql.contains("split_part(name, '/', 2) in ('alhendin-dev', 'alhendin')")
        )
        assertTrue(sql.contains("to authenticated"))
        assertFalse(sql.contains("to anon"))
        assertFalse(sql.contains("to public"))
    }

    private fun readPoliciesSql(): String {
        val candidates = listOf(
            File("supabase/storage-policies.sql"),
            File("../supabase/storage-policies.sql")
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("No se encuentra supabase/storage-policies.sql")
        return file.readText()
    }
}

internal object StorageObjectAccess {
    const val BUCKET = "alhendin-files"
    const val ISSUER = "https://securetoken.google.com/alhendinfc"
    const val AUDIENCE = "alhendinfc"

    fun allows(
        objectName: String,
        role: String?,
        workspaces: List<String>?,
        bucketId: String = BUCKET,
        issuer: String? = ISSUER,
        audience: String? = AUDIENCE
    ): Boolean {
        if (role != "authenticated") return false
        if (bucketId != BUCKET) return false
        if (issuer != ISSUER || audience != AUDIENCE) return false
        val parts = objectName.split('/')
        if (parts.size < 4) return false
        if (parts[0] != "workspaces") return false
        if (parts[2] != "attachments") return false
        val workspace = parts[1]
        return !workspace.isBlank() && workspaces.orEmpty().contains(workspace)
    }
}
