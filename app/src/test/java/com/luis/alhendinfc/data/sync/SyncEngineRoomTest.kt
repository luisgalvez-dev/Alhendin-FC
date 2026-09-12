package com.luis.alhendinfc.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.luis.alhendinfc.cloud.CloudDoc
import com.luis.alhendinfc.cloud.CloudStore
import com.luis.alhendinfc.cloud.MemoryCloudStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import com.luis.alhendinfc.domain.repository.TeamRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SyncEngineRoomTest {

    private lateinit var db: AlhendinDatabase
    private lateinit var store: MemoryCloudStore
    private lateinit var engine: SyncEngine
    private lateinit var teams: TeamRepositoryImpl
    private lateinit var matches: MatchRepositoryImpl

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AlhendinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SyncHooks.gate = SyncWriteGate(db)
        store = MemoryCloudStore()
        engine = SyncEngine(db, store, SyncRegistry(db), isOnline = { true })
        teams = TeamRepositoryImpl(db.teamDao())
        matches = MatchRepositoryImpl(db.matchDao(), db.matchEventDao())
    }

    @After
    fun tearDown() {
        SyncHooks.gate = null
        db.close()
    }

    @Test
    fun outbox_writeEnqueues_andCollapses() = runTest {
        val id = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        assertEquals(1, db.syncOutboxDao().count())
        val created = db.teamDao().getByIdOnce(id)!!
        teams.updateTeam(Team(id = id, name = "B", category = "1", season = "25/26"))
        teams.updateTeam(Team(id = id, name = "C", category = "1", season = "25/26"))
        assertEquals(1, db.syncOutboxDao().count())
        val row = db.syncOutboxDao().getAll().single()
        assertEquals(SyncEntityType.TEAM, row.entityType)
        assertEquals(created.syncId, row.entitySyncId)
    }

    @Test
    fun outbox_successClears_failureKeeps() = runTest {
        teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        engine.sync()
        assertEquals(0, db.syncOutboxDao().count())

        teams.addTeam(Team(name = "B", category = "1", season = "25/26"))
        val failing = SyncEngine(
            db,
            FailingPutStore(store),
            SyncRegistry(db),
            isOnline = { true }
        )
        runCatching { failing.sync() }
        assertEquals(1, db.syncOutboxDao().count())
    }

    @Test
    fun remoteApply_doesNotEnqueue() = runTest {
        val before = db.syncOutboxDao().count()
        engine.applyRemoteCollection(
            SyncEntityType.TEAM,
            listOf(
                CloudDoc(
                    "remote-team",
                    mapOf(
                        "syncId" to "remote-team",
                        "name" to "Cloud",
                        "category" to "1",
                        "season" to "25/26",
                        "createdAt" to 10L,
                        "updatedAt" to 10L,
                        "deletedAt" to null
                    )
                )
            )
        )
        assertEquals(before, db.syncOutboxDao().count())
        assertNotNull(db.teamDao().getBySyncIdIncludingDeleted("remote-team"))
    }

    @Test
    fun relations_unknownParentIsDeferred() = runTest {
        engine.applyRemoteCollection(
            SyncEntityType.PLAYER,
            listOf(
                CloudDoc(
                    "p1",
                    mapOf(
                        "syncId" to "p1",
                        "teamSyncId" to "missing-team",
                        "name" to "Hugo",
                        "createdAt" to 5L,
                        "updatedAt" to 5L
                    )
                )
            )
        )
        assertEquals(0, db.playerDao().getAllOnce().size)
        engine.applyRemoteCollection(
            SyncEntityType.TEAM,
            listOf(
                CloudDoc(
                    "missing-team",
                    mapOf(
                        "syncId" to "missing-team",
                        "name" to "Padre",
                        "category" to "1",
                        "season" to "25/26",
                        "createdAt" to 1L,
                        "updatedAt" to 1L
                    )
                )
            )
        )
        engine.sync()
        assertNotNull(db.teamDao().getBySyncIdIncludingDeleted("missing-team"))
    }

    @Test
    fun live_doesNotPushTransient_finishedEnables() = runTest {
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val team = db.teamDao().getByIdOnce(teamId)!!
        val stamped = EntityWrites.matchForInsert(
            MatchEntity(teamId = teamId, rival = "Rival", status = MatchStatus.OPEN.name),
            EntitySync.now()
        )
        val matchId = SyncHooks.local(SyncEntityType.MATCH, stamped.syncId) {
            db.matchDao().insertMatch(stamped).toInt()
        }
        db.syncOutboxDao().clear()
        matches.startLiveMatch(matchId)
        assertEquals(0, db.syncOutboxDao().count())
        matches.updateLiveClock(matchId, 30, true, 1L, 1, "secs")
        assertEquals(0, db.syncOutboxDao().count())
        val live = db.matchDao().getByIdOnce(matchId)!!
        assertEquals(MatchStatus.LIVE.name, live.status)
        assertFalse(LiveMatchGuard.shouldPushMatch(live.status))

        matches.markMatchFinished(matchId, 2, 1, 2, 5400, "final-clock", "")
        assertTrue(db.syncOutboxDao().getAll().any { it.entityType == SyncEntityType.MATCH })
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        engine.sync()
        val cloud = store.get(SyncEntityType.MATCH, live.syncId)
        assertNotNull(cloud)
        assertEquals("FINISHED", cloud!!.data["status"])
        assertEquals("final-clock", cloud.data["fieldSecondsJson"])
        assertEquals(team.syncId, cloud.data["teamSyncId"])
    }

    @Test
    fun live_eventsStayOffOutbox_untilFinishedEnqueuesAll() = runTest {
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val stamped = EntityWrites.matchForInsert(
            MatchEntity(teamId = teamId, rival = "Rival", status = MatchStatus.OPEN.name),
            EntitySync.now()
        )
        val matchId = SyncHooks.local(SyncEntityType.MATCH, stamped.syncId) {
            db.matchDao().insertMatch(stamped).toInt()
        }
        val player = EntityWrites.playerForInsert(
            PlayerEntity(teamId = teamId, name = "Paco"),
            EntitySync.now()
        )
        val playerId = db.playerDao().insert(player).toInt()
        matches.setPlayerCallup(matchId, playerId, CallupStatus.TITULAR)
        db.syncOutboxDao().clear()

        matches.startLiveMatch(matchId)
        matches.addEvent(MatchEvent.builtin(matchId, StatisticType.GOAL, playerId = playerId, minute = 12))
        matches.addEvent(MatchEvent.builtin(matchId, StatisticType.ASSIST, playerId = playerId, minute = 12))
        matches.addEvent(MatchEvent.builtin(matchId, StatisticType.YELLOW_CARD, playerId = playerId, minute = 40))
        matches.updateLiveClock(matchId, 90, true, 1L, 1, "provisional")
        matches.updateLiveScore(matchId, 1, 0)
        assertEquals(0, db.syncOutboxDao().count())

        matches.markMatchFinished(matchId, 2, 1, 2, 5400, "final-minutes", "")
        val pending = db.syncOutboxDao().getAll()
        assertTrue(pending.any { it.entityType == SyncEntityType.MATCH && it.entitySyncId == stamped.syncId })
        val eventRows = db.matchEventDao().getByMatchIncludingDeleted(matchId)
        assertEquals(3, eventRows.size)
        eventRows.forEach { event ->
            assertTrue(event.syncId.isNotBlank())
            assertTrue(
                pending.any {
                    it.entityType == SyncEntityType.MATCH_EVENT && it.entitySyncId == event.syncId
                }
            )
        }
        val callup = db.matchDao().getAllMatchPlayersOnce().filter { it.matchId == matchId }
        assertTrue(callup.isNotEmpty())
        callup.forEach { row ->
            assertTrue(
                pending.any {
                    it.entityType == SyncEntityType.MATCH_PLAYER && it.entitySyncId == row.syncId
                }
            )
        }
        val finished = db.matchDao().getByIdOnce(matchId)!!
        assertEquals(MatchStatus.FINISHED.name, finished.status)
        assertEquals(2, finished.homeScore)
        assertEquals("final-minutes", finished.fieldSecondsJson)
    }

    @Test
    fun bootstrap_claimHappensBeforeUploadAndMarksInitializedAfter() = runTest {
        val recorder = OrderCloudStore(store)
        val ordered = SyncEngine(db, recorder, SyncRegistry(db), isOnline = { true })
        teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        ordered.sync()
        val claimAt = recorder.events.indexOf("claim")
        val firstPut = recorder.events.indexOf("put")
        val initAt = recorder.events.indexOf("init")
        assertTrue(claimAt >= 0)
        assertTrue(firstPut >= 0)
        assertTrue(initAt >= 0)
        assertTrue(claimAt < firstPut)
        assertTrue(firstPut < initAt)
        assertEquals(true, store.getWorkspace()?.initialized)
    }

    @Test
    fun bootstrap_concurrentClaims_onlyOneWins() = runTest {
        val results = listOf(
            async { store.claimBootstrap() },
            async { store.claimBootstrap() }
        ).awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(false, store.claimBootstrap())
        store.markInitialized(1L, 1)
        assertEquals(true, store.getWorkspace()?.initialized)
    }

    @Test
    fun seed_secondDeviceDoesNotUploadDuplicateDevTasks() = runTest {
        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val teamId = teams.addTeam(Team(name = "[DEV] Equipo de prueba", category = "1", season = "25/26"))
        taskRepo.add(Task(teamId = teamId, name = "[DEV] Task X", objective = "A"))
        engine.sync()
        assertEquals(1, store.list(SyncEntityType.TASK).size)
        val winnerTaskId = store.list(SyncEntityType.TASK).single().id

        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbB = Room.inMemoryDatabaseBuilder(ctx, AlhendinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val gateB = SyncWriteGate(dbB)
        SyncHooks.gate = gateB
        val teamsB = TeamRepositoryImpl(dbB.teamDao())
        val tasksB = TaskRepositoryImpl(dbB.taskDao())
        val teamB = teamsB.addTeam(Team(name = "[DEV] Equipo de prueba", category = "1", season = "25/26"))
        tasksB.add(Task(teamId = teamB, name = "[DEV] Task X", objective = "B"))
        val engineB = SyncEngine(dbB, store, SyncRegistry(dbB), isOnline = { true })
        engineB.sync()
        engineB.sync()

        assertEquals(1, store.list(SyncEntityType.TASK).size)
        assertEquals(winnerTaskId, store.list(SyncEntityType.TASK).single().id)
        val livingB = dbB.taskDao().getAllOnce().filter { it.deletedAt == null }
        assertEquals(1, livingB.size)
        assertEquals(winnerTaskId, livingB.single().syncId)
        SyncHooks.gate = SyncWriteGate(db)
        dbB.close()
    }

    @Test
    fun remote_doesNotOverwriteLocalLive() = runTest {
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        db.syncOutboxDao().clear()
        val stamped = EntityWrites.matchForInsert(
            MatchEntity(teamId = teamId, rival = "Rival", status = MatchStatus.OPEN.name),
            EntitySync.now()
        )
        val matchId = db.matchDao().insertMatch(stamped).toInt()
        matches.startLiveMatch(matchId)
        matches.updateLiveClock(matchId, 120, true, 9L, 2, "live-secs")
        val live = db.matchDao().getByIdOnce(matchId)!!
        engine.applyRemoteCollection(
            SyncEntityType.MATCH,
            listOf(
                CloudDoc(
                    live.syncId,
                    mapOf(
                        "syncId" to live.syncId,
                        "teamSyncId" to db.teamDao().getByIdOnce(teamId)!!.syncId,
                        "rival" to "Otro",
                        "status" to "FINISHED",
                        "homeScore" to 9,
                        "awayScore" to 9,
                        "createdAt" to live.createdAt,
                        "updatedAt" to live.updatedAt + 50_000
                    )
                )
            )
        )
        val still = db.matchDao().getByIdOnce(matchId)!!
        assertEquals(MatchStatus.LIVE.name, still.status)
        assertEquals(120, still.liveElapsedSeconds)
        assertEquals("live-secs", still.fieldSecondsJson)
    }

    @Test
    fun bootstrap_emptyCloudDoesNotWipeRoom() = runTest {
        teams.addTeam(Team(name = "Local", category = "1", season = "25/26"))
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        engine.sync()
        assertEquals(1, db.teamDao().getAllOnce().size)
        assertEquals("Local", db.teamDao().getAllOnce().single().name)
    }

    @Test
    fun parentResolution_playerNeedsTeam() = runTest {
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val team = db.teamDao().getByIdOnce(teamId)!!
        val player = EntityWrites.playerForInsert(
            PlayerEntity(teamId = teamId, name = "Paco", syncId = "", createdAt = 0, updatedAt = 0),
            EntitySync.now()
        )
        SyncHooks.local(SyncEntityType.PLAYER, player.syncId) {
            db.playerDao().insert(player)
        }
        store.setWorkspace(initialized = true, 1L, 1)
        engine.sync()
        val cloud = store.get(SyncEntityType.PLAYER, player.syncId)!!
        assertEquals(team.syncId, cloud.data["teamSyncId"])
        assertFalse(cloud.data.containsKey("teamId"))
    }

    @Test
    fun task_pendingOnlineEdit_oldRemoteSnapshotDoesNotRevert() = runTest {
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        val tasks = TaskRepositoryImpl(db.taskDao())
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val team = db.teamDao().getByIdOnce(teamId)!!
        val taskId = tasks.add(
            Task(teamId = teamId, name = "Pressing", durationMinutes = 5, playerCount = 8)
        )
        engine.sync()
        val local = db.taskDao().getByIdOnce(taskId)!!
        val remoteBefore = store.get(SyncEntityType.TASK, local.syncId)!!
        assertEquals(5, remoteBefore.data["durationMinutes"])

        tasks.update(
            Task(
                id = taskId,
                teamId = teamId,
                name = "Pressing",
                durationMinutes = 8,
                playerCount = 8,
                syncId = local.syncId
            )
        )
        assertEquals(1, db.syncOutboxDao().count())
        val pending = db.taskDao().getByIdOnce(taskId)!!
        assertEquals(8, pending.durationMinutes)

        engine.applyRemoteCollection(
            SyncEntityType.TASK,
            listOf(
                CloudDoc(
                    local.syncId,
                    mapOf(
                        "syncId" to local.syncId,
                        "teamSyncId" to team.syncId,
                        "name" to "Pressing",
                        "objective" to "",
                        "playerCount" to 8,
                        "durationMinutes" to 5,
                        "description" to "",
                        "boardSyncId" to null,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to null
                    )
                )
            )
        )
        val afterStale = db.taskDao().getByIdOnce(taskId)!!
        assertEquals(8, afterStale.durationMinutes)
        assertEquals(1, db.syncOutboxDao().count())

        engine.sync()
        val pushed = store.get(SyncEntityType.TASK, local.syncId)!!
        assertEquals(8, pushed.data["durationMinutes"])
        assertEquals(0, db.syncOutboxDao().count())
        assertEquals(8, db.taskDao().getByIdOnce(taskId)!!.durationMinutes)
    }

    @Test
    fun task_pendingOnlineDelete_oldActiveRemoteDoesNotResurrect() = runTest {
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        val tasks = TaskRepositoryImpl(db.taskDao())
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val team = db.teamDao().getByIdOnce(teamId)!!
        val taskId = tasks.add(Task(teamId = teamId, name = "Rondo", durationMinutes = 5))
        engine.sync()
        val local = db.taskDao().getBySyncIdIncludingDeleted(
            db.taskDao().getByIdIncludingDeleted(taskId)!!.syncId
        )!!
        tasks.delete(Task(id = taskId, teamId = teamId, name = "Rondo", syncId = local.syncId))
        assertNotNull(db.taskDao().getByIdIncludingDeleted(taskId)!!.deletedAt)
        assertEquals(1, db.syncOutboxDao().count())

        engine.applyRemoteCollection(
            SyncEntityType.TASK,
            listOf(
                CloudDoc(
                    local.syncId,
                    mapOf(
                        "syncId" to local.syncId,
                        "teamSyncId" to team.syncId,
                        "name" to "Rondo",
                        "objective" to "",
                        "durationMinutes" to 5,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to null
                    )
                )
            )
        )
        val still = db.taskDao().getByIdIncludingDeleted(taskId)!!
        assertNotNull(still.deletedAt)
        assertEquals(null, db.taskDao().getByIdOnce(taskId))

        engine.sync()
        val cloud = store.get(SyncEntityType.TASK, local.syncId)!!
        assertNotNull(cloud.deletedAt)
        val other = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlhendinDatabase::class.java
        ).allowMainThreadQueries().build()
        SyncHooks.gate = SyncWriteGate(other)
        val engineB = SyncEngine(other, store, SyncRegistry(other), isOnline = { true })
        engineB.sync()
        val remoteCopy = other.taskDao().getBySyncIdIncludingDeleted(local.syncId)!!
        assertNotNull(remoteCopy.deletedAt)
        SyncHooks.gate = SyncWriteGate(db)
        other.close()
    }

    @Test
    fun task_offlineEdit_reconnectPushes() = runTest {
        var online = true
        val gated = SyncEngine(db, store, SyncRegistry(db), isOnline = { online })
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        val tasks = TaskRepositoryImpl(db.taskDao())
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val taskId = tasks.add(Task(teamId = teamId, name = "Rueda", durationMinutes = 5, playerCount = 6))
        gated.sync()
        online = false
        val local = db.taskDao().getByIdOnce(taskId)!!
        tasks.update(
            Task(
                id = taskId,
                teamId = teamId,
                name = "Rueda",
                durationMinutes = 12,
                playerCount = 10,
                syncId = local.syncId
            )
        )
        assertEquals(12, db.taskDao().getByIdOnce(taskId)!!.durationMinutes)
        gated.sync()
        assertEquals(5, store.get(SyncEntityType.TASK, local.syncId)!!.data["durationMinutes"])
        online = true
        gated.sync()
        assertEquals(12, store.get(SyncEntityType.TASK, local.syncId)!!.data["durationMinutes"])
        assertEquals(10, store.get(SyncEntityType.TASK, local.syncId)!!.data["playerCount"])
        assertEquals(0, db.syncOutboxDao().count())
    }

    @Test
    fun task_offlineDelete_reconnectPublishesTombstone() = runTest {
        var online = true
        val gated = SyncEngine(db, store, SyncRegistry(db), isOnline = { online })
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        val tasks = TaskRepositoryImpl(db.taskDao())
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val taskId = tasks.add(Task(teamId = teamId, name = "Oleada", durationMinutes = 5))
        gated.sync()
        val local = db.taskDao().getByIdOnce(taskId)!!
        online = false
        tasks.delete(Task(id = taskId, teamId = teamId, name = "Oleada", syncId = local.syncId))
        assertNotNull(db.taskDao().getByIdIncludingDeleted(taskId)!!.deletedAt)
        gated.sync()
        assertEquals(null, store.get(SyncEntityType.TASK, local.syncId)!!.deletedAt)
        online = true
        gated.sync()
        assertNotNull(store.get(SyncEntityType.TASK, local.syncId)!!.deletedAt)
        assertEquals(0, db.syncOutboxDao().count())
    }

    @Test
    fun player_pendingOnlineEdit_oldRemoteSnapshotDoesNotRevert() = runTest {
        store.setWorkspace(initialized = true, initializedAt = 1L, schemaVersion = 1)
        val teamId = teams.addTeam(Team(name = "A", category = "1", season = "25/26"))
        val team = db.teamDao().getByIdOnce(teamId)!!
        val player = EntityWrites.playerForInsert(
            PlayerEntity(teamId = teamId, name = "Hugo"),
            EntitySync.now()
        )
        SyncHooks.local(SyncEntityType.PLAYER, player.syncId) {
            db.playerDao().insert(player)
        }
        engine.sync()
        val stored = db.playerDao().getBySyncIdIncludingDeleted(player.syncId)!!
        val updated = stored.copy(name = "Hugo López", updatedAt = EntitySync.now())
        SyncHooks.local(SyncEntityType.PLAYER, player.syncId) {
            db.playerDao().update(updated)
        }
        engine.applyRemoteCollection(
            SyncEntityType.PLAYER,
            listOf(
                CloudDoc(
                    player.syncId,
                    mapOf(
                        "syncId" to player.syncId,
                        "teamSyncId" to team.syncId,
                        "name" to "Hugo",
                        "createdAt" to stored.createdAt,
                        "updatedAt" to stored.updatedAt,
                        "deletedAt" to null
                    )
                )
            )
        )
        assertEquals("Hugo López", db.playerDao().getBySyncIdIncludingDeleted(player.syncId)!!.name)
        engine.sync()
        assertEquals("Hugo López", store.get(SyncEntityType.PLAYER, player.syncId)!!.data["name"])
    }
}

private class FailingPutStore(private val inner: MemoryCloudStore) : CloudStore by inner {
    override suspend fun put(collection: String, id: String, data: Map<String, Any?>) {
        throw IllegalStateException("red")
    }
}

private class OrderCloudStore(private val inner: MemoryCloudStore) : CloudStore by inner {
    val events = mutableListOf<String>()

    override suspend fun claimBootstrap(): Boolean {
        events += "claim"
        return inner.claimBootstrap()
    }

    override suspend fun put(collection: String, id: String, data: Map<String, Any?>) {
        events += "put"
        inner.put(collection, id, data)
    }

    override suspend fun markInitialized(initializedAt: Long, schemaVersion: Int) {
        events += "init"
        inner.markInitialized(initializedAt, schemaVersion)
    }
}
