package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.TeamEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncContractTest {

    private val now = 1_800_000_000_000L

    @Test
    fun tombstone_setsDeletedAtAndUpdatedAt() {
        val team = TeamEntity(id = 1, name = "A", category = "c", season = "s", syncId = "s1", createdAt = 10L, updatedAt = 10L)
        val deleted = EntityWrites.applyTombstone({ d, u -> team.copy(deletedAt = d, updatedAt = u) }, now)
        assertEquals(now, deleted.deletedAt)
        assertEquals(now, deleted.updatedAt)
        assertEquals(10L, deleted.createdAt)
        assertEquals("s1", deleted.syncId)
        assertFalse(EntitySync.isActive(deleted.deletedAt))
    }

    @Test
    fun teamProjection_excludesIsSelected() {
        val team = TeamEntity(
            id = 2, name = "Alhendin", category = "1", season = "25/26",
            isSelected = true, syncId = "team-1", createdAt = 1L, updatedAt = 2L
        )
        val projection = TeamSyncMapper.toProjection(team)
        assertEquals("team-1", projection.syncId)
        assertEquals("Alhendin", projection.name)
        val fields = projection.javaClass.declaredFields.map { it.name }
        assertFalse(fields.contains("isSelected"))
        assertFalse(TeamSyncProjection::class.java.declaredFields.any { it.name == "isSelected" })
    }

    @Test
    fun matchProjection_excludesLiveLocalState() {
        val live = MatchEntity(
            id = 9,
            teamId = 1,
            rival = "Rival",
            date = "07/09/2026",
            status = "LIVE",
            homeScore = 1,
            awayScore = 0,
            livePeriod = 2,
            liveElapsedSeconds = 900,
            liveClockRunning = true,
            liveClockAnchorWallMs = 50L,
            fieldSecondsJson = "sec",
            fieldPositionsJson = "pos",
            syncId = "m-1",
            createdAt = 3L,
            updatedAt = 4L
        )
        val projection = MatchSyncMapper.toProjection(live)
        val liveState = MatchSyncMapper.liveLocalState(live)
        assertEquals("Rival", projection.rival)
        assertEquals("LIVE", projection.status)
        assertEquals(1, projection.homeScore)
        assertEquals(900, liveState.liveElapsedSeconds)
        assertTrue(liveState.liveClockRunning)
        val projectionNames = MatchSyncProjection::class.java.declaredFields.map { it.name }.toSet()
        MatchSyncMapper.liveLocalFieldNames().forEach { name ->
            assertFalse("$name no debe ir en la proyección durable", projectionNames.contains(name))
        }
        assertFalse(MatchSyncMapper.shouldPushLiveLocalState())
        val player = MatchSyncMapper.toPlayerProjection(
            MatchPlayerEntity(
                id = 1, matchId = 9, playerId = 5, callupStatus = "TITULAR", isOnField = true,
                syncId = "mp", createdAt = 1L, updatedAt = 1L
            )
        )
        assertEquals("TITULAR", player.callupStatus)
        assertFalse(MatchPlayerSyncProjection::class.java.declaredFields.any { it.name == "isOnField" })
    }

    @Test
    fun finishedMatch_keepsDurableFichaAndScore() {
        val finished = MatchEntity(
            id = 4,
            teamId = 1,
            rival = "Jaen",
            stadium = "Campo",
            date = "01/02/2026",
            matchday = 3,
            isHome = false,
            status = "FINISHED",
            homeScore = 2,
            awayScore = 1,
            opponentClubId = 8,
            formation = "4-3-3",
            notes = "acta",
            livePeriod = 2,
            liveElapsedSeconds = 5400,
            liveClockRunning = false,
            fieldSecondsJson = "final-clock",
            fieldPositionsJson = "final-pos",
            syncId = "fin",
            createdAt = 10L,
            updatedAt = 20L,
            dateEpochDay = 20_000L
        )
        val projection = MatchSyncMapper.toProjection(finished)
        assertTrue(MatchSyncMapper.finishedExposesDurableResult(finished))
        assertEquals("Jaen", projection.rival)
        assertEquals("Campo", projection.stadium)
        assertEquals(3, projection.matchday)
        assertEquals(2, projection.homeScore)
        assertEquals(1, projection.awayScore)
        assertEquals("4-3-3", projection.formation)
        assertEquals("acta", projection.notes)
        assertEquals(8, projection.opponentClubId)
        assertEquals("FINISHED", projection.status)
        assertFalse(MatchSyncProjection::class.java.declaredFields.any { it.name == "liveElapsedSeconds" })
    }

    @Test
    fun attachmentContract_isConceptualOnly() {
        val contract = AttachmentContract(
            id = 0,
            syncId = "att-1",
            parentType = "match",
            parentSyncId = "m-1",
            mimeType = "application/pdf",
            name = "acta.pdf",
            localPath = "/files/acta.pdf",
            remotePath = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        assertEquals("match", contract.parentType)
        assertNull(contract.remotePath)
        val hasRoomEntity = File("app/src/main/java/com/luis/alhendinfc/data/local/AttachmentEntity.kt").exists() ||
            File("src/main/java/com/luis/alhendinfc/data/local/AttachmentEntity.kt").exists()
        assertTrue(hasRoomEntity)
        assertTrue(AttachmentParentType.KNOWN.containsAll(listOf(
            "TRAINING", "TASK", "MATCH", "OPPONENT", "BOARD",
            "PLAYER_PHOTO", "TEAM_SHIELD", "OPPONENT_SHIELD"
        )))
    }

    @Test
    fun homePrefsRemainPersonal() {
        assertTrue(SyncScope.HOME_PREFS_ARE_PERSONAL)
        assertTrue(SyncScope.TEAM_IS_SELECTED_IS_LOCAL)
        val home = listOf(
            File("app/src/main/java/com/luis/alhendinfc/data/preferences/HomePreferencesRepository.kt"),
            File("src/main/java/com/luis/alhendinfc/data/preferences/HomePreferencesRepository.kt")
        ).first { it.exists() }.readText()
        assertTrue(home.contains("No forma parte del dataset deportivo"))
    }

    @Test
    fun uiQueriesHideTombstones_backupReadsThem() {
        val teamDao = daoSource("TeamDao.kt")
        assertTrue(queryAbove(teamDao, "fun getAllTeams(").contains("deletedAt IS NULL"))
        assertFalse(queryAbove(teamDao, "fun getAllOnce(").contains("deletedAt IS NULL"))
        val playerDao = daoSource("PlayerDao.kt")
        assertTrue(queryAbove(playerDao, "fun getAllByTeam(").contains("deletedAt IS NULL"))
        assertFalse(queryAbove(playerDao, "fun getAllOnce(").contains("deletedAt IS NULL"))
        val matchDao = daoSource("MatchDao.kt")
        assertTrue(queryAbove(matchDao, "fun getMatchesByTeam(").contains("deletedAt IS NULL"))
        assertFalse(queryAbove(matchDao, "fun getAllMatchesOnce(").contains("deletedAt IS NULL"))
        assertFalse(queryAbove(matchDao, "fun getAllMatchPlayersOnce(").contains("deletedAt IS NULL"))
        val eventDao = daoSource("MatchEventDao.kt")
        assertTrue(queryAbove(eventDao, "fun getEventsByMatch(").contains("deletedAt IS NULL"))
        assertFalse(queryAbove(eventDao, "fun getAllOnce(").contains("deletedAt IS NULL"))
    }

    @Test
    fun daosMarkDeletedInsteadOfPhysicalDelete() {
        listOf("TeamDao.kt", "PlayerDao.kt", "MatchDao.kt", "MatchEventDao.kt", "CustomStatTypeDao.kt", "CalendarDao.kt")
            .forEach { name ->
                val text = daoSource(name)
                assertFalse("$name no debe usar @Delete", text.contains("@Delete"))
                assertTrue("$name debe marcar tombstone", text.contains("markDeleted"))
            }
        val backup = listOf(
            File("app/src/main/java/com/luis/alhendinfc/data/backup/BackupRestore.kt"),
            File("src/main/java/com/luis/alhendinfc/data/backup/BackupRestore.kt")
        ).first { it.exists() }.readText()
        assertTrue(backup.contains("DELETE FROM"))
        assertTrue(backup.contains("Wipe interno"))
    }

    private fun daoSource(name: String): String = listOf(
        File("src/main/java/com/luis/alhendinfc/data/local/$name"),
        File("app/src/main/java/com/luis/alhendinfc/data/local/$name")
    ).first { it.exists() }.readText()

    private fun queryAbove(source: String, funPrefix: String): String {
        val idx = source.indexOf(funPrefix)
        require(idx >= 0) { "No se encontró $funPrefix" }
        val before = source.substring(0, idx)
        val queryStart = before.lastIndexOf("@Query")
        require(queryStart >= 0) { "No hay @Query sobre $funPrefix" }
        return before.substring(queryStart)
    }
}
