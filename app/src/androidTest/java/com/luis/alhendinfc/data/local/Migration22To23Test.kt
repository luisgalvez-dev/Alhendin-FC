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
class Migration22To23Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate22To23_addsOpponentClubSyncId_andBackfillsFromClub() {
        helper.createDatabase(TEST_DB, 22).apply {
            execSQL(
                """
                INSERT INTO team (id, name, category, season, isSelected, syncId, createdAt, updatedAt)
                VALUES (1, 'Alhendin', '1', '25/26', 1, 'team-sync', 10, 10)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO opponent_club (
                    id, teamId, name, shortName, stadium, kitColors, sortOrder,
                    syncId, createdAt, updatedAt
                ) VALUES (
                    4, 1, 'U.D. Maracena', 'Maracena', 'Ciudad Deportiva', '', 0,
                    'club-maracena', 10, 10
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO match_table (
                    id, teamId, rival, stadium, date, time, matchday, isHome,
                    durationPerPart, numParts, formation, notes, status,
                    opponentClubId, livePeriod, liveElapsedSeconds, liveClockRunning,
                    liveClockAnchorWallMs, fieldSecondsJson, fieldPositionsJson,
                    syncId, createdAt, updatedAt
                ) VALUES (
                    1, 1, 'U.D. Maracena', '', '12/09/2026', '', 4, 1,
                    45, 2, '', '', 'FINISHED',
                    4, 1, 0, 0, 0, '', '',
                    'match-sync', 10, 10
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 23, true, Migration22To23)
        assertTrue(hasColumn(db, "match_table", "opponentClubSyncId"))
        assertEquals("club-maracena", string(db, "SELECT opponentClubSyncId FROM match_table WHERE id = 1"))
        db.close()
    }

    private fun string(db: SupportSQLiteDatabase, sql: String): String? {
        val cursor = db.query(sql)
        cursor.moveToFirst()
        val value = if (cursor.isNull(0)) null else cursor.getString(0)
        cursor.close()
        return value
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
        private const val TEST_DB = "migration-22-23-test"
    }
}
