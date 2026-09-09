package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.AttachmentEntity
import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.local.TaskEntity
import com.luis.alhendinfc.data.local.TrainingDao
import com.luis.alhendinfc.data.local.TrainingEntity
import com.luis.alhendinfc.data.local.TrainingTaskDao
import com.luis.alhendinfc.data.local.TrainingTaskEntity
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.CalendarDate
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Training
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

class TrainingRepositoryTest {

    private val day = CalendarDate.toEpochDay("08/09/2026")!!

    private fun harness(): Harness {
        val trainingDao = InMemoryTrainingDao()
        val trainingTaskDao = InMemoryTrainingTaskDao()
        val taskDao = InMemoryTrainingTaskTaskDao()
        val matchDao = InMemoryOccupancyMatchDao()
        val fixtureDao = InMemoryOccupancyFixtureDao()
        val attachmentDao = InMemoryAttachmentDao()
        val repo = TrainingRepositoryImpl(
            trainingDao, trainingTaskDao, taskDao, matchDao, fixtureDao, attachmentDao
        )
        return Harness(repo, trainingDao, trainingTaskDao, taskDao, matchDao, fixtureDao, attachmentDao)
    }

    @Test
    fun create_generatesUuidAndEqualTimestamps() = runTest {
        val h = harness()
        val id = h.repo.add(Training(teamId = 1, date = "08/09/2026", dateEpochDay = day))
        val created = h.repo.getById(id).first()!!
        assertTrue(created.syncId.isNotBlank())
        assertEquals(created.createdAt, created.updatedAt)
        assertNull(created.deletedAt)
        assertEquals(day, created.dateEpochDay)
    }

    @Test
    fun edit_preservesIdentityAndDoesNotMoveDate() = runTest {
        val h = harness()
        val id = h.repo.add(Training(teamId = 1, date = "08/09/2026", notes = "a"))
        val original = h.repo.getById(id).first()!!
        Thread.sleep(2)
        h.repo.update(original.copy(notes = "b", date = "09/09/2026", teamId = 99, syncId = "stale"))
        val edited = h.repo.getById(id).first()!!
        assertEquals(original.syncId, edited.syncId)
        assertEquals(original.id, edited.id)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals("08/09/2026", edited.date)
        assertEquals(1, edited.teamId)
        assertEquals("b", edited.notes)
        assertTrue(edited.updatedAt >= original.updatedAt)
    }

    @Test
    fun delete_tombstonesTrainingRelationsAndAttachments_notLibraryTasks() = runTest {
        val h = harness()
        val taskId = h.taskDao.insert(
            TaskEntity(teamId = 1, name = "Rondo", syncId = "task-1", createdAt = 1, updatedAt = 1)
        ).toInt()
        val id = h.repo.add(Training(teamId = 1, date = "08/09/2026"))
        h.repo.addTask(id, taskId)
        val training = h.repo.getById(id).first()!!
        h.attachmentDao.insert(
            AttachmentEntity(
                syncId = "att-1",
                parentType = AttachmentParentType.TRAINING,
                parentSyncId = training.syncId,
                mimeType = "application/pdf",
                name = "sesion.pdf",
                localPath = "/tmp/sesion.pdf",
                createdAt = 1,
                updatedAt = 1
            )
        )
        h.repo.delete(training)
        assertNull(h.repo.getById(id).first())
        assertTrue(h.repo.getByTeam(1).first().isEmpty())
        val stored = h.trainingDao.getByIdIncludingDeleted(id)!!
        assertNotNull(stored.deletedAt)
        assertTrue(h.trainingTaskDao.getAllOnce().all { it.deletedAt != null })
        assertTrue(h.attachmentDao.getAllOnce().all { it.deletedAt != null })
        assertNull(h.taskDao.getByIdOnce(taskId)?.deletedAt)
    }

    @Test
    fun normalQueriesHideTombstones() = runTest {
        val h = harness()
        val id = h.repo.add(Training(teamId = 1, date = "08/09/2026"))
        h.repo.delete(h.repo.getById(id).first()!!)
        assertTrue(h.repo.getByTeam(1).first().isEmpty())
        assertEquals(1, h.trainingDao.getAllOnce().size)
    }

