package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 15 → 16: biblioteca de tareas. Solo CREATE TABLE + índices. */
object Migration15To16 : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `objective` TEXT NOT NULL,
                `playerCount` INTEGER,
                `durationMinutes` INTEGER,
                `description` TEXT NOT NULL,
                `boardSyncId` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_teamId` ON `task` (`teamId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_syncId` ON `task` (`syncId`)")
    }
}
