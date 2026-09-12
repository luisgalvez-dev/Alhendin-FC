package com.luis.alhendinfc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luis.alhendinfc.data.sync.SyncHooks
import com.luis.alhendinfc.data.sync.SyncWriteGate

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        MatchPlayerEntity::class,
        MatchEventEntity::class,
        CustomStatTypeEntity::class,
        OpponentClubEntity::class,
        SeasonFixtureEntity::class,
        TaskEntity::class,
        TrainingEntity::class,
        TrainingTaskEntity::class,
        AttachmentEntity::class,
        RivalAnalysisEntity::class,
        RivalLinkEntity::class,
        OpponentPlayerEntity::class,
        BoardEntity::class,
        SyncOutboxEntity::class
    ],
    version = 21,
    exportSchema = true
)
abstract class AlhendinDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun matchEventDao(): MatchEventDao
    abstract fun customStatTypeDao(): CustomStatTypeDao
    abstract fun opponentClubDao(): OpponentClubDao
    abstract fun seasonFixtureDao(): SeasonFixtureDao
    abstract fun taskDao(): TaskDao
    abstract fun trainingDao(): TrainingDao
    abstract fun trainingTaskDao(): TrainingTaskDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun rivalAnalysisDao(): RivalAnalysisDao
    abstract fun rivalLinkDao(): RivalLinkDao
    abstract fun opponentPlayerDao(): OpponentPlayerDao
    abstract fun boardDao(): BoardDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        const val VERSION = 21
        const val NAME = "alhendin_db"

        @Volatile
        private var INSTANCE: AlhendinDatabase? = null

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN livePeriod INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN liveElapsedSeconds INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN liveClockRunning INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN fieldSecondsJson TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN liveClockAnchorWallMs INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE match_table ADD COLUMN fieldPositionsJson TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_match_table_teamId` ON `match_table` (`teamId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_match_table_teamId_status` ON `match_table` (`teamId`, `status`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_player_teamId` ON `player` (`teamId`)"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE opponent_club ADD COLUMN kitColors TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Si la 12→13 antigua añadió kitColorArgb (int), añadimos el texto aquí.
                val cursor = db.query("PRAGMA table_info(`opponent_club`)")
                var hasKitColors = false
                while (cursor.moveToNext()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx >= 0 && cursor.getString(nameIdx) == "kitColors") {
                        hasKitColors = true
                        break
                    }
                }
                cursor.close()
                if (!hasKitColors) {
                    db.execSQL(
                        "ALTER TABLE opponent_club ADD COLUMN kitColors TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        fun getInstance(context: Context): AlhendinDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlhendinDatabase::class.java,
                    NAME
                )
                    .addMigrations(
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        Migration14To15,
                        Migration15To16,
                        Migration16To17,
                        Migration17To18,
                        Migration18To19,
                        Migration19To20,
                        Migration20To21
                    )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { db ->
                        INSTANCE = db
                        SyncHooks.gate = SyncWriteGate(db)
                    }
            }
        }

        /** Reinicia la instancia (tras importar backup). */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                SyncHooks.gate = null
            }
        }
    }
}
