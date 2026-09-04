package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentClubDao {

    @Query("SELECT * FROM opponent_club WHERE teamId = :teamId ORDER BY sortOrder ASC, name ASC")
    fun getByTeam(teamId: Int): Flow<List<OpponentClubEntity>>

    @Query("SELECT * FROM opponent_club WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): OpponentClubEntity?

    @Query("SELECT COUNT(*) FROM opponent_club WHERE teamId = :teamId")
    suspend fun countByTeam(teamId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OpponentClubEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<OpponentClubEntity>): List<Long>

    @Update
    suspend fun update(entity: OpponentClubEntity)

    @Delete
    suspend fun delete(entity: OpponentClubEntity)
}

@Dao
interface SeasonFixtureDao {

    @Query("SELECT * FROM season_fixture WHERE teamId = :teamId ORDER BY matchday ASC")
    fun getByTeam(teamId: Int): Flow<List<SeasonFixtureEntity>>

    @Query("SELECT * FROM season_fixture WHERE teamId = :teamId AND matchday = :matchday LIMIT 1")
    suspend fun getByMatchday(teamId: Int, matchday: Int): SeasonFixtureEntity?

    @Query("SELECT COUNT(*) FROM season_fixture WHERE teamId = :teamId")
    suspend fun countByTeam(teamId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SeasonFixtureEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<SeasonFixtureEntity>)

    @Update
    suspend fun update(entity: SeasonFixtureEntity)

    @Delete
    suspend fun delete(entity: SeasonFixtureEntity)
}
