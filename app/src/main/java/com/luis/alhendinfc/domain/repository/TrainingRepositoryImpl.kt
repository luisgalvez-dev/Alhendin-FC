package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.local.TrainingDao
import com.luis.alhendinfc.data.local.TrainingEntity
import com.luis.alhendinfc.data.local.TrainingTaskDao
import com.luis.alhendinfc.data.local.TrainingTaskEntity
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.CalendarDate
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Training
import com.luis.alhendinfc.domain.model.TrainingRules
import com.luis.alhendinfc.domain.model.TrainingTask
import com.luis.alhendinfc.domain.model.TrainingTaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TrainingRepositoryImpl(
    private val trainingDao: TrainingDao,
    private val trainingTaskDao: TrainingTaskDao,
    private val taskDao: TaskDao,
    private val matchDao: MatchDao,
    private val fixtureDao: SeasonFixtureDao,
    private val attachmentDao: AttachmentDao
) : TrainingRepository {

    override fun getByTeam(teamId: Int): Flow<List<Training>> =
        trainingDao.getByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getById(id: Int): Flow<Training?> =
        trainingDao.getById(id).map { it?.toDomain() }

    override fun getTasks(trainingId: Int): Flow<List<TrainingTaskItem>> =
        combine(
            trainingTaskDao.getActiveByTraining(trainingId),
            taskDao.observeAllIncludingDeleted()
        ) { rows, tasks ->
            val byId = tasks.associateBy { it.id }
            rows.mapNotNull { row ->
                val task = byId[row.taskId] ?: return@mapNotNull null
                TrainingTaskItem(row.toDomain(), task.toDomain())
            }
        }

    override suspend fun canCreate(teamId: Int, epochDay: Long): Boolean {
        val hasTraining = trainingDao.getActiveByTeamAndDay(teamId, epochDay).isNotEmpty()
        val matchDays = matchDao.getMatchesByTeamOnce(teamId).mapNotNull { match ->
            match.dateEpochDay ?: CalendarDate.toEpochDay(match.date)
        }.toSet()
        val fixtureDays = fixtureDao.getAllOnce()
            .filter { it.teamId == teamId && it.deletedAt == null }
            .mapNotNull { fixture ->
                fixture.dateEpochDay ?: CalendarDate.toEpochDay(fixture.date)
            }
            .toSet()
        return TrainingRules.canCreateTraining(epochDay, hasTraining, matchDays, fixtureDays)
    }

    override suspend fun add(training: Training): Int {
        val epoch = CalendarDate.toEpochDay(training.date)
            ?: throw IllegalArgumentException("La fecha del entrenamiento no es válida")
        require(canCreate(training.teamId, epoch)) {
            "No se puede crear un entrenamiento: ese día ya tiene partido, jornada o entrenamiento"
        }
        return trainingDao.insert(
            EntityWrites.trainingForInsert(training.toEntity().copy(dateEpochDay = epoch), EntitySync.now())
        ).toInt()
    }

    override suspend fun update(training: Training) {
        val existing = trainingDao.getByIdOnce(training.id) ?: return
        trainingDao.update(EntityWrites.trainingForUpdate(existing, training.toEntity(), EntitySync.now()))
    }

    override suspend fun delete(training: Training) {
        val now = EntitySync.now()
        val existing = trainingDao.getByIdIncludingDeleted(training.id) ?: return
        trainingDao.markDeleted(training.id, now)
        trainingTaskDao.markDeletedByTraining(training.id, now)
        if (existing.syncId.isNotBlank()) {
            attachmentDao.markDeletedByParent(AttachmentParentType.TRAINING, existing.syncId, now)
        }
    }

    override suspend fun addTask(trainingId: Int, taskId: Int) {
        val training = trainingDao.getByIdOnce(trainingId) ?: return
        val now = EntitySync.now()
        val existing = trainingTaskDao.getByTrainingAndTaskIncludingDeleted(trainingId, taskId)
        val nextOrder = (trainingTaskDao.getActiveByTrainingOnce(trainingId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        if (existing != null) {
            if (existing.deletedAt == null) return
            trainingTaskDao.update(EntityWrites.trainingTaskForRevive(existing, nextOrder, now))
            return
        }
        trainingTaskDao.insert(
            EntityWrites.trainingTaskForInsert(
                TrainingTaskEntity(trainingId = training.id, taskId = taskId, sortOrder = nextOrder),
                now
            )
        )
    }

    override suspend fun removeTask(trainingId: Int, taskId: Int) {
        val existing = trainingTaskDao.getByTrainingAndTaskIncludingDeleted(trainingId, taskId) ?: return
        if (existing.deletedAt != null) return
        trainingTaskDao.markDeleted(existing.id, EntitySync.now())
    }

    override suspend fun moveTask(trainingId: Int, taskId: Int, up: Boolean) {
        val rows = trainingTaskDao.getActiveByTrainingOnce(trainingId)
        val index = rows.indexOfFirst { it.taskId == taskId }
        if (index < 0) return
        val swapWith = if (up) index - 1 else index + 1
        if (swapWith !in rows.indices) return
        val a = rows[index]
        val b = rows[swapWith]
        val now = EntitySync.now()
        trainingTaskDao.update(a.copy(sortOrder = b.sortOrder, updatedAt = now))
        trainingTaskDao.update(b.copy(sortOrder = a.sortOrder, updatedAt = now))
    }

    private fun TrainingEntity.toDomain() = Training(
        id = id,
        teamId = teamId,
        date = date,
        dateEpochDay = dateEpochDay,
        opponentClubId = opponentClubId,
        notes = notes,
        syncId = syncId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun Training.toEntity() = TrainingEntity(
        id = id,
        syncId = syncId,
        teamId = teamId,
        date = date,
        dateEpochDay = dateEpochDay,
        opponentClubId = opponentClubId,
        notes = notes.trim(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun TrainingTaskEntity.toDomain() = TrainingTask(
        id = id,
        trainingId = trainingId,
        taskId = taskId,
        sortOrder = sortOrder,
        syncId = syncId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun com.luis.alhendinfc.data.local.TaskEntity.toDomain() = Task(
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
}
