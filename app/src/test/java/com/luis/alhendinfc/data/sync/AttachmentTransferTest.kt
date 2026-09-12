package com.luis.alhendinfc.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.luis.alhendinfc.cloud.CloudDoc
import com.luis.alhendinfc.cloud.MemoryCloudStore
import com.luis.alhendinfc.cloud.StoragePath
import com.luis.alhendinfc.cloud.storage.BinaryStorageConfig
import com.luis.alhendinfc.cloud.storage.IdTokenProvider
import com.luis.alhendinfc.cloud.storage.MemoryBinaryStorage
import com.luis.alhendinfc.cloud.storage.UnavailableBinaryStorage
import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.TransferKind
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchReports
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AttachmentTransferTest {

    private lateinit var db: AlhendinDatabase
    private lateinit var store: MemoryCloudStore
    private lateinit var blobs: MemoryBinaryStorage
    private lateinit var files: DiskFileStore
    private lateinit var transfer: TransferEngine
    private lateinit var engine: SyncEngine
    private lateinit var repo: AttachmentRepositoryImpl
    private var online = true
    private var token: String? = "test-firebase-token"
    private var tokenCalls = 0

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AlhendinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SyncHooks.gate = SyncWriteGate(db)
        store = MemoryCloudStore()
        blobs = MemoryBinaryStorage()
        files = DiskFileStore(Files.createTempDirectory("att-test").toFile())
        val tokens = IdTokenProvider {
            tokenCalls += 1
            token
        }
        transfer = TransferEngine(
            db = db,
            blobs = blobs,
            files = files,
            workspaceId = "alhendin-dev",
            isOnline = { online },
            tokenProvider = tokens
        )
        TransferHooks.engine = transfer
        engine = SyncEngine(db, store, SyncRegistry(db), isOnline = { online })
        repo = AttachmentRepositoryImpl(db.attachmentDao())
    }

    @After
    fun tearDown() {
        TransferHooks.engine = null
        SyncHooks.gate = null
        db.close()
    }

    @Test
    fun uploadSuccess_setsRemotePath_enqueuesMetadata_noDoubleUpload() = runTest {
        val path = files.importStream("hola".byteInputStream(), "foto.jpg", "att-sync-1")
        repo.add(AttachmentParentType.TASK, "task-1", "image/jpeg", "foto.jpg", path, "att-sync-1")
        assertEquals(1, db.transferJobDao().count())
        assertNull(db.syncOutboxDao().find(SyncEntityType.ATTACHMENT, "att-sync-1"))
        transfer.processAll()
        val stored = db.attachmentDao().getBySyncIdIncludingDeleted("att-sync-1")!!
        assertEquals(
            StoragePath.blobPath("alhendin-dev", "att-sync-1", "foto.jpg"),
            stored.remotePath
        )
        assertTrue(StoragePath.isPortableObjectPath(stored.remotePath!!))
        assertEquals(1, blobs.blobCount())
        assertTrue(tokenCalls > 0)
        assertNotNull(db.syncOutboxDao().find(SyncEntityType.ATTACHMENT, "att-sync-1"))
        engine.sync()
        val doc = store.get(SyncEntityType.ATTACHMENT, "att-sync-1")!!
        assertFalse(doc.data.containsKey("localPath"))
        assertEquals(stored.remotePath, doc.data["remotePath"])
        val uploads = blobs.uploadedPaths
        transfer.reconcile()
        transfer.processAll()
        assertEquals(uploads, blobs.uploadedPaths)
        assertEquals(0, db.transferJobDao().count())
    }

    @Test
    fun uploadOffline_staysPending_thenUploadsOnReconnect() = runTest {
        online = false
        val path = files.importStream("pdf".byteInputStream(), "a.pdf", "att-off-1")
        repo.add(AttachmentParentType.TRAINING, "tr-1", "application/pdf", "a.pdf", path, "att-off-1")
        transfer.processAll()
        assertEquals(1, db.transferJobDao().count())
        assertNull(store.get(SyncEntityType.ATTACHMENT, "att-off-1"))
        assertTrue(files.exists(path))
        online = true
        transfer.processAll()
        engine.sync()
        assertEquals(0, db.transferJobDao().count())
        assertNotNull(store.get(SyncEntityType.ATTACHMENT, "att-off-1"))
        assertEquals(1, blobs.blobCount())
    }

    @Test
    fun uploadFail_doesNotPublishFirestore() = runTest {
        blobs.failBlobs = true
        val path = files.importStream("x".byteInputStream(), "a.jpg", "att-fail-1")
        repo.add(AttachmentParentType.OPPONENT, "club-1", "image/jpeg", "a.jpg", path, "att-fail-1")
        transfer.processAll()
        assertNull(db.attachmentDao().getBySyncIdIncludingDeleted("att-fail-1")!!.remotePath)
        engine.sync()
        assertNull(store.get(SyncEntityType.ATTACHMENT, "att-fail-1"))
        blobs.failBlobs = false
        transfer.processAll()
        engine.sync()
        assertNotNull(store.get(SyncEntityType.ATTACHMENT, "att-fail-1"))
    }

    @Test
    fun download_setsOwnLocalPath_doesNotEnqueueUpload() = runTest {
        val remote = StoragePath.blobPath("alhendin-dev", "att-dl-1", "doc.pdf")
        val src = files.importStream("remoto".byteInputStream(), "doc.pdf", "seed-dl")
        blobs.upload(remote, java.io.File(src), "application/pdf")
        engine.applyRemoteCollection(
            SyncEntityType.ATTACHMENT,
            listOf(
                CloudDoc(
                    "att-dl-1",
                    mapOf(
                        "syncId" to "att-dl-1",
                        "parentType" to AttachmentParentType.MATCH,
                        "parentSyncId" to "match-1",
                        "mime" to "application/pdf",
                        "name" to "doc.pdf",
                        "remotePath" to remote,
                        "createdAt" to 10L,
                        "updatedAt" to 10L,
                        "deletedAt" to null
                    )
                )
            )
        )
        val beforeOutbox = db.syncOutboxDao().count()
        val pending = db.attachmentDao().getBySyncIdIncludingDeleted("att-dl-1")!!
        assertNull(pending.localPath)
        transfer.processAll()
        val after = db.attachmentDao().getBySyncIdIncludingDeleted("att-dl-1")!!
        assertNotNull(after.localPath)
        assertNotEquals(src, after.localPath)
        assertEquals("remoto", java.io.File(after.localPath!!).readText())
        assertEquals(beforeOutbox, db.syncOutboxDao().count())
        val uploads = blobs.uploadedPaths
        transfer.reconcile()
        transfer.processAll()
        assertEquals(uploads, blobs.uploadedPaths)
    }

    @Test
    fun delete_tombstonesImmediately_blobCleanupRetries_noResurrection() = runTest {
        val path = files.importStream("bin".byteInputStream(), "x.pdf", "att-del-1")
        repo.add(AttachmentParentType.BOARD, "board-1", "application/pdf", "x.pdf", path, "att-del-1")
        transfer.processAll()
        engine.sync()
        assertEquals(1, blobs.blobCount())
        val live = repo.getActiveByParent(AttachmentParentType.BOARD, "board-1")
        val att = live.first().single()
        blobs.failBlobs = true
        repo.delete(att)
        assertTrue(db.attachmentDao().getBySyncIdIncludingDeleted("att-del-1")!!.deletedAt != null)
        assertTrue(live.first().isEmpty())
        engine.sync()
        assertNotNull(store.get(SyncEntityType.ATTACHMENT, "att-del-1")!!.deletedAt)
        assertEquals(1, blobs.blobCount())
        blobs.failBlobs = false
        transfer.processAll()
        assertEquals(0, blobs.blobCount())
        engine.applyRemoteCollection(
            SyncEntityType.ATTACHMENT,
            listOf(
                CloudDoc(
                    "att-del-1",
                    mapOf(
                        "syncId" to "att-del-1",
                        "parentType" to AttachmentParentType.BOARD,
                        "parentSyncId" to "board-1",
                        "mime" to "application/pdf",
                        "name" to "x.pdf",
                        "remotePath" to db.attachmentDao().getBySyncIdIncludingDeleted("att-del-1")!!.remotePath,
                        "createdAt" to 1L,
                        "updatedAt" to 1L,
                        "deletedAt" to null
                    )
                )
            )
        )
        assertNotNull(db.attachmentDao().getBySyncIdIncludingDeleted("att-del-1")!!.deletedAt)
    }

    @Test
    fun parents_keepParentSyncId_andMatchReportIsSingleAttachment() = runTest {
        val t = files.importStream("t".byteInputStream(), "t.jpg", "att-task")
        val tr = files.importStream("tr".byteInputStream(), "tr.pdf", "att-tr")
        val op = files.importStream("op".byteInputStream(), "op.pdf", "att-op")
        val m = files.importStream("m".byteInputStream(), "m.pdf", "att-m")
        val b = files.importStream("b".byteInputStream(), "b.jpg", "att-b")
        repo.add(AttachmentParentType.TASK, "task-a", "image/jpeg", "t.jpg", t, "att-task")
        repo.add(AttachmentParentType.TRAINING, "tr-a", "application/pdf", "tr.pdf", tr, "att-tr")
        repo.add(AttachmentParentType.OPPONENT, "club-a", "application/pdf", "op.pdf", op, "att-op")
        repo.add(AttachmentParentType.MATCH, "match-a", "application/pdf", "m.pdf", m, "att-m")
        repo.add(AttachmentParentType.BOARD, "board-a", "image/jpeg", "b.jpg", b, "att-b")
        transfer.processAll()
        engine.sync()
        assertEquals("task-a", store.get(SyncEntityType.ATTACHMENT, "att-task")!!.data["parentSyncId"])
        assertEquals("tr-a", store.get(SyncEntityType.ATTACHMENT, "att-tr")!!.data["parentSyncId"])
        assertEquals("club-a", store.get(SyncEntityType.ATTACHMENT, "att-op")!!.data["parentSyncId"])
        assertEquals("match-a", store.get(SyncEntityType.ATTACHMENT, "att-m")!!.data["parentSyncId"])
        assertEquals("board-a", store.get(SyncEntityType.ATTACHMENT, "att-b")!!.data["parentSyncId"])
        val match = Match(id = 1, teamId = 1, opponentClubId = 9, rival = "Rival", syncId = "match-a")
        val attachments = listOf(
            Attachment(
                syncId = "att-m",
                parentType = AttachmentParentType.MATCH,
                parentSyncId = "match-a",
                mimeType = "application/pdf",
                name = "m.pdf",
                localPath = m
            )
        )
        assertEquals(1, MatchReports.activeForMatch(attachments, "match-a").size)
        assertEquals(1, MatchReports.forOpponent(listOf(match), attachments, 9).size)
        assertEquals(0, MatchReports.forOpponent(listOf(match), attachments, 8).size)
    }

    @Test
    fun withoutFirebaseToken_doesNotTransfer_jobStaysPending() = runTest {
        token = null
        val path = files.importStream("x".byteInputStream(), "a.jpg", "att-notoken")
        repo.add(AttachmentParentType.TASK, "task-nt", "image/jpeg", "a.jpg", path, "att-notoken")
        transfer.processAll()
        assertEquals(1, db.transferJobDao().count())
        assertEquals(0, blobs.blobCount())
        assertNull(db.attachmentDao().getBySyncIdIncludingDeleted("att-notoken")!!.remotePath)
        val job = db.transferJobDao().getAll().single()
        assertEquals(BinaryStorageConfig.ERROR_NO_TOKEN, job.lastError)
        engine.sync()
        assertNull(store.get(SyncEntityType.ATTACHMENT, "att-notoken"))
    }

    @Test
    fun storageNotConfigured_keepsPending_doesNotCrash() = runTest {
        val tokens = IdTokenProvider { "test-firebase-token" }
        transfer = TransferEngine(
            db = db,
            blobs = UnavailableBinaryStorage(),
            files = files,
            workspaceId = "alhendin-dev",
            isOnline = { true },
            tokenProvider = tokens
        )
        TransferHooks.engine = transfer
        val path = files.importStream("x".byteInputStream(), "a.jpg", "att-noconfig")
        repo.add(AttachmentParentType.TASK, "task-nc", "image/jpeg", "a.jpg", path, "att-noconfig")
        transfer.processAll()
        assertEquals(1, db.transferJobDao().count())
        assertEquals(BinaryStorageConfig.ERROR_NOT_CONFIGURED, db.transferJobDao().getAll().single().lastError)
        assertNull(db.attachmentDao().getBySyncIdIncludingDeleted("att-noconfig")!!.remotePath)
    }

    @Test
    fun registryResolvesAttachmentsPlural_notSingular() {
        val registry = SyncRegistry(db)
        assertEquals("attachments", SyncEntityType.ATTACHMENT)
        assertNotNull(registry.adapter(SyncEntityType.ATTACHMENT))
        assertNull(registry.adapter("attachment"))
    }

    @Test
    fun attachmentWithRemotePath_syncPushesMetadataAndClearsOutbox() = runTest {
        val path = files.importStream("hola".byteInputStream(), "Mijas.jpg", "att-meta-1")
        repo.add(AttachmentParentType.TASK, "task-1", "image/jpeg", "Mijas.jpg", path, "att-meta-1")
        val remote = StoragePath.blobPath("alhendin-dev", "att-meta-1", "Mijas.jpg")
        db.attachmentDao().setRemotePath("att-meta-1", remote)
        SyncHooks.enqueue(SyncEntityType.ATTACHMENT, "att-meta-1")
        val pending = db.syncOutboxDao().find(SyncEntityType.ATTACHMENT, "att-meta-1")!!
        assertEquals("attachments", pending.entityType)
        assertEquals(0, pending.attempts)
        engine.sync()
        val doc = store.get(SyncEntityType.ATTACHMENT, "att-meta-1")!!
        assertEquals(remote, doc.data["remotePath"])
        assertFalse(doc.data.containsKey("localPath"))
        assertNull(db.syncOutboxDao().find(SyncEntityType.ATTACHMENT, "att-meta-1"))
    }

    @Test
    fun attachmentWithoutRemotePath_doesNotPublishMetadata() = runTest {
        val path = files.importStream("hola".byteInputStream(), "Mijas.jpg", "att-ghost-1")
        repo.add(AttachmentParentType.TASK, "task-1", "image/jpeg", "Mijas.jpg", path, "att-ghost-1")
        SyncHooks.enqueue(SyncEntityType.ATTACHMENT, "att-ghost-1")
        engine.sync()
        assertNull(store.get(SyncEntityType.ATTACHMENT, "att-ghost-1"))
        val pending = db.syncOutboxDao().find(SyncEntityType.ATTACHMENT, "att-ghost-1")!!
        assertEquals(0, pending.attempts)
        assertNull(pending.lastError)
    }

    @Test
    fun listAttachmentsDenied_stillPushesOutboxMetadata() = runTest {
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        store.failListTypes = setOf(SyncEntityType.ATTACHMENT)
        val path = files.importStream("hola".byteInputStream(), "Mijas.jpg", "att-list-fail")
        repo.add(AttachmentParentType.TASK, "task-1", "image/jpeg", "Mijas.jpg", path, "att-list-fail")
        val remote = StoragePath.blobPath("alhendin-dev", "att-list-fail", "Mijas.jpg")
        db.attachmentDao().setRemotePath("att-list-fail", remote)
        SyncHooks.enqueue(SyncEntityType.ATTACHMENT, "att-list-fail")
        engine.sync()
        val doc = store.get(SyncEntityType.ATTACHMENT, "att-list-fail")!!
        assertEquals(remote, doc.data["remotePath"])
        assertEquals(0, db.syncOutboxDao().count())
    }

    @Test
    fun remoteAttachment_appliesRoom_enqueuesDownload_notOutbox() = runTest {
        val remote = StoragePath.blobPath("alhendin-dev", "att-remote-1", "Mijas.jpg")
        val beforeOutbox = db.syncOutboxDao().count()
        engine.applyRemoteCollection(
            SyncEntityType.ATTACHMENT,
            listOf(
                CloudDoc(
                    "att-remote-1",
                    mapOf(
                        "syncId" to "att-remote-1",
                        "parentType" to AttachmentParentType.TASK,
                        "parentSyncId" to "task-remote",
                        "mime" to "image/jpeg",
                        "name" to "Mijas.jpg",
                        "remotePath" to remote,
                        "createdAt" to 10L,
                        "updatedAt" to 10L,
                        "deletedAt" to null
                    )
                )
            )
        )
        val stored = db.attachmentDao().getBySyncIdIncludingDeleted("att-remote-1")!!
        assertNull(stored.localPath)
        assertEquals(remote, stored.remotePath)
        val downloads = db.transferJobDao().getAll().filter { it.kind == TransferKind.DOWNLOAD }
        assertEquals(1, downloads.size)
        assertEquals("att-remote-1", downloads.single().attachmentSyncId)
        assertEquals(beforeOutbox, db.syncOutboxDao().count())
    }
}
