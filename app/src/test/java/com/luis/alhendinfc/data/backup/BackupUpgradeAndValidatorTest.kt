package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.domain.model.CalendarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUpgradeAndValidatorTest {

    @Test
    fun v14Upgrade_assignsUuidsAndEpochDay_keepsIds() {
        val originalJson = v14Json()
        val payload = BackupValidator.validateJson(originalJson)
        assertEquals(15, payload.schemaVersion)
        assertEquals(1, payload.teams.size)
        assertEquals(17, payload.teams[0].id)
        assertTrue(payload.teams[0].syncId.isNotBlank())
        assertEquals(4, payload.matches[0].id)
        assertEquals("07/09/2026", payload.matches[0].date)
        assertEquals(CalendarDate.toEpochDay("07/09/2026"), payload.matches[0].dateEpochDay)
        assertEquals(99L, payload.events[0].createdAt)
        assertEquals(99L, payload.events[0].updatedAt)
        assertNull(payload.teams[0].deletedAt)
        assertEquals(originalJson, v14Json())
    }

    @Test
    fun v15_preservesSyncIds() {
        val payload = BackupValidator.validateJson(v15Json("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.teams[0].syncId)
        assertEquals(15, payload.schemaVersion)
    }

    @Test
    fun v15_emptySyncId_isRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(v15Json("   "))
        }
        assertTrue(error.message!!.contains("syncId"))
    }

    @Test
    fun v15_missingSyncId_isRejected() {
        val json = v15Json("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            .replace("\"syncId\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",", "")
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("syncId"))
    }

    @Test
    fun v15_duplicateSyncId_isRejected() {
        val json = """
            {
              "schemaVersion": 15,
              "teams": [
                {"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "dup"},
                {"id": 2, "name": "B", "category": "", "season": "", "isSelected": false, "syncId": "dup"}
              ],
              "players": [], "matches": [], "matchPlayers": [], "events": [],
              "customStatTypes": [], "opponentClubs": [], "fixtures": []
            }
        """.trimIndent()
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("duplicado"))
    }

    @Test
    fun schema13And16_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(BackupValidatorTest.validJson(13))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(BackupValidatorTest.validJson(16))
        }
    }

    @Test
    fun upgradeDoesNotReuseSameUuid() {
        val a = BackupUpgrade.toV15(
            ValidatedBackup(
                schemaVersion = 14,
                teams = listOf(TeamEntity(id = 1, name = "A"), TeamEntity(id = 2, name = "B")),
                players = emptyList(),
                matches = listOf(MatchEntity(id = 9, teamId = 1, date = "07/09/2026")),
                matchPlayers = emptyList(),
                events = listOf(
                    MatchEventEntity(id = 1, matchId = 9, typeCode = "GOAL", createdAt = 5L)
                ),
                customStatTypes = emptyList(),
                opponentClubs = emptyList(),
                fixtures = emptyList(),
                homeLayout = null,
                counts = BackupCounts(2, 0, 1, 0, 1, 0, 0, 0)
            ),
            now = 100L
        )
        assertNotEquals(a.teams[0].syncId, a.teams[1].syncId)
        assertEquals(5L, a.events[0].createdAt)
        assertEquals(5L, a.events[0].updatedAt)
    }

    private fun v14Json() = """
        {
          "schemaVersion": 14,
          "teams": [{"id": 17, "name": "Alhendin", "category": "", "season": "", "isSelected": true}],
          "players": [{"id": 8, "teamId": 17, "name": "Jugador"}],
          "matches": [{"id": 4, "teamId": 17, "date": "07/09/2026", "status": "FINISHED"}],
          "matchPlayers": [{"id": 1, "matchId": 4, "playerId": 8, "callupStatus": "TITULAR"}],
          "events": [{"id": 2, "matchId": 4, "typeCode": "GOAL", "createdAt": 99}],
          "customStatTypes": [],
          "opponentClubs": [],
          "fixtures": [{"id": 3, "teamId": 17, "matchday": 1, "opponentClubId": 0, "date": "7/9/2026"}]
        }
    """.trimIndent()

    private fun v15Json(syncId: String) = """
        {
          "schemaVersion": 15,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "$syncId", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [], "opponentClubs": [], "fixtures": []
        }
    """.trimIndent()
}
