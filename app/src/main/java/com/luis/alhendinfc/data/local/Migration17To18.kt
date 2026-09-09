package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 17 → 18: análisis 1:1 del rival y enlaces reutilizables. */
object Migration17To18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `rival_analysis` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `opponentClubId` INTEGER NOT NULL,
                `usualSystem` TEXT NOT NULL,
                `variants` TEXT NOT NULL,
                `buildUp` TEXT NOT NULL,
                `progression` TEXT NOT NULL,
                `finalThird` TEXT NOT NULL,
                `highPress` TEXT NOT NULL,
                `midBlock` TEXT NOT NULL,
                `lowBlock` TEXT NOT NULL,
                `transAttackToDefense` TEXT NOT NULL,
                `transDefenseToAttack` TEXT NOT NULL,
                `cornersOffensive` TEXT NOT NULL,
                `cornersDefensive` TEXT NOT NULL,
                `setPieces` TEXT NOT NULL,
                `strengths` TEXT NOT NULL,
                `weaknesses` TEXT NOT NULL,
                `keyPlayers` TEXT NOT NULL,
                `generalNotes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rival_analysis_opponentClubId` ON `rival_analysis` (`opponentClubId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rival_analysis_syncId` ON `rival_analysis` (`syncId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `rival_link` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `opponentClubId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_rival_link_opponentClubId` ON `rival_link` (`opponentClubId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rival_link_syncId` ON `rival_link` (`syncId`)"
        )
    }
}
