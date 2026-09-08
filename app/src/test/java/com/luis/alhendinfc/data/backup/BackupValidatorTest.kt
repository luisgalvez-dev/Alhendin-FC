package com.luis.alhendinfc.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun validEmptyBackupV15_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(15))
        assertEquals(15, payload.schemaVersion)
        assertEquals(0, payload.counts.teams)
    }

    @Test
    fun validEmptyBackupV14_isUpgraded() {
        val payload = BackupValidator.validateJson(validJson(14))
        assertEquals(15, payload.schemaVersion)
        assertEquals(0, payload.counts.players)
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
        fun validJson(schemaVersion: Int) = """
            {
              "schemaVersion": $schemaVersion,
              "teams": [],
              "players": [],
              "matches": [],
              "matchPlayers": [],
              "events": [],
              "customStatTypes": [],
              "opponentClubs": [],
              "fixtures": []
            }
        """.trimIndent()
    }
}
