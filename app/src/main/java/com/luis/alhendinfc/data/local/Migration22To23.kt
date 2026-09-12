package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 22 → 23: identidad portable del rival en el partido.
 * Cloud ya enviaba `opponentClubSyncId`; Room solo guardaba el id local.
 */
object Migration22To23 : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `match_table` ADD COLUMN `opponentClubSyncId` TEXT")
        db.execSQL(
            """
            UPDATE `match_table`
            SET `opponentClubSyncId` = (
                SELECT c.`syncId` FROM `opponent_club` c
                WHERE c.`id` = `match_table`.`opponentClubId`
                  AND c.`syncId` IS NOT NULL
                  AND TRIM(c.`syncId`) != ''
            )
            WHERE `opponentClubId` IS NOT NULL
            """.trimIndent()
        )
    }
}
