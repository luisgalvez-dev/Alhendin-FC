package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luis.alhendinfc.domain.model.CalendarDate
import java.util.UUID

/**
 * 14 → 15: syncId, timestamps, dateEpochDay.
 * ALTER + backfill, luego reconstrucción para que el schema coincida con las Entity
 * (sin DEFAULT residuales de ALTER).
 */
object Migration14To15 : Migration(14, 15) {

    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        addSyncColumns(db, "team", hasCreatedAt = false)
        addSyncColumns(db, "player", hasCreatedAt = false)
        addSyncColumns(db, "match_table", hasCreatedAt = false)
        addSyncColumns(db, "match_player", hasCreatedAt = false)
        addSyncColumns(db, "match_event", hasCreatedAt = true)
        addSyncColumns(db, "opponent_club", hasCreatedAt = false)
        addSyncColumns(db, "season_fixture", hasCreatedAt = false)
        addSyncColumns(db, "custom_stat_type", hasCreatedAt = true)

        db.execSQL("ALTER TABLE match_table ADD COLUMN dateEpochDay INTEGER")
        db.execSQL("ALTER TABLE season_fixture ADD COLUMN dateEpochDay INTEGER")

        backfillUuidsAndTimestamps(db, "team", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "player", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "match_table", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "match_player", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "match_event", now, hasCreatedAt = true)
        backfillUuidsAndTimestamps(db, "opponent_club", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "season_fixture", now, hasCreatedAt = false)
        backfillUuidsAndTimestamps(db, "custom_stat_type", now, hasCreatedAt = true)

        backfillEpochDays(db, "match_table")
        backfillEpochDays(db, "season_fixture")

        rebuildTeam(db)
        rebuildPlayer(db)
        rebuildMatch(db)
        rebuildMatchPlayer(db)
        rebuildMatchEvent(db)
        rebuildOpponentClub(db)
        rebuildSeasonFixture(db)
        rebuildCustomStatType(db)

