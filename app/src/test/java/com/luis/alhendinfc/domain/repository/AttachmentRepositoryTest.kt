package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentRepositoryTest {

    @Test
    fun addDeleteAndActiveListIgnoreTombstones() = runTest {
        val dao = InMemoryAttachmentDao()
        val repo = AttachmentRepositoryImpl(dao)
        val id = repo.add(
            AttachmentParentType.TRAINING, "tr-1", "application/pdf", "a.pdf", "/files/a.pdf"
        )
        val second = repo.add(
            AttachmentParentType.TRAINING, "tr-1", "image/jpeg", "b.jpg", "/files/b.jpg"
        )
        assertEquals(2, repo.getActiveByParent(AttachmentParentType.TRAINING, "tr-1").first().size)
        val first = repo.getActiveByParent(AttachmentParentType.TRAINING, "tr-1").first().first { it.id == id }
        repo.delete(first)
        val active = repo.getActiveByParent(AttachmentParentType.TRAINING, "tr-1").first()
        assertEquals(1, active.size)
        assertEquals(second, active[0].id)
        val stored = dao.getAllOnce().first { it.id == id }
        assertNotNull(stored.deletedAt)
        assertNull(stored.remotePath)
    }

    @Test
    fun taskImage_addChangeOnlyOneActive_clearKeepsTaskUntouched() = runTest {
        val dao = InMemoryAttachmentDao()
        val repo = AttachmentRepositoryImpl(dao)
        repo.setTaskImage("task-sync", "image/jpeg", "a.jpg", "/files/a.jpg")
        repo.setTaskImage("task-sync", "image/png", "b.png", "/files/b.png")
        val active = repo.getActiveByParent(AttachmentParentType.TASK, "task-sync").first()
        assertEquals(1, active.size)
        assertEquals("b.png", active[0].name)
        assertTrue(active[0].isImage)
        assertEquals(2, dao.getAllOnce().size)
        assertEquals(1, dao.getAllOnce().count { it.deletedAt != null })
        repo.clearTaskImages("task-sync")
        assertTrue(repo.getActiveByParent(AttachmentParentType.TASK, "task-sync").first().isEmpty())
        assertEquals(2, dao.getAllOnce().size)
        assertTrue(dao.getAllOnce().all { it.deletedAt != null })
    }

    @Test
    fun matchReports_severalTypesStayOnOneMatch_andIgnoreOtherParents() = runTest {
        val dao = InMemoryAttachmentDao()
        val repo = AttachmentRepositoryImpl(dao)
        repo.add(AttachmentParentType.MATCH, "match-a", "application/pdf", "a.pdf", "/files/a.pdf")
        repo.add(AttachmentParentType.MATCH, "match-a", "image/jpeg", "b.jpg", "/files/b.jpg")
        repo.add(AttachmentParentType.MATCH, "match-b", "text/plain", "c.txt", "/files/c.txt")
        repo.add(AttachmentParentType.OPPONENT, "club-a", "application/pdf", "rival.pdf", "/files/r.pdf")
        val forA = repo.getActiveByParent(AttachmentParentType.MATCH, "match-a").first()
        assertEquals(2, forA.size)
        assertEquals(setOf("a.pdf", "b.jpg"), forA.map { it.name }.toSet())
        val forB = repo.getActiveByParent(AttachmentParentType.MATCH, "match-b").first()
        assertEquals(1, forB.size)
        repo.delete(forA.first { it.name == "a.pdf" })
        val after = repo.getActiveByParent(AttachmentParentType.MATCH, "match-a").first()
        assertEquals(listOf("b.jpg"), after.map { it.name })
        assertEquals(1, dao.getAllOnce().count { it.deletedAt != null })
        assertEquals(1, repo.getActiveByType(AttachmentParentType.OPPONENT).first().size)
    }
}
