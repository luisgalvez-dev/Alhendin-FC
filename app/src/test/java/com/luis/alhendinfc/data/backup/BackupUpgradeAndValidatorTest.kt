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
        assertEquals(21, payload.schemaVersion)
        assertEquals(1, payload.teams.size)
        assertEquals(17, payload.teams[0].id)
        assertTrue(payload.teams[0].syncId.isNotBlank())
        assertEquals(4, payload.matches[0].id)
        assertEquals("07/09/2026", payload.matches[0].date)
        assertEquals(CalendarDate.toEpochDay("07/09/2026"), payload.matches[0].dateEpochDay)
        assertEquals(99L, payload.events[0].createdAt)
        assertEquals(99L, payload.events[0].updatedAt)
        assertNull(payload.teams[0].deletedAt)
        assertTrue(payload.tasks.isEmpty())
        assertTrue(payload.trainings.isEmpty())
        assertTrue(payload.trainingTasks.isEmpty())
        assertTrue(payload.attachments.isEmpty())
        assertTrue(payload.rivalAnalyses.isEmpty())
        assertTrue(payload.rivalLinks.isEmpty())
        assertTrue(payload.opponentPlayers.isEmpty())
        assertTrue(payload.boards.isEmpty())
        assertEquals(0, payload.counts.tasks)
        assertEquals(0, payload.counts.trainings)
        assertEquals(originalJson, v14Json())
    }

    @Test
    fun v15_preservesSyncIds() {
        val payload = BackupValidator.validateJson(v15Json("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.teams[0].syncId)
        assertEquals(21, payload.schemaVersion)
        assertTrue(payload.tasks.isEmpty())
        assertTrue(payload.trainings.isEmpty())
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
    fun schema13And22_areRejected_v21IsAccepted() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(BackupValidatorTest.validJson(13))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(BackupValidatorTest.validJson(22))
        }
        val v21 = BackupValidator.validateJson(BackupValidatorTest.validJson(21))
        assertEquals(21, v21.schemaVersion)
        val v20 = BackupValidator.validateJson(BackupValidatorTest.validJson(20))
        assertEquals(21, v20.schemaVersion)
        val v19 = BackupValidator.validateJson(BackupValidatorTest.validJson(19))
        assertEquals(21, v19.schemaVersion)
        assertTrue(v19.boards.isEmpty())
        val v18 = BackupValidator.validateJson(BackupValidatorTest.validJson(18))
        assertEquals(21, v18.schemaVersion)
        val v17 = BackupValidator.validateJson(BackupValidatorTest.validJson(17))
        assertEquals(21, v17.schemaVersion)
        val v16 = BackupValidator.validateJson(BackupValidatorTest.validJson(16))
        assertEquals(21, v16.schemaVersion)
    }

    @Test
    fun v16_restoresTasksKeepingIdsSyncIdsAndTombstones() {
        val payload = BackupValidator.validateJson(v16JsonWithTasks())
        assertEquals(21, payload.schemaVersion)
        assertEquals(2, payload.tasks.size)
        assertEquals(10, payload.tasks[0].id)
        assertEquals("task-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.tasks[0].syncId)
        assertEquals(1, payload.tasks[0].teamId)
        assertEquals("Presión tras pérdida", payload.tasks[0].name)
        assertNull(payload.tasks[0].deletedAt)
        assertEquals(11, payload.tasks[1].id)
        assertEquals("task-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb", payload.tasks[1].syncId)
        assertEquals(1_700L, payload.tasks[1].deletedAt)
        assertEquals(2, payload.counts.tasks)
        assertTrue(payload.trainings.isEmpty())
    }

    @Test
    fun v16_emptySyncId_isRejected() {
        val json = v16JsonWithTasks().replace(
            "\"syncId\": \"task-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"",
            "\"syncId\": \"   \""
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("syncId"))
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

    @Test
    fun v17_restoresTrainingTasksAttachmentsKeepingIdsSyncIdsAndTombstones() {
        val payload = BackupValidator.validateJson(v17Json())
        assertEquals(21, payload.schemaVersion)
        assertEquals(1, payload.trainings.size)
        assertEquals(21, payload.trainings[0].id)
        assertEquals("tr-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.trainings[0].syncId)
        assertEquals(1, payload.trainings[0].teamId)
        assertNull(payload.trainings[0].deletedAt)
        assertEquals(1, payload.trainingTasks.size)
        assertEquals("tt-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.trainingTasks[0].syncId)
        assertEquals(2, payload.attachments.size)
        assertEquals(30, payload.attachments[0].id)
        assertEquals("att-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.attachments[0].syncId)
        assertNull(payload.attachments[0].deletedAt)
        assertEquals(31, payload.attachments[1].id)
        assertEquals(2_000L, payload.attachments[1].deletedAt)
        assertEquals(1, payload.counts.trainings)
        assertEquals(1, payload.counts.trainingTasks)
        assertEquals(2, payload.counts.attachments)
        assertTrue(payload.rivalAnalyses.isEmpty())
        assertTrue(payload.rivalLinks.isEmpty())
    }

    @Test
    fun v18_restoresAnalysesAndLinksKeepingIdsSyncIdsAndTombstones() {
        val payload = BackupValidator.validateJson(v18Json())
        assertEquals(21, payload.schemaVersion)
        assertEquals(1, payload.rivalAnalyses.size)
        assertEquals(40, payload.rivalAnalyses[0].id)
        assertEquals("an-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.rivalAnalyses[0].syncId)
        assertEquals("1-4-3-3", payload.rivalAnalyses[0].usualSystem)
        assertNull(payload.rivalAnalyses[0].deletedAt)
        assertEquals(2, payload.rivalLinks.size)
        assertEquals(50, payload.rivalLinks[0].id)
        assertEquals("lk-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.rivalLinks[0].syncId)
        assertEquals("RFAF", payload.rivalLinks[0].type)
        assertEquals(51, payload.rivalLinks[1].id)
        assertEquals(3_000L, payload.rivalLinks[1].deletedAt)
        assertEquals(1, payload.counts.rivalAnalyses)
        assertEquals(2, payload.counts.rivalLinks)
        assertTrue(payload.opponentPlayers.isEmpty())
    }

    @Test
    fun v19_restoresOpponentPlayersKeepingIdsSyncIdsAndTombstones() {
        val payload = BackupValidator.validateJson(v19Json())
        assertEquals(21, payload.schemaVersion)
        assertEquals(2, payload.opponentPlayers.size)
        assertEquals(60, payload.opponentPlayers[0].id)
        assertEquals("op-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.opponentPlayers[0].syncId)
        assertEquals("Antonio Pérez", payload.opponentPlayers[0].name)
        assertEquals(7, payload.opponentPlayers[0].opponentClubId)
        assertNull(payload.opponentPlayers[0].deletedAt)
        assertEquals(61, payload.opponentPlayers[1].id)
        assertEquals(4_000L, payload.opponentPlayers[1].deletedAt)
        assertEquals(2, payload.counts.opponentPlayers)
        assertTrue(payload.boards.isEmpty())
        assertEquals(0, payload.counts.boards)
    }

    @Test
    fun v20_restoresBoardsKeepingIdsSyncIdsSceneAndTombstones() {
        val payload = BackupValidator.validateJson(v20Json())
        assertEquals(21, payload.schemaVersion)
        assertEquals(2, payload.boards.size)
        assertEquals(70, payload.boards[0].id)
        assertEquals("bd-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.boards[0].syncId)
        assertEquals(1, payload.boards[0].teamId)
        assertEquals("Salida de balón 3+2", payload.boards[0].name)
        assertEquals(1, payload.boards[0].sceneVersion)
        assertTrue(payload.boards[0].sceneJson.contains("bluePlayer"))
        assertNull(payload.boards[0].deletedAt)
        assertEquals(71, payload.boards[1].id)
        assertEquals(5_000L, payload.boards[1].deletedAt)
        assertEquals(2, payload.counts.boards)
        assertEquals("board-sync-task", payload.tasks[0].boardSyncId)
    }

    @Test
    fun v20_keepsMatchAttachmentsWithoutNewRoomVersion() {
        val payload = BackupValidator.validateJson(v20Json())
        assertEquals(21, payload.schemaVersion)
        assertEquals(2, payload.attachments.size)
        assertEquals("MATCH", payload.attachments[0].parentType)
        assertEquals("match-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", payload.attachments[0].parentSyncId)
        assertEquals("Informe postpartido.pdf", payload.attachments[0].name)
        assertEquals("application/pdf", payload.attachments[0].mimeType)
        assertNull(payload.attachments[0].deletedAt)
        assertEquals("MATCH", payload.attachments[1].parentType)
        assertEquals("image/jpeg", payload.attachments[1].mimeType)
        assertEquals(9_000L, payload.attachments[1].deletedAt)
        assertEquals(2, payload.counts.attachments)
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

    private fun v16JsonWithTasks() = """
        {
          "schemaVersion": 16,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [], "opponentClubs": [], "fixtures": [],
          "tasks": [
            {
              "id": 10,
              "syncId": "task-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "teamId": 1,
              "name": "Presión tras pérdida",
              "objective": "Recuperar",
              "playerCount": 8,
              "durationMinutes": 12,
              "description": "Tras pérdida inmediata",
              "boardSyncId": null,
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 11,
              "syncId": "task-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "teamId": 1,
              "name": "Antigua",
              "objective": "",
              "playerCount": null,
              "durationMinutes": null,
              "description": "",
              "boardSyncId": null,
              "createdAt": 50,
              "updatedAt": 1700,
              "deletedAt": 1700
            }
          ]
        }
    """.trimIndent()

    private fun v17Json() = """
        {
          "schemaVersion": 17,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [], "opponentClubs": [], "fixtures": [],
          "tasks": [
            {
              "id": 10,
              "syncId": "task-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "teamId": 1,
              "name": "Rondo",
              "objective": "",
              "description": "",
              "createdAt": 100,
              "updatedAt": 100
            }
          ],
          "trainings": [
            {
              "id": 21,
              "syncId": "tr-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "teamId": 1,
              "date": "08/09/2026",
              "dateEpochDay": 20700,
              "opponentClubId": null,
              "notes": "Sesión",
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            }
          ],
          "trainingTasks": [
            {
              "id": 5,
              "syncId": "tt-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "trainingId": 21,
              "taskId": 10,
              "sortOrder": 0,
              "createdAt": 100,
              "updatedAt": 100
            }
          ],
          "attachments": [
            {
              "id": 30,
              "syncId": "att-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "parentType": "TRAINING",
              "parentSyncId": "tr-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "mimeType": "application/pdf",
              "name": "sesion.pdf",
              "localPath": "attachments/att-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "remotePath": null,
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 31,
              "syncId": "att-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "parentType": "TASK",
              "parentSyncId": "task-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "mimeType": "image/jpeg",
              "name": "old.jpg",
              "localPath": "/old/path.jpg",
              "remotePath": null,
              "createdAt": 50,
              "updatedAt": 2000,
              "deletedAt": 2000
            }
          ]
        }
    """.trimIndent()

    private fun v18Json() = """
        {
          "schemaVersion": 18,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [],
          "opponentClubs": [{"id": 7, "teamId": 1, "name": "Rival", "shortName": "RIV", "stadium": "", "kitColors": "", "sortOrder": 0, "syncId": "club-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "fixtures": [],
          "tasks": [],
          "trainings": [],
          "trainingTasks": [],
          "attachments": [],
          "rivalAnalyses": [
            {
              "id": 40,
              "syncId": "an-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "opponentClubId": 7,
              "usualSystem": "1-4-3-3",
              "variants": "1-4-2-3-1",
              "buildUp": "Centrales abiertos",
              "progression": "",
              "finalThird": "",
              "highPress": "Salta extremo",
              "midBlock": "",
              "lowBlock": "",
              "transAttackToDefense": "",
              "transDefenseToAttack": "Extremo derecho",
              "cornersOffensive": "",
              "cornersDefensive": "",
              "setPieces": "",
              "strengths": "Juego aéreo",
              "weaknesses": "Espalda de laterales",
              "keyPlayers": "Nº9 fuerte de espaldas",
              "generalNotes": "DEMO",
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            }
          ],
          "rivalLinks": [
            {
              "id": 50,
              "syncId": "lk-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "opponentClubId": 7,
              "type": "RFAF",
              "label": "[DEV] Ficha RFAF DEMO",
              "url": "https://example.com/dev/rfaf",
              "sortOrder": 0,
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 51,
              "syncId": "lk-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "opponentClubId": 7,
              "type": "YOUTUBE",
              "label": "old",
              "url": "https://example.com/old",
              "sortOrder": 1,
              "createdAt": 50,
              "updatedAt": 3000,
              "deletedAt": 3000
            }
          ]
        }
    """.trimIndent()

    private fun v19Json() = """
        {
          "schemaVersion": 19,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [],
          "opponentClubs": [{"id": 7, "teamId": 1, "name": "Rival", "shortName": "RIV", "stadium": "", "kitColors": "", "sortOrder": 0, "syncId": "club-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "fixtures": [],
          "tasks": [],
          "trainings": [],
          "trainingTasks": [],
          "attachments": [],
          "rivalAnalyses": [],
          "rivalLinks": [],
          "opponentPlayers": [
            {
              "id": 60,
              "syncId": "op-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "opponentClubId": 7,
              "name": "Antonio Pérez",
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 61,
              "syncId": "op-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "opponentClubId": 7,
              "name": "Mario López",
              "createdAt": 50,
              "updatedAt": 4000,
              "deletedAt": 4000
            }
          ]
        }
    """.trimIndent()

    private fun v20Json() = """
        {
          "schemaVersion": 20,
          "teams": [{"id": 1, "name": "A", "category": "", "season": "", "isSelected": false, "syncId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "createdAt": 1, "updatedAt": 1}],
          "players": [], "matches": [], "matchPlayers": [], "events": [],
          "customStatTypes": [], "opponentClubs": [],
          "fixtures": [],
          "tasks": [
            {
              "id": 12,
              "syncId": "task-sync-cccc-cccc-cccc-cccccccccccc",
              "teamId": 1,
              "name": "Salida de balón 3+2",
              "objective": "",
              "playerCount": null,
              "durationMinutes": null,
              "description": "",
              "boardSyncId": "board-sync-task",
              "createdAt": 1,
              "updatedAt": 1,
              "deletedAt": null
            }
          ],
          "trainings": [],
          "trainingTasks": [],
          "attachments": [
            {
              "id": 80,
              "syncId": "att-match-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "parentType": "MATCH",
              "parentSyncId": "match-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "mimeType": "application/pdf",
              "name": "Informe postpartido.pdf",
              "localPath": "attachments/att-match-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "remotePath": null,
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 81,
              "syncId": "att-match-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "parentType": "MATCH",
              "parentSyncId": "match-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "mimeType": "image/jpeg",
              "name": "old.jpg",
              "localPath": "/old/foto.jpg",
              "remotePath": null,
              "createdAt": 50,
              "updatedAt": 9000,
              "deletedAt": 9000
            }
          ],
          "rivalAnalyses": [],
          "rivalLinks": [],
          "opponentPlayers": [],
          "boards": [
            {
              "id": 70,
              "syncId": "bd-sync-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "teamId": 1,
              "name": "Salida de balón 3+2",
              "sceneVersion": 1,
              "sceneJson": "{\"version\":1,\"background\":\"FIELD\",\"objects\":[{\"objectId\":\"o1\",\"type\":\"bluePlayer\",\"x\":0.2,\"y\":0.8}]}",
              "createdAt": 100,
              "updatedAt": 100,
              "deletedAt": null
            },
            {
              "id": 71,
              "syncId": "bd-sync-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "teamId": 1,
              "name": "Copia antigua",
              "sceneVersion": 1,
              "sceneJson": "{}",
              "createdAt": 50,
              "updatedAt": 5000,
              "deletedAt": 5000
            }
          ]
        }
    """.trimIndent()
}
