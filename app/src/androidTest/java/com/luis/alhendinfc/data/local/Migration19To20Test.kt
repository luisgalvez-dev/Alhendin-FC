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
class Migration19To20Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate19To20_keepsPreviousTablesAndAddsBoardSceneColumns() {
        helper.createDatabase(TEST_DB_19, 19).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO opponent_player (id, syncId, opponentClubId, name, createdAt, updatedAt)
                VALUES (2, 'op-sync', 4, 'Antonio Pérez', 20, 20)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_19, 20, true, Migration19To20)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "opponent_player"))
        assertTrue(hasTable(db, "opponent_player"))
        assertTrue(hasTable(db, "board"))
        assertTrue(hasColumn(db, "board", "sceneVersion"))
        assertTrue(hasColumn(db, "board", "sceneJson"))
        assertTrue(hasIndex(db, "index_board_syncId"))
        assertTrue(hasIndex(db, "index_board_teamId"))
        assertEquals(0L, count(db, "board"))
        db.close()
    }

    @Test
    fun migrate14To20_chainKeepsDataAndAddsBoardScene() {
        helper.createDatabase(TEST_DB_14, 14).apply {
            execSQL("INSERT INTO team (id, name, category, season, isSelected) VALUES (1, 'Alhendin', '1', '25/26', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(
            TEST_DB_14,
            20,
            true,
            Migration14To15,
            Migration15To16,
            Migration16To17,
            Migration17To18,
            Migration18To19,
            Migration19To20
        )
        assertEquals(1L, count(db, "team"))
        assertTrue(hasTable(db, "opponent_player"))
        assertTrue(hasTable(db, "rival_analysis"))
        assertTrue(hasTable(db, "board"))
        assertTrue(hasColumn(db, "board", "sceneVersion"))
        assertTrue(hasColumn(db, "board", "sceneJson"))
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

    private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        val cursor = db.query("PRAGMA table_info(`$table`)")
        val nameIndex = cursor.getColumnIndex("name")
        var found = false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                found = true
                break
            }
        }
        cursor.close()
        return found
    }

    companion object {
        private const val TEST_DB_19 = "migration-19-20-test"
        private const val TEST_DB_14 = "migration-14-20-test"
    }
}
