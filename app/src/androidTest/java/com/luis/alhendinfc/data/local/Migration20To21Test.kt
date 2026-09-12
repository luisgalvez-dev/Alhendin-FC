package com.luis.alhendinfc.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration20To21Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate20To21_createsOutbox_andKeepsSportsTables() {
        helper.createDatabase(TEST_DB, 20).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO board (id, syncId, teamId, name, sceneVersion, sceneJson, createdAt, updatedAt)
                VALUES (2, 'board-sync', 1, 'Presión', 1, '{}', 20, 20)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 21, true, Migration20To21)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "board"))
        assertTrue(hasTable(db, "sync_outbox"))
        assertTrue(hasColumn(db, "sync_outbox", "entityType"))
        assertTrue(hasColumn(db, "sync_outbox", "entitySyncId"))
        assertEquals(0L, count(db, "sync_outbox"))
        assertFalse(hasColumn(db, "team", "isDemo"))
        db.close()
    }

    @Test
    fun migrate20To21_outboxUniqueAndKeepsSportsData() {
        helper.createDatabase(TEST_DB_UNIQUE, 20).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB_UNIQUE, 21, true, Migration20To21)
        db.execSQL(
            """
            INSERT INTO sync_outbox (entityType, entitySyncId, enqueuedAt, attempts)
            VALUES ('tasks', 'same-id', 1, 0)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sync_outbox (entityType, entitySyncId, enqueuedAt, attempts)
            VALUES ('teams', 'same-id', 1, 0)
            """.trimIndent()
        )
        var duplicateRejected = false
        try {
            db.execSQL(
                """
                INSERT INTO sync_outbox (entityType, entitySyncId, enqueuedAt, attempts)
                VALUES ('tasks', 'same-id', 2, 0)
                """.trimIndent()
            )
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            duplicateRejected = true
        }
        assertTrue(duplicateRejected)
        assertEquals(2L, count(db, "sync_outbox"))
        assertEquals(1L, count(db, "team"))
        db.close()
    }

    @Test
    fun migrate14To21_chainKeepsTeamAndAddsOutbox() {
        helper.createDatabase(TEST_DB_14, 14).apply {
            execSQL("INSERT INTO team (id, name, category, season, isSelected) VALUES (1, 'Alhendin', '1', '25/26', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(
            TEST_DB_14,
            21,
            true,
            Migration14To15,
            Migration15To16,
            Migration16To17,
            Migration17To18,
            Migration18To19,
            Migration19To20,
            Migration20To21
        )
        assertEquals(1L, count(db, "team"))
        assertTrue(hasTable(db, "sync_outbox"))
        assertTrue(hasColumn(db, "sync_outbox", "entityType"))
        assertTrue(hasColumn(db, "sync_outbox", "entitySyncId"))
        db.close()
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Long {
        val cursor = db.query("SELECT COUNT(*) FROM $table")
        cursor.moveToFirst()
        val value = cursor.getLong(0)
        cursor.close()
        return value
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
        private const val TEST_DB = "migration-20-21-test"
        private const val TEST_DB_UNIQUE = "migration-20-21-unique"
        private const val TEST_DB_14 = "migration-14-21-test"
    }
}
