package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.BoardDao
import com.luis.alhendinfc.data.local.BoardEntity
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.data.sync.SyncEntityType
import com.luis.alhendinfc.data.sync.SyncHooks
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardRules
import com.luis.alhendinfc.domain.model.BoardScene
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BoardRepository(
    private val boardDao: BoardDao,
    private val taskDao: TaskDao,
    private val attachmentDao: AttachmentDao,
    private val fileStore: DiskFileStore? = null
) {
    fun getByTeam(teamId: Int): Flow<List<Board>> =
        boardDao.getByTeam(teamId).map { list -> list.map { it.toDomain() } }

    fun getById(id: Int): Flow<Board?> =
        boardDao.getById(id).map { it?.toDomain() }

    suspend fun getOnce(id: Int): Board? =
        boardDao.getByIdOnce(id)?.toDomain()

    suspend fun getBySyncId(syncId: String): Board? =
        boardDao.getBySyncIdOnce(syncId)?.toDomain()

    suspend fun add(board: Board): Int {
        val error = BoardRules.validate(board.name)
        require(error == null) { error!! }
        val sceneJson = board.sceneJson.ifBlank { BoardScene.empty().toJson() }
        val stamped = EntityWrites.boardForInsert(
            board.toEntity().copy(
                sceneVersion = BoardScene.CURRENT_VERSION,
                sceneJson = sceneJson
            ),
            EntitySync.now()
        )
        return SyncHooks.local(SyncEntityType.BOARD, stamped.syncId) {
            boardDao.insert(stamped).toInt()
        }
    }

    suspend fun update(board: Board) {
        val error = BoardRules.validate(board.name)
        require(error == null) { error!! }
        val existing = boardDao.getByIdOnce(board.id) ?: return
        SyncHooks.local(SyncEntityType.BOARD, existing.syncId) {
            boardDao.update(
                EntityWrites.boardForUpdate(
                    existing,
                    board.toEntity().copy(sceneVersion = BoardScene.CURRENT_VERSION),
                    EntitySync.now()
                )
            )
        }
    }

    suspend fun rename(id: Int, name: String) {
        val existing = getOnce(id) ?: return
        update(existing.copy(name = name.trim()))
    }

    suspend fun saveScene(id: Int, scene: BoardScene) {
        val existing = getOnce(id) ?: return
        update(
            existing.copy(
                sceneVersion = BoardScene.CURRENT_VERSION,
                sceneJson = scene.copy(version = BoardScene.CURRENT_VERSION).toJson()
            )
        )
    }

    suspend fun countTasksUsing(syncId: String): Int =
        taskDao.getByBoardSyncIdIncludingDeleted(syncId).count { it.deletedAt == null }

    /**
     * Tombstone de la pizarra y de sus attachments. Quita [Task.boardSyncId]
     * de todas las tareas que la referencian (activas o tombstoneadas).
     */
    suspend fun delete(board: Board) {
        val now = EntitySync.now()
        val existing = boardDao.getByIdIncludingDeleted(board.id) ?: return
        val linkedTasks = if (existing.syncId.isNotBlank()) {
            taskDao.getByBoardSyncIdIncludingDeleted(existing.syncId)
        } else {
            emptyList()
        }
        val items = buildList {
            add(SyncEntityType.BOARD to existing.syncId)
            linkedTasks.forEach { add(SyncEntityType.TASK to it.syncId) }
        }
        SyncHooks.localMany(items) {
            boardDao.markDeleted(board.id, now)
            if (existing.syncId.isNotBlank()) {
                attachmentDao.markDeletedByParent(AttachmentParentType.BOARD, existing.syncId, now)
                linkedTasks.forEach { task ->
                    if (task.boardSyncId != null) {
                        taskDao.update(
                            EntityWrites.taskForUpdate(task, task.copy(boardSyncId = null), now)
                        )
                    }
                }
            }
        }
    }

    /**
     * Nuevo id/syncId/nombre. Copia sceneJson y duplica attachments BOARD
     * (nuevo syncId + copia física) para no compartir identidad con la original.
     */
    suspend fun duplicate(board: Board): Int {
        val source = boardDao.getByIdOnce(board.id) ?: return 0
        val now = EntitySync.now()
        val originals = attachmentDao.getActiveByParentOnce(AttachmentParentType.BOARD, source.syncId)
        val copies = originals.map { att ->
            val newSync = UUID.randomUUID().toString()
            val path = if (fileStore != null && att.localPath.isNotBlank() && fileStore.exists(att.localPath)) {
                fileStore.copyFile(att.localPath, att.name, newSync)
            } else {
                att.localPath
            }
            Triple(att, newSync, path)
        }
        val mediaMap = copies.associate { it.first.syncId to it.second }
        val scene = BoardScene.fromJson(source.sceneJson).let { current ->
            current.copy(
                version = BoardScene.CURRENT_VERSION,
                mediaAttachmentSyncId = current.mediaAttachmentSyncId?.let { mediaMap[it] ?: it }
            )
        }
        val stamped = EntityWrites.boardForInsert(
            source.copy(
                id = 0,
                name = BoardRules.copyName(source.name),
                sceneJson = scene.toJson(),
                sceneVersion = BoardScene.CURRENT_VERSION
            ),
            now
        )
        val newId = SyncHooks.local(SyncEntityType.BOARD, stamped.syncId) {
            boardDao.insert(stamped).toInt()
        }
        val created = boardDao.getByIdOnce(newId) ?: return newId
        copies.forEach { (att, newSync, path) ->
            attachmentDao.insert(
                EntityWrites.attachmentForInsert(
                    att.copy(
                        id = 0,
                        syncId = newSync,
                        parentSyncId = created.syncId,
                        localPath = path
                    ),
                    now
                )
            )
        }
        return newId
    }

    private fun BoardEntity.toDomain() = Board(
        id = id,
        syncId = syncId,
        teamId = teamId,
        name = name,
        sceneVersion = sceneVersion,
        sceneJson = sceneJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun Board.toEntity() = BoardEntity(
        id = id,
        syncId = syncId,
        teamId = teamId,
        name = name.trim(),
        sceneVersion = sceneVersion,
        sceneJson = sceneJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
