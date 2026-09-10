package com.luis.alhendinfc.dev

import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerPosition
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.repository.BoardRepository
import com.luis.alhendinfc.domain.repository.InMemoryAttachmentDao
import com.luis.alhendinfc.domain.repository.InMemoryBoardDao
import com.luis.alhendinfc.domain.repository.InMemoryMatchEventDao
import com.luis.alhendinfc.domain.repository.InMemoryOccupancyFixtureDao
import com.luis.alhendinfc.domain.repository.InMemoryOccupancyMatchDao
import com.luis.alhendinfc.domain.repository.InMemoryOpponentClubDao
import com.luis.alhendinfc.domain.repository.InMemoryOpponentPlayerDao
import com.luis.alhendinfc.domain.repository.InMemoryPlayerDao
import com.luis.alhendinfc.domain.repository.InMemoryRivalAnalysisDao
import com.luis.alhendinfc.domain.repository.InMemoryRivalLinkDao
import com.luis.alhendinfc.domain.repository.InMemoryTrainingDao
import com.luis.alhendinfc.domain.repository.InMemoryTrainingTaskDao
import com.luis.alhendinfc.domain.repository.InMemoryTrainingTaskTaskDao
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.repository.RivalRepository
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import com.luis.alhendinfc.domain.repository.TrainingRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevSeedDataTest {

    private data class Harness(
        val stores: DevSeedData.DevSeedStores,
        val trainingTaskDao: InMemoryTrainingTaskDao,
        val taskRepo: TaskRepositoryImpl,
        val trainingRepo: TrainingRepositoryImpl,
        val rivalRepo: RivalRepository,
        val playerRepo: PlayerRepositoryImpl
    )

    private fun harness(): Harness {
        val taskDao = InMemoryTrainingTaskTaskDao()
        val clubDao = InMemoryOpponentClubDao()
        val fixtureDao = InMemoryOccupancyFixtureDao()
        val matchDao = InMemoryOccupancyMatchDao()
        val trainingDao = InMemoryTrainingDao()
        val trainingTaskDao = InMemoryTrainingTaskDao()
        val analysisDao = InMemoryRivalAnalysisDao()
        val linkDao = InMemoryRivalLinkDao()
        val playerDao = InMemoryOpponentPlayerDao()
        val ownPlayerDao = InMemoryPlayerDao()
        val boardDao = InMemoryBoardDao()
        val taskRepo = TaskRepositoryImpl(taskDao)
        val calendarRepo = SeasonCalendarRepository(clubDao, fixtureDao)
        val trainingRepo = TrainingRepositoryImpl(
            trainingDao, trainingTaskDao, taskDao, matchDao, fixtureDao, InMemoryAttachmentDao()
        )
        val rivalRepo = RivalRepository(analysisDao, linkDao, playerDao)
        val boardRepo = BoardRepository(boardDao, taskDao, InMemoryAttachmentDao())
        val playerRepo = PlayerRepositoryImpl(ownPlayerDao, matchDao)
        val matchRepo = MatchRepositoryImpl(matchDao, InMemoryMatchEventDao())
        return Harness(
            DevSeedData.DevSeedStores(
                taskDao = taskDao,
                taskRepo = taskRepo,
                clubDao = clubDao,
                calendarRepo = calendarRepo,
                trainingDao = trainingDao,
                matchDao = matchDao,
                fixtureDao = fixtureDao,
                trainingRepo = trainingRepo,
                analysisDao = analysisDao,
                linkDao = linkDao,
                playerDao = playerDao,
                rivalRepo = rivalRepo,
                boardDao = boardDao,
                boardRepo = boardRepo,
                ownPlayerDao = ownPlayerDao,
                playerRepo = playerRepo,
                matchRepo = matchRepo
            ),
            trainingTaskDao,
            taskRepo,
            trainingRepo,
            rivalRepo,
            playerRepo
        )
    }

    private suspend fun seedAll(h: Harness, teamId: Int = 1, teamName: String = DevSeedMarkers.labeled(DevSeedMarkers.DEMO_TEAM)) {
        val clubs = DevSeedData.seedModuleExamples(h.stores, teamId, teamName)
        DevSeedData.seedMatchesAndFixture(h.stores, teamId, clubs)
    }

    @Test
    fun firstRun_createsDemoExamples() = runTest {
        val h = harness()
        h.taskRepo.add(Task(teamId = 1, name = "Rondo real", playerCount = 8, durationMinutes = 10))

        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val demoTasks = h.stores.taskDao.getAllOnce().filter { DevSeedMarkers.isDemo(it.name) }
        assertEquals(4, demoTasks.size)
        assertEquals(1, h.stores.taskDao.getAllOnce().count { it.name == "Rondo real" })

        val clubs = h.stores.clubDao.getAllOnce().filter { DevSeedMarkers.isDemo(it.name) }
        assertEquals(3, clubs.size)

        val trainings = h.stores.trainingDao.getAllOnce().filter { DevSeedMarkers.isDemo(it.notes) }
        assertEquals(3, trainings.size)
        val taskSets = trainings.map { training ->
            h.trainingRepo.getTasks(training.id).first().map { it.task.id }.toSet()
        }
        assertEquals(3, taskSets.distinct().size)
        assertTrue(taskSets.any { it.size == 1 })
        assertTrue(taskSets.any { it.size == 2 })

        val rivalA = clubs.first { it.name.contains("Rival A") }
        val rivalB = clubs.first { it.name.contains("Rival B") }
        val analysisA = h.stores.analysisDao.getByClubIncludingDeleted(rivalA.id)
        val analysisB = h.stores.analysisDao.getByClubIncludingDeleted(rivalB.id)
        assertTrue(analysisA != null && analysisA.deletedAt == null)
        assertTrue(analysisB != null && analysisB.deletedAt == null)
        assertEquals(2, h.stores.linkDao.getAllOnce().count { it.opponentClubId == rivalA.id })
        assertEquals(2, h.stores.linkDao.getAllOnce().count { it.opponentClubId == rivalB.id })
        assertEquals(6, h.stores.playerDao.getAllOnce().count { it.opponentClubId == rivalA.id })
        assertEquals(5, h.stores.playerDao.getAllOnce().count { it.opponentClubId == rivalB.id })

        val boards = h.stores.boardDao.getAllOnce().filter { DevSeedMarkers.isDemo(it.name) }
        assertEquals(3, boards.size)
        assertTrue(boards.any { it.name.contains("Salida de balón 3+2") })
        assertTrue(boards.any { it.name.contains("Presión tras pérdida") })
        assertTrue(boards.any { it.name.contains("Córner ofensivo") })
        boards.forEach { board ->
            assertTrue(board.sceneJson.isNotBlank())
            assertTrue(com.luis.alhendinfc.domain.model.BoardScene.fromJson(board.sceneJson).objects.isNotEmpty())
        }
        val linkedTask = h.stores.taskDao.getAllOnce().first { it.name.contains("Salida de balón 3+2") }
        val linkedBoard = boards.first { it.name.contains("Salida de balón 3+2") }
        assertEquals(linkedBoard.syncId, linkedTask.boardSyncId)

        val ownSquad = h.stores.ownPlayerDao.getAllOnce().filter { it.teamId == 1 }
        assertEquals(20, ownSquad.size)
        assertTrue(ownSquad.all { DevSeedMarkers.isDemo(it.name) })
        assertTrue(ownSquad.any { it.position == PlayerPosition.PORTERO.name && it.jerseyNumber == 1 })
        assertTrue(ownSquad.any { it.position == PlayerPosition.DELANTERO.name && it.jerseyNumber == 9 })
        assertTrue(ownSquad.map { it.jerseyNumber }.toSet().size == 20)
    }

    @Test
    fun secondRun_doesNotDuplicate() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)
        val first = snapshot(h)
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)
        assertEquals(first, snapshot(h))
    }

    @Test
    fun reseed_doesNotChangeManuallyEditedTrainingTasks() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val training = h.stores.trainingDao.getAllOnce().first { DevSeedMarkers.isDemo(it.notes) }
        val originalNotes = training.notes
        val originalRival = training.opponentClubId
        val items = h.trainingRepo.getTasks(training.id).first()
        assertTrue(items.size >= 2)
        h.trainingRepo.moveTask(training.id, items[1].task.id, up = true)
        val removed = items[0].task
        h.trainingRepo.removeTask(training.id, removed.id)

        val extraTask = h.stores.taskDao.getAllOnce()
            .first { DevSeedMarkers.isDemo(it.name) && it.id != items[0].task.id && it.id != items[1].task.id }
        h.trainingRepo.addTask(training.id, extraTask.id)

        val afterEdit = h.trainingTaskDao.getAllOnce()
            .filter { it.trainingId == training.id }
            .map { RelationSnap(it.id, it.taskId, it.sortOrder, it.deletedAt, it.syncId) }

        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val afterSeed = h.stores.trainingDao.getAllOnce().first { it.id == training.id }
        assertEquals(originalNotes, afterSeed.notes)
        assertEquals(originalRival, afterSeed.opponentClubId)
        assertEquals(
            afterEdit,
            h.trainingTaskDao.getAllOnce()
                .filter { it.trainingId == training.id }
                .map { RelationSnap(it.id, it.taskId, it.sortOrder, it.deletedAt, it.syncId) }
        )
        val activeIds = h.trainingRepo.getTasks(training.id).first().map { it.task.id }
        assertTrue(removed.id !in activeIds)
        assertTrue(extraTask.id in activeIds)
    }

    @Test
    fun reseed_doesNotOverwriteManualRivalEdits() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val rivalA = h.stores.clubDao.getAllOnce().first { it.name.contains("Rival A") }
        val analysis = h.rivalRepo.getAnalysis(rivalA.id).first()!!
        h.rivalRepo.saveAnalysis(
            analysis.copy(
                usualSystem = "1-3-5-2 editado",
                generalNotes = "Notas manuales"
            )
        )
        val links = h.rivalRepo.getLinks(rivalA.id).first()
        assertTrue(links.isNotEmpty())
        h.rivalRepo.updateLink(
            links.first().copy(label = "Enlace editado", url = "https://example.com/manual")
        )
        h.rivalRepo.deleteLink(links.last())

        val squad = h.rivalRepo.getPlayers(rivalA.id).first()
        assertTrue(squad.size >= 2)
        h.rivalRepo.updatePlayer(squad.first().copy(name = "Nombre manual"))
        h.rivalRepo.deletePlayer(squad.last())
        h.rivalRepo.addPlayer(com.luis.alhendinfc.domain.model.OpponentPlayer(opponentClubId = rivalA.id, name = "Fichaje manual"))

        val analysisAfterEdit = h.stores.analysisDao.getByClubIncludingDeleted(rivalA.id)!!
        val linksAfterEdit = h.stores.linkDao.getAllOnce()
            .filter { it.opponentClubId == rivalA.id }
            .map { LinkSnap(it.id, it.label, it.url, it.sortOrder, it.deletedAt, it.syncId) }
        val playersAfterEdit = h.stores.playerDao.getAllOnce()
            .filter { it.opponentClubId == rivalA.id }
            .map { PlayerSnap(it.id, it.name, it.deletedAt, it.syncId) }

        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val analysisAfterSeed = h.stores.analysisDao.getByClubIncludingDeleted(rivalA.id)!!
        assertEquals(analysisAfterEdit.usualSystem, analysisAfterSeed.usualSystem)
        assertEquals(analysisAfterEdit.generalNotes, analysisAfterSeed.generalNotes)
        assertEquals(analysisAfterEdit.updatedAt, analysisAfterSeed.updatedAt)
        assertEquals(analysisAfterEdit.syncId, analysisAfterSeed.syncId)
        assertEquals(
            linksAfterEdit,
            h.stores.linkDao.getAllOnce()
                .filter { it.opponentClubId == rivalA.id }
                .map { LinkSnap(it.id, it.label, it.url, it.sortOrder, it.deletedAt, it.syncId) }
        )
        assertEquals("1-3-5-2 editado", analysisAfterSeed.usualSystem)
        assertEquals("Enlace editado", h.rivalRepo.getLinks(rivalA.id).first().first().label)
        assertEquals(1, h.rivalRepo.getLinks(rivalA.id).first().size)
        assertEquals(
            playersAfterEdit,
            h.stores.playerDao.getAllOnce()
                .filter { it.opponentClubId == rivalA.id }
                .map { PlayerSnap(it.id, it.name, it.deletedAt, it.syncId) }
        )
        assertTrue(h.rivalRepo.getPlayers(rivalA.id).first().any { it.name == "Nombre manual" })
        assertTrue(h.rivalRepo.getPlayers(rivalA.id).first().any { it.name == "Fichaje manual" })
        assertTrue(h.rivalRepo.getPlayers(rivalA.id).first().none { it.name == squad.last().name })
    }

    @Test
    fun reseed_doesNotOverwriteManualBoardEdits() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val board = h.stores.boardDao.getAllOnce().first { it.name.contains("Córner ofensivo") }
        h.stores.boardRepo.rename(board.id, DevSeedMarkers.labeled("Córner ofensivo editado"))
        h.stores.boardRepo.saveScene(
            board.id,
            BoardScene(objects = listOf(BoardObject("manual", BoardObjectType.CONE, 0.3f, 0.3f)))
        )
        val task = h.stores.taskDao.getAllOnce().first { it.name.contains("Salida de balón 3+2") }
        h.stores.taskRepo.setBoardSyncId(task.id, null)
        val afterEdit = h.stores.boardDao.getAllOnce().map { BoardSnap(it.id, it.name, it.sceneJson, it.syncId, it.deletedAt) }
        val taskAfterEdit = h.stores.taskDao.getAllOnce().first { it.id == task.id }

        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        assertEquals(
            afterEdit,
            h.stores.boardDao.getAllOnce().map { BoardSnap(it.id, it.name, it.sceneJson, it.syncId, it.deletedAt) }
        )
        val renamed = h.stores.boardRepo.getOnce(board.id)!!
        assertTrue(renamed.name.contains("editado"))
        assertTrue(renamed.sceneJson.contains("manual"))
        assertNull(h.stores.taskDao.getAllOnce().first { it.id == task.id }.boardSyncId)
        assertEquals(taskAfterEdit.updatedAt, h.stores.taskDao.getAllOnce().first { it.id == task.id }.updatedAt)
        assertEquals(3, h.stores.boardDao.getAllOnce().size)
        assertEquals(3, h.stores.boardDao.getAllOnce().count { DevSeedMarkers.isDemo(it.name) })
    }

    @Test
    fun emptySquad_createsDevPlayers() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val squad = h.stores.ownPlayerDao.getAllByTeamOnce(1)
        assertEquals(20, squad.size)
        assertTrue(squad.all { DevSeedMarkers.isDemo(it.name) && it.teamId == 1 && it.deletedAt == null })
        assertEquals(2, squad.count { it.position == PlayerPosition.PORTERO.name })
        assertTrue(squad.any { it.name.contains("Álvaro Martín") && it.jerseyNumber == 1 })
        assertTrue(squad.any { it.name.contains("Sergio López") && it.jerseyNumber == 20 })
    }

    @Test
    fun emptyNonDemoTeam_createsDevPlayers() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1, teamName = "Alhendín CF")
        assertEquals(20, h.stores.ownPlayerDao.getAllByTeamOnce(1).size)
    }

    @Test
    fun realSquad_doesNotAddFictionalPlayers() = runTest {
        val h = harness()
        h.playerRepo.addPlayer(
            Player(teamId = 1, name = "Luis Real", jerseyNumber = 4, position = PlayerPosition.CENTRAL_DERECHO)
        )
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val squad = h.stores.ownPlayerDao.getAllOnce().filter { it.teamId == 1 }
        assertEquals(1, squad.size)
        assertEquals("Luis Real", squad.single().name)
        assertFalse(squad.any { DevSeedMarkers.isDemo(it.name) })
    }

    @Test
    fun editedDevPlayer_isNotRestored() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)
        val player = h.playerRepo.getPlayersByTeam(1).first().first { it.jerseyNumber == 9 }
        h.playerRepo.updatePlayer(
            player.copy(name = "[DEV] Miguel editado", jerseyNumber = 99, position = PlayerPosition.EXTREMO_DERECHO)
        )
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        val after = h.stores.ownPlayerDao.getByIdOnce(player.id)!!
        assertEquals("[DEV] Miguel editado", after.name)
        assertEquals(99, after.jerseyNumber)
        assertEquals(PlayerPosition.EXTREMO_DERECHO.name, after.position)
        assertEquals(20, h.stores.ownPlayerDao.getAllOnce().count { it.teamId == 1 })
        assertTrue(h.stores.ownPlayerDao.getAllOnce().none { it.name.contains("Miguel García") && it.jerseyNumber == 9 })
    }

    @Test
    fun tombstonedDevPlayer_doesNotReappear() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)
        val player = h.playerRepo.getPlayersByTeam(1).first().first { it.jerseyNumber == 1 }
        val originalName = player.name
        h.playerRepo.deletePlayer(player)
        DevSeedData.seedModuleExamples(h.stores, teamId = 1)

        assertNull(h.stores.ownPlayerDao.getByIdOnce(player.id))
        val tombstone = h.stores.ownPlayerDao.getAllOnce().first { it.id == player.id }
        assertTrue(tombstone.deletedAt != null)
        assertEquals(originalName, tombstone.name)
        assertEquals(19, h.stores.ownPlayerDao.getAllByTeamOnce(1).size)
        assertEquals(1, h.stores.ownPlayerDao.getAllOnce().count { it.name == originalName })
    }

    @Test
    fun ownSquad_belongsToSeededTeamId() = runTest {
        val h = harness()
        DevSeedData.seedModuleExamples(h.stores, teamId = 3)
        val squad = h.stores.ownPlayerDao.getAllOnce()
        assertTrue(squad.isNotEmpty())
        assertTrue(squad.all { it.teamId == 3 })
        assertEquals(0, h.stores.ownPlayerDao.getAllByTeamOnce(1).size)
    }

    @Test
    fun demoCallup_isCreatedOnceAndNotOverwritten() = runTest {
        val h = harness()
        seedAll(h)
        val open = h.stores.matchDao.getAllMatchesOnce().first { it.status == "OPEN" && DevSeedMarkers.isDemo(it.notes) }
        val finished = h.stores.matchDao.getAllMatchesOnce().first { it.status == "FINISHED" && DevSeedMarkers.isDemo(it.notes) }
        assertTrue(open.syncId.isNotBlank())
        assertTrue(finished.syncId.isNotBlank())
        assertNotNull(open.opponentClubId)
        assertNotNull(finished.opponentClubId)
        val openCallup = h.stores.matchDao.getAllMatchPlayersOnce().filter { it.matchId == open.id && it.deletedAt == null }
        val finishedCallup = h.stores.matchDao.getAllMatchPlayersOnce().filter { it.matchId == finished.id && it.deletedAt == null }
        assertEquals(11, openCallup.count { it.callupStatus == CallupStatus.TITULAR.name })
        assertTrue(openCallup.count { it.callupStatus == CallupStatus.SUPLENTE.name } >= 5)
        assertEquals(11, finishedCallup.count { it.callupStatus == CallupStatus.TITULAR.name })

        val titular = openCallup.first { it.callupStatus == CallupStatus.TITULAR.name }
        h.stores.matchRepo.setPlayerCallup(open.id, titular.playerId, CallupStatus.SUPLENTE)
        val afterEdit = h.stores.matchDao.getAllMatchPlayersOnce()
            .filter { it.matchId == open.id }
            .map { CallupSnap(it.id, it.playerId, it.callupStatus, it.deletedAt, it.syncId) }

        seedAll(h)

        assertEquals(
            afterEdit,
            h.stores.matchDao.getAllMatchPlayersOnce()
                .filter { it.matchId == open.id }
                .map { CallupSnap(it.id, it.playerId, it.callupStatus, it.deletedAt, it.syncId) }
        )
        val updated = h.stores.matchDao.getMatchPlayerOnce(open.id, titular.playerId)!!
        assertEquals(CallupStatus.SUPLENTE.name, updated.callupStatus)
        assertEquals(2, h.stores.matchDao.getAllMatchesOnce().count { DevSeedMarkers.isDemo(it.notes) })
    }

    @Test
    fun canSeedOwnSquad_condition() {
        val demoTeam = DevSeedMarkers.labeled(DevSeedMarkers.DEMO_TEAM)
        assertTrue(DevSeedData.canSeedOwnSquad(demoTeam, emptyList()))
        assertTrue(DevSeedData.canSeedOwnSquad("Alhendín CF", emptyList()))
        val real = PlayerEntity(id = 1, teamId = 1, name = "Luis Real")
        assertFalse(DevSeedData.canSeedOwnSquad(demoTeam, listOf(real)))
        assertFalse(DevSeedData.canSeedOwnSquad("Alhendín CF", listOf(real)))
        val demoPlayer = PlayerEntity(id = 2, teamId = 1, name = DevSeedMarkers.labeled("Álvaro Martín"))
        assertTrue(DevSeedData.canSeedOwnSquad(demoTeam, listOf(demoPlayer)))
        assertFalse(DevSeedData.canSeedOwnSquad("Alhendín CF", listOf(demoPlayer)))
        val tombstone = real.copy(id = 3, deletedAt = 99L)
        assertTrue(DevSeedData.canSeedOwnSquad(demoTeam, listOf(tombstone)))
        assertTrue(DevSeedData.canSeedOwnSquad("Alhendín CF", listOf(tombstone)))
    }

    private data class RelationSnap(
        val id: Int,
        val taskId: Int,
        val sortOrder: Int,
        val deletedAt: Long?,
        val syncId: String
    )

    private data class LinkSnap(
        val id: Int,
        val label: String,
        val url: String,
        val sortOrder: Int,
        val deletedAt: Long?,
        val syncId: String
    )

    private data class PlayerSnap(
        val id: Int,
        val name: String,
        val deletedAt: Long?,
        val syncId: String
    )

    private data class BoardSnap(
        val id: Int,
        val name: String,
        val sceneJson: String,
        val syncId: String,
        val deletedAt: Long?
    )

    private data class TaskBoardLink(
        val id: Int,
        val boardSyncId: String?
    )

    private data class CallupSnap(
        val id: Int,
        val playerId: Int,
        val callupStatus: String,
        val deletedAt: Long?,
        val syncId: String
    )

    private data class SeedSnapshot(
        val taskIds: List<Int>,
        val clubIds: List<Int>,
        val trainingIds: List<Int>,
        val relations: List<RelationSnap>,
        val analysisIds: List<Int>,
        val linkIds: List<Int>,
        val playerIds: List<Int>,
        val boardIds: List<Int>,
        val taskBoardLinks: List<TaskBoardLink>,
        val ownPlayerIds: List<Int>,
        val callups: List<CallupSnap>
    )

    private suspend fun snapshot(h: Harness) = SeedSnapshot(
        taskIds = h.stores.taskDao.getAllOnce().map { it.id },
        clubIds = h.stores.clubDao.getAllOnce().map { it.id },
        trainingIds = h.stores.trainingDao.getAllOnce().map { it.id },
        relations = h.trainingTaskDao.getAllOnce().map {
            RelationSnap(it.id, it.taskId, it.sortOrder, it.deletedAt, it.syncId)
        },
        analysisIds = h.stores.analysisDao.getAllOnce().map { it.id },
        linkIds = h.stores.linkDao.getAllOnce().map { it.id },
        playerIds = h.stores.playerDao.getAllOnce().map { it.id },
        boardIds = h.stores.boardDao.getAllOnce().map { it.id },
        taskBoardLinks = h.stores.taskDao.getAllOnce().map { TaskBoardLink(it.id, it.boardSyncId) },
        ownPlayerIds = h.stores.ownPlayerDao.getAllOnce().map { it.id },
        callups = h.stores.matchDao.getAllMatchPlayersOnce().map {
            CallupSnap(it.id, it.playerId, it.callupStatus, it.deletedAt, it.syncId)
        }
    )
}
