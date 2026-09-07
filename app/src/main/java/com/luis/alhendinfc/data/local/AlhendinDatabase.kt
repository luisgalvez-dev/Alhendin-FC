package com.luis.alhendinfc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        MatchPlayerEntity::class,
        MatchEventEntity::class,
        CustomStatTypeEntity::class,
        OpponentClubEntity::class,
        SeasonFixtureEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AlhendinDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun matchEventDao(): MatchEventDao
    abstract fun customStatTypeDao(): CustomStatTypeDao
    abstract fun opponentClubDao(): OpponentClubDao
    abstract fun seasonFixtureDao(): SeasonFixtureDao

    companion object {
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
                val app = context.applicationContext
                Room.databaseBuilder(
                    app,
                    AlhendinDatabase::class.java,
                    "alhendin_db"
                )
                    .addMigrations(
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { db ->
                        // Una sola vez: vaciar datos de prueba antiguos (plantilla/jornadas seed).
                        // clearAllTables() NO puede ejecutarse en el hilo principal.
                        val prefs = app.getSharedPreferences("alhendin_meta", Context.MODE_PRIVATE)
                        if (!prefs.getBoolean(KEY_CLEARED_DEMO, false)) {
                            val executor = Executors.newSingleThreadExecutor()
                            try {
                                executor.submit { db.clearAllTables() }.get()
                            } finally {
                                executor.shutdown()
                            }
                            prefs.edit().putBoolean(KEY_CLEARED_DEMO, true).apply()
                        }
                        INSTANCE = db
                    }
            }
        }

        private const val KEY_CLEARED_DEMO = "cleared_demo_data_v1"

        /** Reinicia la instancia (tras importar backup). */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
