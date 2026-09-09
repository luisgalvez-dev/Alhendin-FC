package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.BoardDao
import com.luis.alhendinfc.data.local.BoardEntity
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardRepositoryTest {

    private fun harness(): Triple<BoardRepository, InMemoryBoardDao, InMemoryTrainingTaskTaskDao> {
        val boardDao = InMemoryBoardDao()
        val taskDao = InMemoryTrainingTaskTaskDao()
        val attachments = InMemoryAttachmentDao()
        return Triple(BoardRepository(boardDao, taskDao, attachments), boardDao, taskDao)
    }

    @Test
    fun createEditSaveScene_preservesIdentityAndNormalizedJson() = runTest {
        val (repo, dao, _) = harness()
        val id = repo.add(Board(teamId = 1, name = "Salida de balón 3+2"))
        val created = repo.getOnce(id)!!
        assertTrue(created.syncId.isNotBlank())
        assertEquals(created.createdAt, created.updatedAt)
        repo.rename(id, "  Presión  ")
        val renamed = repo.getOnce(id)!!
        assertEquals(created.syncId, renamed.syncId)
        assertEquals("Presión", renamed.name)
        val scene = BoardScene(
            objects = listOf(
                BoardObject("obj-1", BoardObjectType.BLUE_PLAYER, 0.2f, 0.8f, number = "4")
            )
        )
        repo.saveScene(id, scene)
        val saved = repo.getOnce(id)!!
        val restored = BoardScene.fromJson(saved.sceneJson)
        assertEquals("obj-1", restored.objects[0].objectId)
        assertEquals(0.2f, restored.objects[0].x, 0.0001f)
        assertEquals(1, dao.getAllOnce().size)
    }

    @Test
    fun duplicate_createsNewIdentityAndIndependentScene() = runTest {
        val (repo, dao, _) = harness()
        val id = repo.add(
            Board(
                teamId = 1,
                name = "Córner ofensivo",
                sceneJson = BoardScene(
                    objects = listOf(BoardObject("orig", BoardObjectType.BALL, 0.5f, 0.5f))
                ).toJson()
            )
        )
        val original = repo.getOnce(id)!!
        val copyId = repo.duplicate(original)
        val copy = repo.getOnce(copyId)!!
        assertNotEquals(original.id, copy.id)
        assertNotEquals(original.syncId, copy.syncId)
        assertEquals("Copia de Córner ofensivo", copy.name)
        repo.saveScene(
            copyId,
            BoardScene(objects = listOf(BoardObject("copy", BoardObjectType.CONE, 0.1f, 0.1f)))
        )
        val originalAfter = BoardScene.fromJson(repo.getOnce(id)!!.sceneJson)
        val copyAfter = BoardScene.fromJson(repo.getOnce(copyId)!!.sceneJson)
        assertEquals("orig", originalAfter.objects[0].objectId)
        assertEquals("copy", copyAfter.objects[0].objectId)
        assertEquals(2, dao.getAllOnce().size)
    }

    @Test
    fun delete_tombstonesAndClearsTaskBoardSyncIdIncludingDeletedTasks() = runTest {
        val (repo, dao, taskDao) = harness()
        val id = repo.add(Board(teamId = 1, name = "Presión tras pérdida"))
        val board = repo.getOnce(id)!!
        val taskRepo = TaskRepositoryImpl(taskDao)
        val activeId = taskRepo.add(Task(teamId = 1, name = "Activa", boardSyncId = board.syncId))
        val tombId = taskRepo.add(Task(teamId = 1, name = "Tomb", boardSyncId = board.syncId))
        taskRepo.delete(taskRepo.getOnce(tombId)!!)
        assertEquals(1, repo.countTasksUsing(board.syncId))
        repo.delete(board)
        assertNull(repo.getOnce(id))
        val tombstone = dao.getByIdIncludingDeleted(id)!!
        assertNotNull(tombstone.deletedAt)
        assertNull(taskDao.getByIdOnce(activeId)!!.boardSyncId)
        assertNull(taskDao.getByIdIncludingDeleted(tombId)!!.boardSyncId)
    }

    @Test
    fun isolation_byTeam() = runTest {
        val (repo, _, _) = harness()
        repo.add(Board(teamId = 1, name = "A"))
        repo.add(Board(teamId = 2, name = "B"))
        assertEquals(listOf("A"), repo.getByTeam(1).first().map { it.name })
        assertEquals(listOf("B"), repo.getByTeam(2).first().map { it.name })
    }
}

internal class InMemoryBoardDao : BoardDao {
    private val rows = mutableListOf<BoardEntity>()
    private val state = MutableStateFlow<List<BoardEntity>>(emptyList())
    private var seq = 1

    private fun publish() {
        state.value = rows.toList()
    }

    override fun getByTeam(teamId: Int): Flow<List<BoardEntity>> =
        state.map { list ->
            list.filter { it.teamId == teamId && it.deletedAt == null }
                .sortedWith(compareBy<BoardEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
        }

    override fun getById(id: Int): Flow<BoardEntity?> =
        state.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): BoardEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): BoardEntity? =
        rows.firstOrNull { it.id == id }

    override suspend fun getBySyncIdOnce(syncId: String): BoardEntity? =
        rows.firstOrNull { it.syncId == syncId && it.deletedAt == null }

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): BoardEntity? =
        rows.firstOrNull { it.syncId == syncId }

    override suspend fun getAllOnce(): List<BoardEntity> = rows.sortedBy { it.id }

    override suspend fun insert(entity: BoardEntity): Long {
        val id = if (entity.id == 0) seq++ else entity.id
        if (id >= seq) seq = id + 1
        rows.add(entity.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<BoardEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: BoardEntity) {
        val index = rows.indexOfFirst { it.id == entity.id }
        if (index >= 0) rows[index] = entity
        publish()
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        val index = rows.indexOfFirst { it.id == id && it.deletedAt == null }
        if (index >= 0) {
            rows[index] = rows[index].copy(deletedAt = now, updatedAt = now)
            publish()
        }
    }
}