        repairSequences(db)
    }

    private fun addSyncColumns(db: SupportSQLiteDatabase, table: String, hasCreatedAt: Boolean) {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        if (!hasCreatedAt) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("ALTER TABLE `$table` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN deletedAt INTEGER")
    }

    private fun backfillUuidsAndTimestamps(
        db: SupportSQLiteDatabase,
        table: String,
        now: Long,
        hasCreatedAt: Boolean
    ) {
        val cursor = db.query("SELECT id FROM `$table`")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val uuid = UUID.randomUUID().toString()
            if (hasCreatedAt) {
                db.execSQL(
                    """
                    UPDATE `$table` SET
                        syncId = ?,
                        updatedAt = CASE WHEN createdAt > 0 THEN createdAt ELSE ? END
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf<Any>(uuid, now, id)
                )
            } else {
                db.execSQL(
                    "UPDATE `$table` SET syncId = ?, createdAt = ?, updatedAt = ? WHERE id = ?",
                    arrayOf<Any>(uuid, now, now, id)
                )
            }
        }
        cursor.close()
        val empty = db.query("SELECT COUNT(*) FROM `$table` WHERE syncId = ''")
        empty.moveToFirst()
        val remaining = empty.getInt(0)
        empty.close()
        check(remaining == 0) { "syncId vacío en $table tras backfill ($remaining filas)" }
    }

    private fun backfillEpochDays(db: SupportSQLiteDatabase, table: String) {
        val cursor = db.query("SELECT id, date FROM `$table`")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val date = cursor.getString(1) ?: ""
            val epoch = CalendarDate.toEpochDay(date)
            db.execSQL(
                "UPDATE `$table` SET dateEpochDay = ? WHERE id = ?",
                arrayOf<Any?>(epoch, id)
            )
        }
        cursor.close()
    }

    private fun rebuildTeam(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `team_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `season` TEXT NOT NULL,
                `shieldUri` TEXT,
                `isSelected` INTEGER NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `team_new` (
                id, name, category, season, shieldUri, isSelected, syncId, createdAt, updatedAt, deletedAt
            ) SELECT id, name, category, season, shieldUri, isSelected, syncId, createdAt, updatedAt, deletedAt
            FROM `team`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `team`")
        db.execSQL("ALTER TABLE `team_new` RENAME TO `team`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_team_syncId` ON `team` (`syncId`)")
    }

    private fun rebuildPlayer(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `player_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `alias` TEXT NOT NULL,
                `position` TEXT NOT NULL,
                `jerseyNumber` INTEGER NOT NULL,
                `photoUri` TEXT,
                `height` INTEGER NOT NULL,
                `weight` INTEGER NOT NULL,
                `laterality` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `observations` TEXT NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `player_new` (
                id, teamId, name, alias, position, jerseyNumber, photoUri, height, weight,
                laterality, isActive, observations, syncId, createdAt, updatedAt, deletedAt
            ) SELECT id, teamId, name, alias, position, jerseyNumber, photoUri, height, weight,
                laterality, isActive, observations, syncId, createdAt, updatedAt, deletedAt
            FROM `player`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `player`")
        db.execSQL("ALTER TABLE `player_new` RENAME TO `player`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_teamId` ON `player` (`teamId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_player_syncId` ON `player` (`syncId`)")
    }

    private fun rebuildMatch(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `match_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `rival` TEXT NOT NULL,
                `stadium` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `matchday` INTEGER NOT NULL,
                `isHome` INTEGER NOT NULL,
                `durationPerPart` INTEGER NOT NULL,
                `numParts` INTEGER NOT NULL,
                `formation` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `homeScore` INTEGER,
                `awayScore` INTEGER,
                `opponentClubId` INTEGER,
                `rivalShieldUri` TEXT,
                `livePeriod` INTEGER NOT NULL,
                `liveElapsedSeconds` INTEGER NOT NULL,
                `liveClockRunning` INTEGER NOT NULL,
                `liveClockAnchorWallMs` INTEGER NOT NULL,
                `fieldSecondsJson` TEXT NOT NULL,
                `fieldPositionsJson` TEXT NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `dateEpochDay` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `match_table_new` (
                id, teamId, rival, stadium, date, time, matchday, isHome, durationPerPart, numParts,
                formation, notes, status, homeScore, awayScore, opponentClubId, rivalShieldUri,
                livePeriod, liveElapsedSeconds, liveClockRunning, liveClockAnchorWallMs,
                fieldSecondsJson, fieldPositionsJson, syncId, createdAt, updatedAt, deletedAt, dateEpochDay
            ) SELECT
                id, teamId, rival, stadium, date, time, matchday, isHome, durationPerPart, numParts,
                formation, notes, status, homeScore, awayScore, opponentClubId, rivalShieldUri,
                livePeriod, liveElapsedSeconds, liveClockRunning, liveClockAnchorWallMs,
                fieldSecondsJson, fieldPositionsJson, syncId, createdAt, updatedAt, deletedAt, dateEpochDay
            FROM `match_table`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `match_table`")
        db.execSQL("ALTER TABLE `match_table_new` RENAME TO `match_table`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_match_table_teamId` ON `match_table` (`teamId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_match_table_teamId_status` ON `match_table` (`teamId`, `status`)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_match_table_syncId` ON `match_table` (`syncId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_match_table_dateEpochDay` ON `match_table` (`dateEpochDay`)"
        )
    }

    private fun rebuildMatchPlayer(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `match_player_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `matchId` INTEGER NOT NULL,
                `playerId` INTEGER NOT NULL,
                `callupStatus` TEXT NOT NULL,
                `isOnField` INTEGER NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `match_player_new` (
                id, matchId, playerId, callupStatus, isOnField, syncId, createdAt, updatedAt, deletedAt
            ) SELECT id, matchId, playerId, callupStatus, isOnField, syncId, createdAt, updatedAt, deletedAt
            FROM `match_player`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `match_player`")
        db.execSQL("ALTER TABLE `match_player_new` RENAME TO `match_player`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_match_player_matchId_playerId` ON `match_player` (`matchId`, `playerId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_match_player_syncId` ON `match_player` (`syncId`)"
        )
    }

    private fun rebuildMatchEvent(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `match_event_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `matchId` INTEGER NOT NULL,
                `typeCode` TEXT NOT NULL,
                `playerId` INTEGER,
                `relatedPlayerId` INTEGER,
                `minute` INTEGER NOT NULL,
                `period` INTEGER NOT NULL,
                `value` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `syncId` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `match_event_new` (
                id, matchId, typeCode, playerId, relatedPlayerId, minute, period, value,
                createdAt, syncId, updatedAt, deletedAt
            ) SELECT id, matchId, typeCode, playerId, relatedPlayerId, minute, period, value,
                createdAt, syncId, updatedAt, deletedAt
            FROM `match_event`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `match_event`")
        db.execSQL("ALTER TABLE `match_event_new` RENAME TO `match_event`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_match_event_matchId` ON `match_event` (`matchId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_match_event_playerId` ON `match_event` (`playerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_match_event_typeCode` ON `match_event` (`typeCode`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_match_event_syncId` ON `match_event` (`syncId`)"
        )
    }

    private fun rebuildOpponentClub(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `opponent_club_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `shortName` TEXT NOT NULL,
                `stadium` TEXT NOT NULL,
                `shieldUri` TEXT,
                `kitColors` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `opponent_club_new` (
                id, teamId, name, shortName, stadium, shieldUri, kitColors, sortOrder,
                syncId, createdAt, updatedAt, deletedAt
            ) SELECT id, teamId, name, shortName, stadium, shieldUri, kitColors, sortOrder,
                syncId, createdAt, updatedAt, deletedAt
            FROM `opponent_club`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `opponent_club`")
        db.execSQL("ALTER TABLE `opponent_club_new` RENAME TO `opponent_club`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_opponent_club_teamId` ON `opponent_club` (`teamId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_opponent_club_teamId_name` ON `opponent_club` (`teamId`, `name`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_opponent_club_syncId` ON `opponent_club` (`syncId`)"
        )
    }

    private fun rebuildSeasonFixture(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `season_fixture_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `matchday` INTEGER NOT NULL,
                `opponentClubId` INTEGER NOT NULL,
                `isHome` INTEGER NOT NULL,
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `stadiumOverride` TEXT NOT NULL,
                `syncId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `dateEpochDay` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `season_fixture_new` (
                id, teamId, matchday, opponentClubId, isHome, date, time, stadiumOverride,
                syncId, createdAt, updatedAt, deletedAt, dateEpochDay
            ) SELECT id, teamId, matchday, opponentClubId, isHome, date, time, stadiumOverride,
                syncId, createdAt, updatedAt, deletedAt, dateEpochDay
            FROM `season_fixture`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `season_fixture`")
        db.execSQL("ALTER TABLE `season_fixture_new` RENAME TO `season_fixture`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_season_fixture_teamId` ON `season_fixture` (`teamId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_season_fixture_teamId_matchday` ON `season_fixture` (`teamId`, `matchday`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_season_fixture_syncId` ON `season_fixture` (`syncId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_season_fixture_dateEpochDay` ON `season_fixture` (`dateEpochDay`)"
        )
    }

    private fun rebuildCustomStatType(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_stat_type_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `code` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `shortLabel` TEXT NOT NULL,
                `appliesTo` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `syncId` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `custom_stat_type_new` (
                id, teamId, code, label, shortLabel, appliesTo, sortOrder, isActive,
                createdAt, syncId, updatedAt, deletedAt
            ) SELECT id, teamId, code, label, shortLabel, appliesTo, sortOrder, isActive,
                createdAt, syncId, updatedAt, deletedAt
            FROM `custom_stat_type`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `custom_stat_type`")
        db.execSQL("ALTER TABLE `custom_stat_type_new` RENAME TO `custom_stat_type`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_stat_type_teamId` ON `custom_stat_type` (`teamId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_stat_type_teamId_code` ON `custom_stat_type` (`teamId`, `code`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_stat_type_syncId` ON `custom_stat_type` (`syncId`)"
        )
    }

    private fun repairSequences(db: SupportSQLiteDatabase) {
        val tables = listOf(
            "team",
            "player",
            "match_table",
            "match_player",
            "match_event",
            "opponent_club",
            "season_fixture",
            "custom_stat_type"
        )
        tables.forEach { table ->
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
            db.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) SELECT ?, IFNULL(MAX(id), 0) FROM `$table`",
                arrayOf(table)
            )
        }
    }
}
