package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 21 → 22: `attachment.localPath` nullable (metadata remota sin fichero local)
 * y cola `transfer_job` para upload/download/delete de blobs.
 */
object Migration21To22 : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attachment_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `syncId` TEXT NOT NULL,
                `parentType` TEXT NOT NULL,
                `parentSyncId` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `localPath` TEXT,
                `remotePath` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `attachment_new` (
                `id`, `syncId`, `parentType`, `parentSyncId`, `mimeType`, `name`,
                `localPath`, `remotePath`, `createdAt`, `updatedAt`, `deletedAt`
            )
            SELECT
                `id`, `syncId`, `parentType`, `parentSyncId`, `mimeType`, `name`,
                `localPath`, `remotePath`, `createdAt`, `updatedAt`, `deletedAt`
            FROM `attachment`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `attachment`")
        db.execSQL("ALTER TABLE `attachment_new` RENAME TO `attachment`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachment_parentType_parentSyncId` ON `attachment` (`parentType`, `parentSyncId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_attachment_syncId` ON `attachment` (`syncId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transfer_job` (
                `attachmentSyncId` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `enqueuedAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `lastError` TEXT,
                `hintPath` TEXT,
                PRIMARY KEY(`attachmentSyncId`, `kind`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transfer_job_enqueuedAt` ON `transfer_job` (`enqueuedAt`)"
        )
    }
}
