package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 16 → 17: entrenamientos, relación Training↔Task y Attachment genérico. */
object Migration16To17 : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `training` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `date` TEXT NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `opponentClubId` INTEGER,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_training_teamId` ON `training` (`teamId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_teamId_dateEpochDay` ON `training` (`teamId`, `dateEpochDay`)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_training_syncId` ON `training` (`syncId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `training_task` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `trainingId` INTEGER NOT NULL,
                `taskId` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_training_task_trainingId` ON `training_task` (`trainingId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_training_task_taskId` ON `training_task` (`taskId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_training_task_trainingId_taskId` ON `training_task` (`trainingId`, `taskId`)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_training_task_syncId` ON `training_task` (`syncId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attachment` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `parentType` TEXT NOT NULL,
                `parentSyncId` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `localPath` TEXT NOT NULL,
                `remotePath` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachment_parentType_parentSyncId` ON `attachment` (`parentType`, `parentSyncId`)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attachment_syncId` ON `attachment` (`syncId`)")
    }
}
