package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.local.TaskEntity
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.TaskRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val dao: TaskDao
) : TaskRepository {

    override fun getByTeam(teamId: Int): Flow<List<Task>> =
        dao.getByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun searchByName(teamId: Int, query: String): Flow<List<Task>> {
        val needle = query.trim()
        if (needle.isEmpty()) return getByTeam(teamId)
        return dao.searchByName(teamId, needle).map { list -> list.map { it.toDomain() } }
    }

    override fun getById(id: Int): Flow<Task?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun getOnce(id: Int): Task? =
        dao.getByIdOnce(id)?.toDomain()

    override suspend fun add(task: Task): Int {
        val error = TaskRules.validate(task.name, task.playerCount, task.durationMinutes)
        require(error == null) { error!! }
        return dao.insert(EntityWrites.taskForInsert(task.toEntity(), EntitySync.now())).toInt()
    }

    override suspend fun update(task: Task) {
        val error = TaskRules.validate(task.name, task.playerCount, task.durationMinutes)
        require(error == null) { error!! }
        val existing = dao.getByIdOnce(task.id) ?: return
        dao.update(EntityWrites.taskForUpdate(existing, task.toEntity(), EntitySync.now()))
    }

    override suspend fun setBoardSyncId(taskId: Int, boardSyncId: String?) {
        val existing = dao.getByIdOnce(taskId) ?: return
        dao.update(
            EntityWrites.taskForUpdate(existing, existing.copy(boardSyncId = boardSyncId), EntitySync.now())
        )
    }

    override suspend fun delete(task: Task) {
        dao.markDeleted(task.id, EntitySync.now())
    }

    private fun TaskEntity.toDomain() = Task(
        id = id,
        teamId = teamId,
        name = name,
        objective = objective,
        playerCount = playerCount,
        durationMinutes = durationMinutes,
        description = description,
        boardSyncId = boardSyncId,
        syncId = syncId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        syncId = syncId,
        teamId = teamId,
        name = name.trim(),
        objective = objective.trim(),
        playerCount = playerCount,
        durationMinutes = durationMinutes,
        description = description.trim(),
        boardSyncId = boardSyncId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
