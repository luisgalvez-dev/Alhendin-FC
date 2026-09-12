package com.luis.alhendinfc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 20 → 21: cola de sincronización. Infraestructura, no dato deportivo. */
object Migration20To21 : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_outbox` (
                `entityType` TEXT NOT NULL,
                `entitySyncId` TEXT NOT NULL,
                `enqueuedAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `lastError` TEXT,
                PRIMARY KEY(`entityType`, `entitySyncId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_outbox_enqueuedAt` ON `sync_outbox` (`enqueuedAt`)"
        )
    }
}
