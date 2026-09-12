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
class Migration21To22Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate21To22_makesLocalPathNullable_keepsAttachment_andCreatesTransferJob() {
        helper.createDatabase(TEST_DB, 21).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO attachment (
                    id, syncId, parentType, parentSyncId, mimeType, name,
                    localPath, remotePath, createdAt, updatedAt, deletedAt
                ) VALUES (
                    4, 'att-sync-1', 'TASK', 'task-sync', 'image/jpeg', 'foto.jpg',
                    '/files/foto.jpg', NULL, 20, 20, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 22, true, Migration21To22)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "attachment"))
        assertEquals("/files/foto.jpg", string(db, "SELECT localPath FROM attachment WHERE id = 4"))
        assertTrue(nullable(db, "attachment", "localPath"))
        assertTrue(hasTable(db, "transfer_job"))
        assertTrue(hasColumn(db, "transfer_job", "attachmentSyncId"))
        assertTrue(hasColumn(db, "transfer_job", "kind"))
        assertEquals(0L, count(db, "transfer_job"))
        db.execSQL(
            """
            INSERT INTO attachment (
                syncId, parentType, parentSyncId, mimeType, name,
                localPath, createdAt, updatedAt
            ) VALUES (
                'att-remote', 'MATCH', 'match-sync', 'application/pdf', 'a.pdf',
                NULL, 1, 1
            )
            """.trimIndent()
        )
        assertEquals(2L, count(db, "attachment"))
        db.close()
    }

    @Test
    fun migrate14To22_chainKeepsTeam() {
        helper.createDatabase(TEST_DB_14, 14).apply {
            execSQL("INSERT INTO team (id, name, category, season, isSelected) VALUES (1, 'Alhendin', '1', '25/26', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(
            TEST_DB_14,
            22,
            true,
            Migration14To15,
            Migration15To16,
            Migration16To17,
            Migration17To18,
            Migration18To19,
            Migration19To20,
            Migration20To21,
            Migration21To22
        )
        assertEquals(1L, count(db, "team"))
        assertTrue(hasTable(db, "sync_outbox"))
        assertTrue(hasTable(db, "transfer_job"))
        db.close()
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Long {
        val cursor = db.query("SELECT COUNT(*) FROM $table")
        cursor.moveToFirst()
        val value = cursor.getLong(0)
        cursor.close()
        return value
    }

    private fun string(db: SupportSQLiteDatabase, sql: String): String? {
        val cursor = db.query(sql)
        cursor.moveToFirst()
        val value = if (cursor.isNull(0)) null else cursor.getString(0)
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

    private fun nullable(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        val cursor = db.query("PRAGMA table_info(`$table`)")
        val nameIndex = cursor.getColumnIndex("name")
        val notNullIndex = cursor.getColumnIndex("notnull")
        var isNullable = false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                isNullable = cursor.getInt(notNullIndex) == 0
                break
            }
        }
        cursor.close()
        return isNullable
    }

    companion object {
        private const val TEST_DB = "migration-21-22-test"
        private const val TEST_DB_14 = "migration-14-22-test"
    }
}
