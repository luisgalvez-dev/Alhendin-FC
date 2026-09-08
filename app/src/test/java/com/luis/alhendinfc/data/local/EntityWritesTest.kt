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
    fun matchPlayerUpsert_doesNotOverwriteIdentityColumns() {
        val source = matchDaoSource()
        assertTrue(source.contains("ON CONFLICT(matchId, playerId) DO UPDATE SET"))
        val conflictClause = source
            .substringAfter("ON CONFLICT(matchId, playerId) DO UPDATE SET")
            .substringBefore("\"\"\"")
        assertTrue(conflictClause.contains("callupStatus = excluded.callupStatus"))
        assertTrue(conflictClause.contains("updatedAt = excluded.updatedAt"))
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

    private fun matchDaoSource(): String = listOf(
        File("src/main/java/com/luis/alhendinfc/data/local/MatchDao.kt"),
        File("app/src/main/java/com/luis/alhendinfc/data/local/MatchDao.kt")
    ).first { it.exists() }.readText()
}
