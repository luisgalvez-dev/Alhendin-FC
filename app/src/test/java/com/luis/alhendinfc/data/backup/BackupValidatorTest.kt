package com.luis.alhendinfc.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun validEmptyBackupV15_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(15))
        assertEquals(20, payload.schemaVersion)
        assertEquals(0, payload.counts.teams)
        assertEquals(0, payload.counts.tasks)
        assertEquals(0, payload.counts.trainings)
        assertEquals(0, payload.counts.rivalAnalyses)
        assertTrue(payload.tasks.isEmpty())
        assertTrue(payload.trainings.isEmpty())
        assertTrue(payload.rivalAnalyses.isEmpty())
    }

    @Test
    fun validEmptyBackupV14_isUpgraded() {
        val payload = BackupValidator.validateJson(validJson(14))
        assertEquals(20, payload.schemaVersion)
        assertEquals(0, payload.counts.players)
        assertTrue(payload.tasks.isEmpty())
        assertTrue(payload.attachments.isEmpty())
        assertTrue(payload.rivalAnalyses.isEmpty())
    }

    @Test
    fun validEmptyBackupV16_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(16))
        assertEquals(20, payload.schemaVersion)
        assertTrue(payload.tasks.isEmpty())
        assertTrue(payload.trainings.isEmpty())
    }

    @Test
    fun validEmptyBackupV17_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(17))
        assertEquals(20, payload.schemaVersion)
        assertTrue(payload.trainings.isEmpty())
        assertTrue(payload.trainingTasks.isEmpty())
        assertTrue(payload.attachments.isEmpty())
        assertTrue(payload.rivalAnalyses.isEmpty())
        assertTrue(payload.rivalLinks.isEmpty())
    }

    @Test
    fun validEmptyBackupV18_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(18))
        assertEquals(20, payload.schemaVersion)
        assertTrue(payload.rivalAnalyses.isEmpty())
        assertTrue(payload.rivalLinks.isEmpty())
        assertTrue(payload.opponentPlayers.isEmpty())
    }

    @Test
    fun validEmptyBackupV19_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(19))
        assertEquals(20, payload.schemaVersion)
        assertTrue(payload.opponentPlayers.isEmpty())
        assertTrue(payload.boards.isEmpty())
    }

    @Test
    fun validEmptyBackupV20_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(20))
        assertEquals(20, payload.schemaVersion)
        assertTrue(payload.boards.isEmpty())
        assertEquals(0, payload.counts.boards)
    }

    @Test
    fun invalidJson_doesNotParse() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson("{")
        }
        assertTrue(error.message!!.contains("JSON"))
    }

    @Test
    fun missingSchemaVersion_isRejected() {
        val json = validJson(15).replace("\"schemaVersion\": 15,", "")
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("schemaVersion"))
    }

    @Test
    fun incompatibleSchemaVersion_isRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(validJson(13))
        }
        assertTrue(error.message!!.contains("incompatible"))
    }

    @Test
    fun missingRequiredArray_isRejected() {
        val json = """{"schemaVersion": 15, "teams": []}"""
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("array obligatorio"))
    }

    @Test
    fun declaredCountsMismatch_isRejected() {
        val json = validJson(15).replace(
            "\"teams\": []",
            "\"counts\": {\"teams\": 9, \"players\": 0, \"matches\": 0, \"matchPlayers\": 0, \"events\": 0, \"customStatTypes\": 0, \"opponentClubs\": 0, \"fixtures\": 0}, \"teams\": []"
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json)
        }
        assertTrue(error.message!!.contains("conteos"))
    }

    companion object {
        fun validJson(schemaVersion: Int): String {
            val extra = buildString {
                if (schemaVersion >= 16) append(",\n              \"tasks\": []")
                if (schemaVersion >= 17) {
                    append(",\n              \"trainings\": []")
                    append(",\n              \"trainingTasks\": []")
                    append(",\n              \"attachments\": []")
                }
                if (schemaVersion >= 18) {
                    append(",\n              \"rivalAnalyses\": []")
                    append(",\n              \"rivalLinks\": []")
                }
                if (schemaVersion >= 19) {
                    append(",\n              \"opponentPlayers\": []")
                }
                if (schemaVersion >= 20) {
                    append(",\n              \"boards\": []")
                }
            }
            return """
            {
              "schemaVersion": $schemaVersion,
              "teams": [],
              "players": [],
              "matches": [],
              "matchPlayers": [],
              "events": [],
              "customStatTypes": [],
              "opponentClubs": [],
              "fixtures": []$extra
            }
            """.trimIndent()
        }
    }
}