    @Test
    fun onlyOneActiveTrainingPerTeamDate() = runTest {
        val h = harness()
        h.repo.add(Training(teamId = 1, date = "08/09/2026"))
        val error = runCatching {
            h.repo.add(Training(teamId = 1, date = "08/09/2026"))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals(1, h.repo.getByTeam(1).first().size)
    }

    @Test
    fun cannotCreateWhenMatchOrFixtureThatDay() = runTest {
        val h = harness()
        h.matchDao.rows += MatchEntity(id = 1, teamId = 1, date = "08/09/2026", dateEpochDay = day)
        assertTrue(
            runCatching { h.repo.add(Training(teamId = 1, date = "08/09/2026")) }
                .exceptionOrNull() is IllegalArgumentException
        )
        h.matchDao.rows.clear()
        h.fixtureDao.rows += SeasonFixtureEntity(
            id = 2, teamId = 1, matchday = 1, opponentClubId = 1, date = "08/09/2026", dateEpochDay = day
        )
        assertTrue(
            runCatching { h.repo.add(Training(teamId = 1, date = "08/09/2026")) }
                .exceptionOrNull() is IllegalArgumentException
        )
    }

    @Test
    fun trainingTasks_orderChangeNoDuplicateRemoveReviveAndHistoricalTask() = runTest {
        val h = harness()
        val t1 = h.taskDao.insert(TaskEntity(teamId = 1, name = "A", syncId = "ta", createdAt = 1, updatedAt = 1)).toInt()
        val t2 = h.taskDao.insert(TaskEntity(teamId = 1, name = "B", syncId = "tb", createdAt = 1, updatedAt = 1)).toInt()
        val id = h.repo.add(Training(teamId = 1, date = "08/09/2026"))
        h.repo.addTask(id, t1)
        h.repo.addTask(id, t2)
        h.repo.addTask(id, t1)
        var items = h.repo.getTasks(id).first()
        assertEquals(listOf("A", "B"), items.map { it.task.name })
        val firstSync = items[0].relation.syncId
        h.repo.moveTask(id, t2, up = true)
        items = h.repo.getTasks(id).first()
        assertEquals(listOf("B", "A"), items.map { it.task.name })
        h.repo.removeTask(id, t1)
        items = h.repo.getTasks(id).first()
        assertEquals(listOf("B"), items.map { it.task.name })
        val tombstone = h.trainingTaskDao.getByTrainingAndTaskIncludingDeleted(id, t1)!!
        assertNotNull(tombstone.deletedAt)
        assertEquals(firstSync, tombstone.syncId)
        h.repo.addTask(id, t1)
        items = h.repo.getTasks(id).first()
        assertEquals(2, items.size)
        val revived = items.first { it.task.id == t1 }.relation
        assertEquals(firstSync, revived.syncId)
        assertNull(revived.deletedAt)
        val task = h.taskDao.getByIdOnce(t1)!!
        h.taskDao.markDeleted(t1, 99L)
        items = h.repo.getTasks(id).first()
        assertTrue(items.any { it.task.id == t1 && it.task.deletedAt != null })
        assertEquals(task.syncId, items.first { it.task.id == t1 }.task.syncId)
    }

    private class Harness(
        val repo: TrainingRepositoryImpl,
        val trainingDao: InMemoryTrainingDao,
        val trainingTaskDao: InMemoryTrainingTaskDao,
        val taskDao: InMemoryTrainingTaskTaskDao,
        val matchDao: InMemoryOccupancyMatchDao,
        val fixtureDao: InMemoryOccupancyFixtureDao,
        val attachmentDao: InMemoryAttachmentDao
    )
}

internal class InMemoryTrainingDao : TrainingDao {
    private val rows = mutableListOf<TrainingEntity>()
    private val state = MutableStateFlow<List<TrainingEntity>>(emptyList())
    private var seq = 1
    private fun publish() { state.value = rows.toList() }

    override fun getByTeam(teamId: Int): Flow<List<TrainingEntity>> =
        state.map { list -> list.filter { it.teamId == teamId && it.deletedAt == null }.sortedByDescending { it.id } }

    override suspend fun getActiveByTeamAndDay(teamId: Int, epochDay: Long): List<TrainingEntity> =
        rows.filter { it.teamId == teamId && it.dateEpochDay == epochDay && it.deletedAt == null }

