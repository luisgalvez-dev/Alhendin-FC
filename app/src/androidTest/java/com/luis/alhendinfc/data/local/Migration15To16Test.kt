package com.luis.alhendinfc.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration15To16Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate15To16_keepsPreviousTablesAndCreatesTask() {
        helper.createDatabase(TEST_DB_15, 15).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO player (
                    id, teamId, name, alias, position, jerseyNumber, height, weight, laterality, isActive, observations,
                    syncId, createdAt, updatedAt
                ) VALUES (
                    5, 1, 'Jugador', '', 'MEDIOCENTRO_DEFENSIVO', 10, 0, 0, 'DERECHA', 1, '',
                    'player-sync', 10, 10
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_15, 16, true, Migration15To16)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "player"))
        assertEquals(0L, count(db, "match_table"))
        assertEquals(0L, count(db, "task"))
        assertTrue(hasTable(db, "task"))
        assertTrue(hasIndex(db, "index_task_teamId"))
        assertTrue(hasIndex(db, "index_task_syncId"))
        assertEquals("player-sync", string(db, "SELECT syncId FROM player WHERE id = 5"))

        db.execSQL(
            """
            INSERT INTO task (
                teamId, name, objective, description, syncId, createdAt, updatedAt
            ) VALUES (
                1, 'Presión tras pérdida', 'Recuperar', '', 'task-sync', 20, 20
            )
            """.trimIndent()
        )
        assertEquals(1L, count(db, "task"))
        assertEquals("Presión tras pérdida", string(db, "SELECT name FROM task"))
        db.close()
    }

    @Test
    fun migrate14To16_chainKeepsDataAndAddsTask() {
        helper.createDatabase(TEST_DB_14, 14).apply {
            execSQL("INSERT INTO team (id, name, category, season, isSelected) VALUES (1, 'Alhendin', '1', '25/26', 1)")
            execSQL(
                """
                INSERT INTO player (
                    id, teamId, name, alias, position, jerseyNumber, height, weight, laterality, isActive, observations
                ) VALUES (5, 1, 'Jugador', '', 'MEDIOCENTRO_DEFENSIVO', 10, 0, 0, 'DERECHA', 1, '')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO match_table (
                    id, teamId, rival, stadium, date, time, matchday, isHome, durationPerPart, numParts,
                    formation, notes, status, livePeriod, liveElapsedSeconds, liveClockRunning,
                    liveClockAnchorWallMs, fieldSecondsJson, fieldPositionsJson
                ) VALUES (
                    12, 1, 'Rival', 'Estadio', '07/09/2026', '12:00', 1, 1, 45, 2,
                    '', '', 'FINISHED', 1, 0, 0, 0, '', ''
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_14,
            16,
            true,
            Migration14To15,
            Migration15To16
        )
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "player"))
        assertEquals(1L, count(db, "match_table"))
        assertEquals(12L, scalar(db, "SELECT id FROM match_table"))
        assertTrue(string(db, "SELECT syncId FROM player").isNotBlank())
        assertEquals(0L, count(db, "task"))
        assertTrue(hasTable(db, "task"))
        db.close()
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Long =
        scalar(db, "SELECT COUNT(*) FROM $table")

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Long {
        val cursor = db.query(sql)
        cursor.moveToFirst()
        val value = cursor.getLong(0)
        cursor.close()
        return value
    }

    private fun string(db: SupportSQLiteDatabase, sql: String): String {
        val cursor = db.query(sql)
        cursor.moveToFirst()
        val value = cursor.getString(0)
        cursor.close()
        return value
    }

    private fun hasIndex(db: SupportSQLiteDatabase, name: String): Boolean {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf(name)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    private fun hasTable(db: SupportSQLiteDatabase, name: String): Boolean {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(name)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    companion object {
        private const val TEST_DB_15 = "migration-15-16-test"
        private const val TEST_DB_14 = "migration-14-16-test"
    }
}
