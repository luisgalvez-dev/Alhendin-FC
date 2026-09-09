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
class Migration18To19Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate18To19_keepsPreviousTablesAndCreatesOpponentPlayer() {
        helper.createDatabase(TEST_DB_18, 18).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO opponent_club (id, teamId, name, shortName, stadium, kitColors, sortOrder, syncId, createdAt, updatedAt)
                VALUES (4, 1, 'Rival', 'RIV', 'Campo', '', 0, 'club-sync', 20, 20)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_18, 19, true, Migration18To19)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "opponent_club"))
        assertTrue(hasTable(db, "opponent_player"))
        assertTrue(hasIndex(db, "index_opponent_player_syncId"))
        assertTrue(hasIndex(db, "index_opponent_player_opponentClubId"))
        assertEquals(0L, count(db, "opponent_player"))
        db.close()
    }

    @Test
    fun migrate14To19_chainKeepsDataAndAddsPlantilla() {
        helper.createDatabase(TEST_DB_14, 14).apply {
            execSQL("INSERT INTO team (id, name, category, season, isSelected) VALUES (1, 'Alhendin', '1', '25/26', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(
            TEST_DB_14,
            19,
            true,
            Migration14To15,
            Migration15To16,
            Migration16To17,
            Migration17To18,
            Migration18To19
        )
        assertEquals(1L, count(db, "team"))
        assertTrue(hasTable(db, "rival_analysis"))
        assertTrue(hasTable(db, "opponent_player"))
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

    companion object {
        private const val TEST_DB_18 = "migration-18-19-test"
        private const val TEST_DB_14 = "migration-14-19-test"
    }
}
