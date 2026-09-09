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
class Migration16To17Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate16To17_keepsPreviousTablesAndCreatesTrainingAttachment() {
        helper.createDatabase(TEST_DB_16, 16).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO task (teamId, name, objective, description, syncId, createdAt, updatedAt)
                VALUES (1, 'Rondo', '', '', 'task-sync', 20, 20)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_16, 17, true, Migration16To17)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "task"))
        assertEquals("Rondo", string(db, "SELECT name FROM task"))
        assertTrue(hasTable(db, "training"))
        assertTrue(hasTable(db, "training_task"))
        assertTrue(hasTable(db, "attachment"))
        assertTrue(hasIndex(db, "index_training_syncId"))
        assertTrue(hasIndex(db, "index_training_task_trainingId_taskId"))
        assertTrue(hasIndex(db, "index_attachment_syncId"))
        assertEquals(0L, count(db, "training"))
        assertEquals(0L, count(db, "attachment"))
        db.close()
    }

    @Test
    fun migrate14To17_chainKeepsDataAndAddsTrainingTables() {
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
            17,
            true,
            Migration14To15,
            Migration15To16,
            Migration16To17
        )
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "player"))
        assertEquals(1L, count(db, "match_table"))
        assertEquals(12L, scalar(db, "SELECT id FROM match_table"))
        assertTrue(hasTable(db, "task"))
        assertTrue(hasTable(db, "training"))
        assertTrue(hasTable(db, "training_task"))
        assertTrue(hasTable(db, "attachment"))
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
        private const val TEST_DB_16 = "migration-16-17-test"
        private const val TEST_DB_14 = "migration-14-17-test"
    }
}
