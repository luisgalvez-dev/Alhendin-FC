package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.local.TaskEntity
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

class TaskRepositoryTest {

    private fun repo() = TaskRepositoryImpl(InMemoryTaskDao())

    @Test
    fun create_generatesNonBlankUuidAndEqualTimestamps() = runTest {
        val repository = repo()
        val id = repository.add(Task(teamId = 1, name = "Presión tras pérdida"))
        val created = repository.getById(id).first()!!
        assertTrue(created.syncId.isNotBlank())
        assertEquals(created.createdAt, created.updatedAt)
        assertNull(created.deletedAt)
        assertTrue(created.id > 0)
    }

    @Test
    fun edit_preservesUuidAndBumpsUpdatedAt() = runTest {
        val repository = repo()
        val id = repository.add(Task(teamId = 1, name = "Rondo"))
        val original = repository.getById(id).first()!!
        Thread.sleep(2)
        repository.update(original.copy(name = "Rondo 4x4", objective = "Posesión"))
        val edited = repository.getById(id).first()!!
        assertEquals(original.syncId, edited.syncId)
        assertEquals(original.id, edited.id)
        assertEquals(original.createdAt, edited.createdAt)
        assertTrue(edited.updatedAt >= original.updatedAt)
        assertNotEquals("Rondo", edited.name)
        assertEquals("Posesión", edited.objective)
    }

    @Test
    fun delete_createsTombstoneHiddenFromNormalQueries() = runTest {
        val dao = InMemoryTaskDao()
        val repository = TaskRepositoryImpl(dao)
        val id = repository.add(Task(teamId = 1, name = "Presión"))
        val before = repository.getById(id).first()!!
        repository.delete(before)
        assertNull(repository.getById(id).first())
        assertTrue(repository.getByTeam(1).first().isEmpty())
        val including = dao.getAllOnce()
        assertEquals(1, including.size)
        assertNotNull(including[0].deletedAt)
        assertEquals(including[0].deletedAt, including[0].updatedAt)
        assertEquals(before.syncId, including[0].syncId)
    }

    @Test
    fun search_isPartialAndCaseInsensitive() = runTest {
        val repository = repo()
        repository.add(Task(teamId = 1, name = "Presión tras pérdida"))
        repository.add(Task(teamId = 1, name = "Rondo ofensivo"))
        repository.add(Task(teamId = 2, name = "Presión de otro equipo"))
        val found = repository.searchByName(1, "pres").first()
        assertEquals(1, found.size)
        assertEquals("Presión tras pérdida", found[0].name)
        val upper = repository.searchByName(1, "PRES").first()
        assertEquals(1, upper.size)
    }

    @Test
    fun blankName_isNotSaved() = runTest {
        val repository = repo()
        val error = runCatching { repository.add(Task(teamId = 1, name = "  ")) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(repository.getByTeam(1).first().isEmpty())
    }

    @Test
    fun nonPositivePlayerCountOrDuration_areRejectedWhenSpecified() = runTest {
        val repository = repo()
        assertTrue(
            runCatching { repository.add(Task(teamId = 1, name = "A", playerCount = 0)) }
                .exceptionOrNull() is IllegalArgumentException
        )
        assertTrue(
            runCatching { repository.add(Task(teamId = 1, name = "A", durationMinutes = -3)) }
                .exceptionOrNull() is IllegalArgumentException
        )
        repository.add(Task(teamId = 1, name = "A", playerCount = null, durationMinutes = null))
        assertEquals(1, repository.getByTeam(1).first().size)
    }

    @Test
    fun setBoardSyncId_assignsAndClearsWithoutChangingIdentity() = runTest {
        val repository = repo()
        val id = repository.add(Task(teamId = 1, name = "Pizarra asociada"))
        val original = repository.getOnce(id)!!
        repository.setBoardSyncId(id, "board-sync-1")
        val linked = repository.getOnce(id)!!
        assertEquals("board-sync-1", linked.boardSyncId)
        assertEquals(original.syncId, linked.syncId)
        assertEquals(original.createdAt, linked.createdAt)
        repository.setBoardSyncId(id, null)
        assertNull(repository.getOnce(id)!!.boardSyncId)
    }
}

private class InMemoryTaskDao : TaskDao {
    private val rows = mutableListOf<TaskEntity>()
    private val state = MutableStateFlow<List<TaskEntity>>(emptyList())
    private var seq = 1

    private fun publish() {
        state.value = rows.toList()
    }

    override fun getByTeam(teamId: Int): Flow<List<TaskEntity>> =
        state.map { list ->
            list.filter { it.teamId == teamId && it.deletedAt == null }
                .sortedWith(compareBy<TaskEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
        }

    override fun searchByName(teamId: Int, query: String): Flow<List<TaskEntity>> =
        getByTeam(teamId).map { list ->
            list.filter { it.name.contains(query, ignoreCase = true) }
        }

    override fun getById(id: Int): Flow<TaskEntity?> =
        state.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): TaskEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): TaskEntity? =
        rows.firstOrNull { it.id == id }

    override suspend fun getByIdsIncludingDeleted(ids: List<Int>): List<TaskEntity> =
        if (ids.isEmpty()) emptyList() else rows.filter { it.id in ids }

    override suspend fun getByBoardSyncIdIncludingDeleted(syncId: String): List<TaskEntity> =
        rows.filter { it.boardSyncId == syncId }

    override fun observeAllIncludingDeleted(): Flow<List<TaskEntity>> = state

    override suspend fun getAllOnce(): List<TaskEntity> = rows.sortedBy { it.id }

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): TaskEntity? =
        rows.firstOrNull { it.syncId == syncId }

    override suspend fun insert(entity: TaskEntity): Long {
        val id = if (entity.id == 0) seq++ else entity.id
        if (id >= seq) seq = id + 1
        rows.add(entity.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<TaskEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: TaskEntity) {
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
