package com.luis.alhendinfc.data.files

import com.luis.alhendinfc.data.backup.remapRestoredAttachments
import com.luis.alhendinfc.data.local.AttachmentEntity
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskFileStoreTest {

    @Test
    fun importStream_copiesToInternalPathNotContentUri() {
        val root = createTempDirectory("att-root").toFile()
        val store = DiskFileStore(root)
        val path = store.importStream("hola imagen".byteInputStream(), "foto de tarea.jpg", "sync-img-1")
        assertFalse(path.startsWith("content://"))
        assertTrue(File(path).isFile)
        assertEquals("hola imagen", File(path).readText())
        assertTrue(path.contains("sync-img-1"))
        assertTrue(store.exists(path))
    }

    @Test
    fun remapRestoredAttachments_copiesToNewPathAndLeavesSource() {
        val extract = createTempDirectory("zip-extract").toFile()
        val packed = File(extract, "attachments/att-sync-1")
        packed.parentFile?.mkdirs()
        packed.writeText("pdf-bytes")
        val destRoot = createTempDirectory("app-files").toFile()
        val store = DiskFileStore(destRoot)
        val original = AttachmentEntity(
            id = 4,
            syncId = "att-sync-1",
            parentType = "TRAINING",
            parentSyncId = "tr-1",
            mimeType = "application/pdf",
            name = "sesion.pdf",
            localPath = "attachments/att-sync-1",
            createdAt = 1,
            updatedAt = 1
        )
        val remapped = remapRestoredAttachments(extract, listOf(original), store)
        assertTrue(packed.isFile)
        assertEquals("pdf-bytes", packed.readText())
        assertTrue(File(remapped[0].localPath).isFile)
        assertEquals("pdf-bytes", File(remapped[0].localPath).readText())
        assertEquals(4, remapped[0].id)
        assertEquals("att-sync-1", remapped[0].syncId)
        assertFalse(remapped[0].localPath.startsWith("content://"))
    }
}
