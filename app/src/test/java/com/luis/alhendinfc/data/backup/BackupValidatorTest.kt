package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.AlhendinDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    private val expected = AlhendinDatabase.VERSION

    @Test
    fun validEmptyBackup_isAccepted() {
        val payload = BackupValidator.validateJson(validJson(expected), expected)
        assertEquals(expected, payload.schemaVersion)
        assertEquals(0, payload.counts.teams)
        assertEquals(0, payload.counts.players)
    }

    @Test
    fun invalidJson_doesNotParse() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson("{", expected)
        }
        assertTrue(error.message!!.contains("JSON"))
    }

    @Test
    fun missingSchemaVersion_isRejected() {
        val json = validJson(expected).replace("\"schemaVersion\": $expected,", "")
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json, expected)
        }
        assertTrue(error.message!!.contains("schemaVersion"))
    }

    @Test
    fun incompatibleSchemaVersion_isRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(validJson(expected - 1), expected)
        }
        assertTrue(error.message!!.contains("incompatible"))
    }

    @Test
    fun missingRequiredArray_isRejected() {
        val json = """{"schemaVersion": $expected, "teams": []}"""
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json, expected)
        }
        assertTrue(error.message!!.contains("array obligatorio"))
    }

    @Test
    fun declaredCountsMismatch_isRejected() {
        val json = validJson(expected).replace(
            "\"teams\": []",
            "\"counts\": {\"teams\": 9, \"players\": 0, \"matches\": 0, \"matchPlayers\": 0, \"events\": 0, \"customStatTypes\": 0, \"opponentClubs\": 0, \"fixtures\": 0}, \"teams\": []"
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.validateJson(json, expected)
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
