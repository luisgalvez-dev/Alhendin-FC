package com.luis.alhendinfc.data.local

import com.luis.alhendinfc.domain.model.CalendarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EntityWritesTest {

    private val now = 1_700_000_000_000L

    @Test
    fun editTeam_preservesSyncIdAndCreatedAt() {
        val existing = TeamEntity(
            id = 3, name = "A", category = "c", season = "s",
            syncId = "team-sync", createdAt = 10L, updatedAt = 10L
        )
        val incoming = existing.copy(name = "B", syncId = "stale", createdAt = 99L)
        val updated = EntityWrites.teamForUpdate(existing, incoming, now)
        assertEquals("team-sync", updated.syncId)
        assertEquals(10L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("B", updated.name)
        assertNull(updated.deletedAt)
    }

    @Test
    fun teamIsSelectedOnly_doesNotBumpUpdatedAt() {
        val existing = TeamEntity(
            id = 1, name = "A", category = "c", season = "s", isSelected = false,
            syncId = "s1", createdAt = 5L, updatedAt = 5L
        )
        val incoming = existing.copy(isSelected = true)
        val updated = EntityWrites.teamForUpdate(existing, incoming, now)
        assertEquals(5L, updated.updatedAt)
        assertEquals(false, updated.isSelected)
    }

    @Test
    fun editPlayer_preservesSyncId() {
        val existing = PlayerEntity(
            id = 8, teamId = 1, name = "P", syncId = "p-sync", createdAt = 1L, updatedAt = 1L
        )
        val updated = EntityWrites.playerForUpdate(existing, existing.copy(name = "Q", syncId = "x"), now)
        assertEquals("p-sync", updated.syncId)
        assertEquals(1L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
    }

    @Test
    fun createAndEditMatch_recalculatesEpochDay() {
        val created = EntityWrites.matchForInsert(
            MatchEntity(id = 0, teamId = 1, date = "07/09/2026"),
            now
        )
        assertEquals(java.time.LocalDate.of(2026, 9, 7).toEpochDay(), created.dateEpochDay)
        assertTrue(created.syncId.isNotBlank())
        val edited = EntityWrites.matchForUpdate(
            created.copy(syncId = "m-sync", createdAt = 11L),
            created.copy(date = "01/02/2026", syncId = "other"),
            now
        )
        assertEquals("m-sync", edited.syncId)
        assertEquals(11L, edited.createdAt)
        assertEquals(java.time.LocalDate.of(2026, 2, 1).toEpochDay(), edited.dateEpochDay)
        assertEquals("01/02/2026", edited.date)
    }

    @Test
    fun createAndEditFixture_recalculatesEpochDay() {
        val inserted = EntityWrites.fixtureForInsert(
            SeasonFixtureEntity(id = 0, teamId = 1, matchday = 1, opponentClubId = 2, date = "7/9/2026"),
            now
        )
        assertEquals(CalendarDate.toEpochDay("07/09/2026"), inserted.dateEpochDay)
        val existing = inserted.copy(id = 9, syncId = "f-sync", createdAt = 3L)
        val updated = EntityWrites.fixtureForUpdate(existing, existing.copy(date = "01/10/2026"), now)
        assertEquals("f-sync", updated.syncId)
        assertEquals(java.time.LocalDate.of(2026, 10, 1).toEpochDay(), updated.dateEpochDay)
        assertEquals(9, updated.id)
    }

    @Test
    fun editClubAndStat_preserveSyncId() {
        val club = OpponentClubEntity(
            id = 4, teamId = 1, name = "Rival", syncId = "c-sync", createdAt = 2L, updatedAt = 2L
        )
        assertEquals("c-sync", EntityWrites.clubForUpdate(club, club.copy(name = "Otro", syncId = "x"), now).syncId)
        val stat = CustomStatTypeEntity(
            id = 1, teamId = 1, code = "X", label = "X", shortLabel = "X",
            createdAt = 50L, syncId = "st-sync", updatedAt = 50L
        )
        val updated = EntityWrites.statForUpdate(stat, stat.copy(label = "Y", syncId = "no"), now)
        assertEquals("st-sync", updated.syncId)
        assertEquals(50L, updated.createdAt)
    }

    @Test
    fun createTask_assignsUuidAndEqualTimestamps() {
        val created = EntityWrites.taskForInsert(
            TaskEntity(id = 0, teamId = 1, name = "Presión"),
            now
        )
        assertTrue(created.syncId.isNotBlank())
        assertEquals(now, created.createdAt)
        assertEquals(now, created.updatedAt)
        assertNull(created.deletedAt)
    }

    @Test
    fun editTask_preservesIdSyncIdAndCreatedAt() {
        val existing = TaskEntity(
            id = 7, syncId = "task-sync", teamId = 1, name = "A",
            createdAt = 11L, updatedAt = 11L
        )
        val updated = EntityWrites.taskForUpdate(
            existing,
            existing.copy(name = "B", syncId = "stale", createdAt = 99L),
            now
        )
        assertEquals(7, updated.id)
        assertEquals("task-sync", updated.syncId)
        assertEquals(11L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("B", updated.name)
        assertNull(updated.deletedAt)
    }

    @Test
    fun matchPlayerUpsert_doesNotOverwriteIdentityColumns() {
        val source = matchDaoSource()
        assertTrue(source.contains("ON CONFLICT(matchId, playerId) DO UPDATE SET"))
        val conflictClause = source
            .substringAfter("ON CONFLICT(matchId, playerId) DO UPDATE SET")
            .substringBefore("\"\"\"")
        assertTrue(conflictClause.contains("callupStatus = excluded.callupStatus"))
        assertTrue(conflictClause.contains("updatedAt = excluded.updatedAt"))
        assertTrue(conflictClause.contains("deletedAt = excluded.deletedAt"))
        assertFalse(conflictClause.contains("id = excluded.id"))
        assertFalse(conflictClause.contains("syncId = excluded.syncId"))
        assertFalse(conflictClause.contains("createdAt = excluded.createdAt"))
    }

    @Test
    fun liveDaoQueries_doNotTouchUpdatedAt() {
        val source = matchDaoSource()
        val clock = source.substringAfter("suspend fun updateLiveClock").substringBefore("suspend fun updateLiveScore")
        val positions = source.substringAfter("suspend fun updateFieldPositions").substringBefore("suspend fun updateLiveClock")
        val score = source.substringAfter("suspend fun updateLiveScore")
        assertFalse(clock.contains("updatedAt"))
        assertFalse(positions.contains("updatedAt"))
        assertFalse(score.contains("updatedAt"))
        val finished = source.substringAfter("suspend fun markFinished").substringBefore("suspend fun updateFieldPositions")
        assertTrue(finished.contains("updatedAt"))
    }

    @Test
    fun trainingInsert_stampsUuidAndEpochDay() {
        val inserted = EntityWrites.trainingForInsert(
            TrainingEntity(teamId = 1, date = "08/09/2026", dateEpochDay = 0L, notes = "n"),
            now
        )
        assertTrue(inserted.syncId.isNotBlank())
        assertEquals(now, inserted.createdAt)
        assertEquals(now, inserted.updatedAt)
        assertNull(inserted.deletedAt)
        assertEquals(CalendarDate.toEpochDay("08/09/2026"), inserted.dateEpochDay)
    }

    @Test
    fun trainingUpdate_preservesIdentityAndDate() {
        val existing = TrainingEntity(
            id = 4, syncId = "tr-1", teamId = 1, date = "08/09/2026",
            dateEpochDay = 20_000L, notes = "a", createdAt = 10L, updatedAt = 10L
        )
        val incoming = existing.copy(notes = "b", date = "09/09/2026", teamId = 9, syncId = "stale", createdAt = 99L)
        val updated = EntityWrites.trainingForUpdate(existing, incoming, now)
        assertEquals("tr-1", updated.syncId)
        assertEquals(10L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("08/09/2026", updated.date)
        assertEquals(20_000L, updated.dateEpochDay)
        assertEquals(1, updated.teamId)
        assertEquals("b", updated.notes)
    }

    @Test
    fun trainingTaskRevive_keepsSyncId() {
        val existing = TrainingTaskEntity(
            id = 2, syncId = "tt-1", trainingId = 1, taskId = 8, sortOrder = 0,
            createdAt = 5L, updatedAt = 5L, deletedAt = 9L
        )
        val revived = EntityWrites.trainingTaskForRevive(existing, sortOrder = 3, now)
        assertEquals("tt-1", revived.syncId)
        assertNull(revived.deletedAt)
        assertEquals(3, revived.sortOrder)
        assertEquals(now, revived.updatedAt)
        assertEquals(5L, revived.createdAt)
    }

    @Test
    fun attachmentInsert_forcesNullRemotePath() {
        val inserted = EntityWrites.attachmentForInsert(
            AttachmentEntity(
                syncId = "att-fixed",
                parentType = "TASK",
                parentSyncId = "task-1",
                mimeType = "image/jpeg",
                name = "a.jpg",
                localPath = "/files/a.jpg",
                remotePath = "should-not-keep"
            ),
            now
        )
        assertEquals("att-fixed", inserted.syncId)
        assertNull(inserted.remotePath)
        assertEquals(now, inserted.createdAt)
        assertNull(inserted.deletedAt)
    }

    @Test
    fun rivalAnalysisUpdate_preservesSyncIdAndClub() {
        val existing = RivalAnalysisEntity(
            id = 4, syncId = "an-1", opponentClubId = 7, usualSystem = "1-4-3-3",
            createdAt = 10L, updatedAt = 10L
        )
        val updated = EntityWrites.rivalAnalysisForUpdate(
            existing,
            existing.copy(usualSystem = "1-4-2-3-1", syncId = "stale", opponentClubId = 99),
            now
        )
        assertEquals("an-1", updated.syncId)
        assertEquals(7, updated.opponentClubId)
        assertEquals(10L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("1-4-2-3-1", updated.usualSystem)
    }

    @Test
    fun rivalLinkRevive_keepsSyncId() {
        val existing = RivalLinkEntity(
            id = 2, syncId = "lk-1", opponentClubId = 7, type = "RFAF",
            label = "a", url = "https://example.com", sortOrder = 0,
            createdAt = 5L, updatedAt = 5L, deletedAt = 9L
        )
        val revived = EntityWrites.rivalLinkForRevive(
            existing,
            existing.copy(label = "b", url = "https://example.com/dev"),
            now
        )
        assertEquals("lk-1", revived.syncId)
        assertNull(revived.deletedAt)
        assertEquals("b", revived.label)
        assertEquals(5L, revived.createdAt)
        assertEquals(now, revived.updatedAt)
    }

    @Test
    fun opponentPlayerUpdate_preservesSyncIdAndClub() {
        val existing = OpponentPlayerEntity(
            id = 3, syncId = "op-1", opponentClubId = 7, name = "Antonio Pérez",
            createdAt = 10L, updatedAt = 10L
        )
        val updated = EntityWrites.opponentPlayerForUpdate(
            existing,
            existing.copy(name = "  Mario López  ", syncId = "stale", opponentClubId = 99),
            now
        )
        assertEquals("op-1", updated.syncId)
        assertEquals(7, updated.opponentClubId)
        assertEquals("Mario López", updated.name)
        assertEquals(10L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
    }

    @Test
    fun boardUpdate_preservesSyncIdAndTeam() {
        val existing = BoardEntity(
            id = 4, syncId = "bd-1", teamId = 1, name = "Original",
            sceneVersion = 1, sceneJson = "{}", createdAt = 10L, updatedAt = 10L
        )
        val updated = EntityWrites.boardForUpdate(
            existing,
            existing.copy(name = "  Nueva  ", syncId = "stale", teamId = 99, sceneJson = "{\"version\":1}"),
            now
        )
        assertEquals("bd-1", updated.syncId)
        assertEquals(1, updated.teamId)
        assertEquals("Nueva", updated.name)
        assertEquals(10L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("{\"version\":1}", updated.sceneJson)
    }

    private fun matchDaoSource(): String = listOf(
        File("src/main/java/com/luis/alhendinfc/data/local/MatchDao.kt"),
        File("app/src/main/java/com/luis/alhendinfc/data/local/MatchDao.kt")
    ).first { it.exists() }.readText()
}
