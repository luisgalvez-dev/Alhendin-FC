package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.SharedMedia
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

    @Test
    fun mediaSlots_oneActivePerParent_playersStayIndependent() = runTest {
        val dao = InMemoryAttachmentDao()
        val repo = AttachmentRepositoryImpl(dao)
        repo.setSlotImage(AttachmentParentType.PLAYER_PHOTO, "player-a", "image/jpeg", "a.jpg", "/files/a.jpg")
        repo.setSlotImage(AttachmentParentType.PLAYER_PHOTO, "player-b", "image/png", "b.png", "/files/b.png")
        repo.setSlotImage(AttachmentParentType.PLAYER_PHOTO, "player-a", "image/png", "a2.png", "/files/a2.png")
        val photos = repo.getActiveByType(AttachmentParentType.PLAYER_PHOTO).first()
        assertEquals(2, photos.size)
        val byParent = SharedMedia.byParentSyncId(photos)
        assertEquals("a2.png", byParent["player-a"]?.name)
        assertEquals("b.png", byParent["player-b"]?.name)
        assertEquals(1, dao.getAllOnce().count { it.parentSyncId == "player-a" && it.deletedAt != null })
    }

    @Test
    fun teamAndOpponentSlots_replaceAndClearIndependently() = runTest {
        val dao = InMemoryAttachmentDao()
        val repo = AttachmentRepositoryImpl(dao)
        repo.setSlotImage(AttachmentParentType.TEAM_SHIELD, "team-1", "image/png", "t.png", "/t.png")
        repo.setSlotImage(AttachmentParentType.OPPONENT_SHIELD, "club-a", "image/png", "a.png", "/a.png")
        repo.setSlotImage(AttachmentParentType.OPPONENT_SHIELD, "club-b", "image/png", "b.png", "/b.png")
        repo.setSlotImage(AttachmentParentType.OPPONENT_SHIELD, "club-a", "image/jpeg", "a2.jpg", "/a2.jpg")
        assertEquals(1, repo.getActiveByType(AttachmentParentType.TEAM_SHIELD).first().size)
        val rivals = SharedMedia.byParentSyncId(repo.getActiveByType(AttachmentParentType.OPPONENT_SHIELD).first())
        assertEquals("a2.jpg", rivals["club-a"]?.name)
        assertEquals("b.png", rivals["club-b"]?.name)
        repo.clearSlot(AttachmentParentType.TEAM_SHIELD, "team-1")
        assertTrue(repo.getActiveByType(AttachmentParentType.TEAM_SHIELD).first().isEmpty())
        assertEquals(2, repo.getActiveByType(AttachmentParentType.OPPONENT_SHIELD).first().size)
        repo.clearSlot(AttachmentParentType.OPPONENT_SHIELD, "club-a")
        assertEquals(listOf("b.png"), repo.getActiveByType(AttachmentParentType.OPPONENT_SHIELD).first().map { it.name })
    }

    @Test
    fun slotRejectsPdf() = runTest {
        val repo = AttachmentRepositoryImpl(InMemoryAttachmentDao())
        try {
            repo.setSlotImage(AttachmentParentType.PLAYER_PHOTO, "player-a", "application/pdf", "x.pdf", "/x.pdf")
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun slotRejectsTaskParentType() = runTest {
        val repo = AttachmentRepositoryImpl(InMemoryAttachmentDao())
        try {
            repo.setSlotImage(AttachmentParentType.TASK, "task-1", "image/jpeg", "a.jpg", "/a.jpg")
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