    override fun getById(id: Int): Flow<TrainingEntity?> =
        state.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): TrainingEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): TrainingEntity? =
        rows.firstOrNull { it.id == id }

    override suspend fun getAllOnce(): List<TrainingEntity> = rows.sortedBy { it.id }

    override suspend fun insert(entity: TrainingEntity): Long {
        val id = if (entity.id == 0) seq++ else entity.id
        if (id >= seq) seq = id + 1
        rows.add(entity.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<TrainingEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: TrainingEntity) {
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

internal class InMemoryTrainingTaskDao : TrainingTaskDao {
    private val rows = mutableListOf<TrainingTaskEntity>()
    private val state = MutableStateFlow<List<TrainingTaskEntity>>(emptyList())
    private var seq = 1
    private fun publish() { state.value = rows.toList() }

    override fun getActiveByTraining(trainingId: Int): Flow<List<TrainingTaskEntity>> =
        state.map { list ->
            list.filter { it.trainingId == trainingId && it.deletedAt == null }
                .sortedWith(compareBy<TrainingTaskEntity> { it.sortOrder }.thenBy { it.id })
        }

    override suspend fun getActiveByTrainingOnce(trainingId: Int): List<TrainingTaskEntity> =
        rows.filter { it.trainingId == trainingId && it.deletedAt == null }
            .sortedWith(compareBy<TrainingTaskEntity> { it.sortOrder }.thenBy { it.id })

    override suspend fun getByTrainingAndTaskIncludingDeleted(trainingId: Int, taskId: Int): TrainingTaskEntity? =
        rows.firstOrNull { it.trainingId == trainingId && it.taskId == taskId }

    override suspend fun getAllOnce(): List<TrainingTaskEntity> = rows.sortedBy { it.id }

    override suspend fun insert(entity: TrainingTaskEntity): Long {
        val id = if (entity.id == 0) seq++ else entity.id
        if (id >= seq) seq = id + 1
        rows.add(entity.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<TrainingTaskEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: TrainingTaskEntity) {
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

    override suspend fun markDeletedByTraining(trainingId: Int, now: Long) {
        rows.replaceAll { row ->
            if (row.trainingId == trainingId && row.deletedAt == null) row.copy(deletedAt = now, updatedAt = now)
            else row
        }
        publish()
    }
}

internal class InMemoryTrainingTaskTaskDao : TaskDao {
    private val rows = mutableListOf<TaskEntity>()
    private val state = MutableStateFlow<List<TaskEntity>>(emptyList())
    private var seq = 1
    private fun publish() { state.value = rows.toList() }

    override fun getByTeam(teamId: Int): Flow<List<TaskEntity>> =
        state.map { list -> list.filter { it.teamId == teamId && it.deletedAt == null } }

    override fun searchByName(teamId: Int, query: String): Flow<List<TaskEntity>> = getByTeam(teamId)

    override fun getById(id: Int): Flow<TaskEntity?> =
        state.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): TaskEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): TaskEntity? = rows.firstOrNull { it.id == id }

    override suspend fun getByIdsIncludingDeleted(ids: List<Int>): List<TaskEntity> =
        rows.filter { it.id in ids }

    override suspend fun getByBoardSyncIdIncludingDeleted(syncId: String): List<TaskEntity> =
        rows.filter { it.boardSyncId == syncId }

    override fun observeAllIncludingDeleted(): Flow<List<TaskEntity>> = state

    override suspend fun getAllOnce(): List<TaskEntity> = rows.sortedBy { it.id }

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

internal class InMemoryOccupancyMatchDao : MatchDao {
    val rows = mutableListOf<MatchEntity>()
    val matchPlayers = mutableListOf<MatchPlayerEntity>()
    private var playerSeq = 1

    override fun getMatchesByTeam(teamId: Int) = TODO("unused")
    override suspend fun getMatchesByTeamOnce(teamId: Int): List<MatchEntity> =
        rows.filter { it.teamId == teamId && it.deletedAt == null }
    override fun getMatchById(id: Int) = TODO("unused")
    override suspend fun getByIdOnce(id: Int): MatchEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }
    override suspend fun getAllMatchesOnce() = rows.toList()
    override suspend fun countActiveByTeamAndDay(teamId: Int, epochDay: Long): Int =
        rows.count { it.teamId == teamId && it.deletedAt == null && it.dateEpochDay == epochDay }
    override suspend fun getAllMatchPlayersOnce() = matchPlayers.sortedBy { it.id }
    override suspend fun insertMatches(matches: List<MatchEntity>) {
        matches.forEach { insertMatch(it) }
    }
    override suspend fun insertMatchPlayers(rows: List<MatchPlayerEntity>) {
        rows.forEach { row ->
            matchPlayers += if (row.id == 0) row.copy(id = playerSeq++) else row
        }
    }
    override suspend fun insertMatch(match: MatchEntity): Long {
        val id = if (match.id == 0) (rows.maxOfOrNull { it.id } ?: 0) + 1 else match.id
        rows.add(match.copy(id = id))
        return id.toLong()
    }
    override suspend fun updateMatch(match: MatchEntity) {
        val index = rows.indexOfFirst { it.id == match.id }
        if (index >= 0) rows[index] = match
    }
    override suspend fun markDeleted(id: Int, now: Long) = TODO("unused")
    override fun getMatchPlayersByMatch(matchId: Int) = TODO("unused")
    override suspend fun getMatchPlayerOnce(matchId: Int, playerId: Int): MatchPlayerEntity? =
        matchPlayers.firstOrNull { it.matchId == matchId && it.playerId == playerId }
    override suspend fun upsertMatchPlayerPreservingIdentity(
        matchId: Int, playerId: Int, callupStatus: String, isOnField: Boolean,
        syncId: String, createdAt: Long, updatedAt: Long, deletedAt: Long?
    ) {
        val index = matchPlayers.indexOfFirst { it.matchId == matchId && it.playerId == playerId }
        if (index >= 0) {
            val existing = matchPlayers[index]
            matchPlayers[index] = existing.copy(
                callupStatus = callupStatus,
                isOnField = isOnField,
                updatedAt = updatedAt,
                deletedAt = deletedAt
            )
        } else {
            matchPlayers += MatchPlayerEntity(
                id = playerSeq++,
                matchId = matchId,
                playerId = playerId,
                callupStatus = callupStatus,
                isOnField = isOnField,
                syncId = syncId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt
            )
        }
    }
    override suspend fun markDeletedPlayersByMatch(matchId: Int, now: Long) {
        matchPlayers.replaceAll { row ->
            if (row.matchId == matchId && row.deletedAt == null) row.copy(deletedAt = now, updatedAt = now)
            else row
        }
    }
    override suspend fun markDeletedPlayer(matchId: Int, playerId: Int, now: Long) {
        val index = matchPlayers.indexOfFirst {
            it.matchId == matchId && it.playerId == playerId && it.deletedAt == null
        }
        if (index >= 0) {
            matchPlayers[index] = matchPlayers[index].copy(deletedAt = now, updatedAt = now)
        }
    }
    override suspend fun setOnField(matchId: Int, playerId: Int, onField: Boolean) = TODO("unused")
    override suspend fun putTitularesOnField(matchId: Int) = TODO("unused")
    override suspend fun clearOnField(matchId: Int) = TODO("unused")
    override fun getFinishedCallupsByTeam(teamId: Int) = TODO("unused")
    override suspend fun reassignEventPlayerId(dupId: Int, keepId: Int, updatedAt: Long) = TODO("unused")
    override suspend fun reassignEventRelatedPlayerId(dupId: Int, keepId: Int, updatedAt: Long) = TODO("unused")
    override suspend fun getMatchPlayersByPlayer(playerId: Int): List<MatchPlayerEntity> =
        matchPlayers.filter { it.playerId == playerId }
    override suspend fun markDeletedPlayerById(id: Int, now: Long) = TODO("unused")
    override suspend fun updateMatchPlayerPlayerId(rowId: Int, keepId: Int, updatedAt: Long) = TODO("unused")
    override suspend fun markOpenToLive(matchId: Int, updatedAt: Long) = TODO("unused")
    override suspend fun markFinished(
        matchId: Int, homeScore: Int, awayScore: Int, livePeriod: Int,
        liveElapsedSeconds: Int, fieldSecondsJson: String, fieldPositionsJson: String, updatedAt: Long
    ) {
        val index = rows.indexOfFirst { it.id == matchId && it.deletedAt == null }
        if (index >= 0 && rows[index].status != "FINISHED") {
            rows[index] = rows[index].copy(
                status = "FINISHED",
                homeScore = homeScore,
                awayScore = awayScore,
                livePeriod = livePeriod,
                liveElapsedSeconds = liveElapsedSeconds,
                liveClockRunning = false,
                liveClockAnchorWallMs = 0L,
                fieldSecondsJson = fieldSecondsJson,
                fieldPositionsJson = fieldPositionsJson,
                updatedAt = updatedAt
            )
        }
    }
    override suspend fun updateFieldPositions(matchId: Int, fieldPositionsJson: String) = TODO("unused")
    override suspend fun updateLiveClock(
        matchId: Int, elapsedSeconds: Int, running: Boolean, anchorWallMs: Long,
        period: Int, fieldSecondsJson: String
    ) = TODO("unused")
    override suspend fun updateLiveScore(matchId: Int, homeScore: Int, awayScore: Int) = TODO("unused")
}

internal class InMemoryOccupancyFixtureDao : SeasonFixtureDao {
    val rows = mutableListOf<SeasonFixtureEntity>()
    override fun getByTeam(teamId: Int) = TODO("unused")
    override suspend fun getByMatchday(teamId: Int, matchday: Int): SeasonFixtureEntity? =
        rows.firstOrNull { it.teamId == teamId && it.matchday == matchday && it.deletedAt == null }
    override suspend fun getByMatchdayIncludingDeleted(teamId: Int, matchday: Int): SeasonFixtureEntity? =
        rows.firstOrNull { it.teamId == teamId && it.matchday == matchday }
    override suspend fun getByIdOnce(id: Int): SeasonFixtureEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }
    override suspend fun countByTeam(teamId: Int) = rows.count { it.teamId == teamId && it.deletedAt == null }
    override suspend fun countActiveByTeamAndDay(teamId: Int, epochDay: Long): Int =
        rows.count { it.teamId == teamId && it.deletedAt == null && it.dateEpochDay == epochDay }
    override suspend fun getAllOnce(): List<SeasonFixtureEntity> = rows.toList()
    override suspend fun insertAll(entities: List<SeasonFixtureEntity>) {
        entities.forEach { insert(it) }
    }
    override suspend fun insert(entity: SeasonFixtureEntity): Long {
        val id = if (entity.id == 0) (rows.maxOfOrNull { it.id } ?: 0) + 1 else entity.id
        rows += entity.copy(id = id)
        return id.toLong()
    }
    override suspend fun update(entity: SeasonFixtureEntity) {
        val index = rows.indexOfFirst { it.id == entity.id }
        if (index >= 0) rows[index] = entity
    }
    override suspend fun markDeleted(id: Int, now: Long) {
        val index = rows.indexOfFirst { it.id == id && it.deletedAt == null }
        if (index >= 0) rows[index] = rows[index].copy(deletedAt = now, updatedAt = now)
    }
}

internal class InMemoryAttachmentDao : AttachmentDao {
    private val rows = mutableListOf<AttachmentEntity>()
    private val state = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    private var seq = 1
    private fun publish() { state.value = rows.toList() }

    override fun getActiveByParent(parentType: String, parentSyncId: String): Flow<List<AttachmentEntity>> =
        state.map { list ->
            list.filter { it.parentType == parentType && it.parentSyncId == parentSyncId && it.deletedAt == null }
        }

    override suspend fun getActiveByParentOnce(parentType: String, parentSyncId: String): List<AttachmentEntity> =
        rows.filter { it.parentType == parentType && it.parentSyncId == parentSyncId && it.deletedAt == null }

    override fun getActiveByType(parentType: String): Flow<List<AttachmentEntity>> =
        state.map { list -> list.filter { it.parentType == parentType && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): AttachmentEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getAllOnce(): List<AttachmentEntity> = rows.sortedBy { it.id }

    override suspend fun getActiveWithFilesOnce(): List<AttachmentEntity> =
        rows.filter { it.deletedAt == null && it.localPath.isNotBlank() }

    override suspend fun insert(entity: AttachmentEntity): Long {
        val id = if (entity.id == 0) seq++ else entity.id
        if (id >= seq) seq = id + 1
        rows.add(entity.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<AttachmentEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: AttachmentEntity) {
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

    override suspend fun markDeletedByParent(parentType: String, parentSyncId: String, now: Long) {
        rows.replaceAll { row ->
            if (row.parentType == parentType && row.parentSyncId == parentSyncId && row.deletedAt == null) {
                row.copy(deletedAt = now, updatedAt = now)
            } else row
        }
        publish()
    }
}
