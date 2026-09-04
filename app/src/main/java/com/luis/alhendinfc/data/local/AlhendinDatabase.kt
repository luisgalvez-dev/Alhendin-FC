package com.luis.alhendinfc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 8,
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

        fun getInstance(context: Context): AlhendinDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlhendinDatabase::class.java,
                    "alhendin_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
