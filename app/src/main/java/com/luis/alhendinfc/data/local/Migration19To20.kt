package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 19 → 20: pizarras persistentes. */
object Migration19To20 : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `board` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `sceneVersion` INTEGER NOT NULL,
                `sceneJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_board_teamId` ON `board` (`teamId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_board_syncId` ON `board` (`syncId`)"
        )
    }
}
