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
class Migration14To15Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlhendinDatabase::class.java
    )

    @Test
    fun migrate14To15_preservesIdentityAndAutoincrement() {
        helper.createDatabase(TEST_DB, 14).apply {
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
            execSQL(
                """
                INSERT INTO match_player (id, matchId, playerId, callupStatus, isOnField)
                VALUES (3, 12, 5, 'TITULAR', 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO match_event (id, matchId, typeCode, minute, period, value, createdAt)
                VALUES (8, 12, 'GOAL', 10, 1, 1, 12345)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO opponent_club (id, teamId, name, shortName, stadium, kitColors, sortOrder)
                VALUES (2, 1, 'Rival CF', '', '', '', 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO season_fixture (id, teamId, matchday, opponentClubId, isHome, date, time, stadiumOverride)
                VALUES (4, 1, 1, 2, 1, '7/9/2026', '', '')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO custom_stat_type (id, teamId, code, label, shortLabel, appliesTo, sortOrder, isActive, createdAt)
                VALUES (6, 1, 'PRESS', 'Presion', 'PR', 'ALL', 0, 1, 999)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 15, true, Migration14To15)
        assertEquals(1L, count(db, "team"))
        assertEquals(1L, count(db, "player"))
        assertEquals(1L, count(db, "match_table"))
        assertEquals(1L, count(db, "match_player"))
        assertEquals(1L, count(db, "match_event"))
        assertEquals(1L, count(db, "opponent_club"))
        assertEquals(1L, count(db, "season_fixture"))
        assertEquals(1L, count(db, "custom_stat_type"))
        assertEquals(5L, scalar(db, "SELECT id FROM player"))
        assertEquals(12L, scalar(db, "SELECT id FROM match_table"))
        assertEquals(1L, scalar(db, "SELECT teamId FROM player"))
        assertEquals(12L, scalar(db, "SELECT matchId FROM match_player"))
        assertEquals(5L, scalar(db, "SELECT playerId FROM match_player"))
        assertEquals(12345L, scalar(db, "SELECT createdAt FROM match_event"))
        assertEquals(12345L, scalar(db, "SELECT updatedAt FROM match_event"))
        assertEquals(999L, scalar(db, "SELECT createdAt FROM custom_stat_type"))
        assertTrue(string(db, "SELECT syncId FROM player").isNotBlank())
        assertTrue(string(db, "SELECT syncId FROM match_table").isNotBlank())
        assertEquals(0L, count(db, "player WHERE deletedAt IS NOT NULL"))
        assertEquals("07/09/2026", string(db, "SELECT date FROM match_table"))
        assertEquals(
            java.time.LocalDate.of(2026, 9, 7).toEpochDay(),
            scalar(db, "SELECT dateEpochDay FROM match_table")
        )
        val playerSync = string(db, "SELECT syncId FROM player")
        val matchSync = string(db, "SELECT syncId FROM match_table")

        db.execSQL(
            """
            INSERT INTO player (
                teamId, name, alias, position, jerseyNumber, height, weight, laterality, isActive, observations,
                syncId, createdAt, updatedAt
            ) VALUES (
                1, 'Nuevo', '', 'MEDIOCENTRO_DEFENSIVO', 11, 0, 0, 'DERECHA', 1, '',
                '11111111-1111-1111-1111-111111111111', 1, 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO match_table (
                teamId, rival, stadium, date, time, matchday, isHome, durationPerPart, numParts,
                formation, notes, status, livePeriod, liveElapsedSeconds, liveClockRunning,
                liveClockAnchorWallMs, fieldSecondsJson, fieldPositionsJson,
                syncId, createdAt, updatedAt
            ) VALUES (
                1, 'Otro', '', '01/02/2026', '', 2, 1, 45, 2, '', '', 'OPEN', 1, 0, 0, 0, '', '',
                '22222222-2222-2222-2222-222222222222', 1, 1
            )
            """.trimIndent()
        )
        val newPlayerId = scalar(db, "SELECT MAX(id) FROM player")
        val newMatchId = scalar(db, "SELECT MAX(id) FROM match_table")
        assertTrue("nuevo player id=$newPlayerId", newPlayerId > 5)
        assertTrue("nuevo match id=$newMatchId", newMatchId > 12)
        db.close()

        val reopened = helper.runMigrationsAndValidate(TEST_DB, 15, true, Migration14To15)
        assertEquals(playerSync, string(reopened, "SELECT syncId FROM player WHERE id = 5"))
        assertEquals(matchSync, string(reopened, "SELECT syncId FROM match_table WHERE id = 12"))
        assertFalse(hasIndex(reopened, "index_player_deletedAt"))
        assertTrue(hasIndex(reopened, "index_player_syncId"))
        assertTrue(hasIndex(reopened, "index_match_table_dateEpochDay"))
        assertTrue(hasIndex(reopened, "index_player_teamId"))
        reopened.close()
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
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(name))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    companion object {
        private const val TEST_DB = "migration-14-15-test"
    }
}
