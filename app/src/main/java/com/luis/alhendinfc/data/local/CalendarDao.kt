package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentClubDao {

    @Query(
        "SELECT * FROM opponent_club WHERE teamId = :teamId AND deletedAt IS NULL ORDER BY sortOrder ASC, name ASC"
    )
    fun getByTeam(teamId: Int): Flow<List<OpponentClubEntity>>

    @Query("SELECT * FROM opponent_club WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: Int): OpponentClubEntity?

    @Query("SELECT * FROM opponent_club WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun observeById(id: Int): Flow<OpponentClubEntity?>

    @Query("SELECT * FROM opponent_club WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): OpponentClubEntity?

    @Query(
        "SELECT * FROM opponent_club WHERE teamId = :teamId AND name = :name LIMIT 1"
    )
    suspend fun getByTeamAndNameIncludingDeleted(teamId: Int, name: String): OpponentClubEntity?

    @Query("SELECT COUNT(*) FROM opponent_club WHERE teamId = :teamId AND deletedAt IS NULL")
    suspend fun countByTeam(teamId: Int): Int

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM opponent_club ORDER BY id ASC")
    suspend fun getAllOnce(): List<OpponentClubEntity>

    @Query("SELECT * FROM opponent_club WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): OpponentClubEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<OpponentClubEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OpponentClubEntity): Long

    @Update
    suspend fun update(entity: OpponentClubEntity)

    @Query(
        "UPDATE opponent_club SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}

@Dao
interface SeasonFixtureDao {

    @Query(
        "SELECT * FROM season_fixture WHERE teamId = :teamId AND deletedAt IS NULL ORDER BY matchday ASC"
    )
    fun getByTeam(teamId: Int): Flow<List<SeasonFixtureEntity>>

    @Query(
        "SELECT * FROM season_fixture WHERE teamId = :teamId AND matchday = :matchday AND deletedAt IS NULL LIMIT 1"
    )
    suspend fun getByMatchday(teamId: Int, matchday: Int): SeasonFixtureEntity?

    @Query(
        "SELECT * FROM season_fixture WHERE teamId = :teamId AND matchday = :matchday LIMIT 1"
    )
    suspend fun getByMatchdayIncludingDeleted(teamId: Int, matchday: Int): SeasonFixtureEntity?

    @Query("SELECT * FROM season_fixture WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): SeasonFixtureEntity?

    @Query("SELECT COUNT(*) FROM season_fixture WHERE teamId = :teamId AND deletedAt IS NULL")
    suspend fun countByTeam(teamId: Int): Int

    @Query(
        """
        SELECT COUNT(*) FROM season_fixture
        WHERE teamId = :teamId AND deletedAt IS NULL AND dateEpochDay = :epochDay
        """
    )
    suspend fun countActiveByTeamAndDay(teamId: Int, epochDay: Long): Int

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM season_fixture ORDER BY id ASC")
    suspend fun getAllOnce(): List<SeasonFixtureEntity>

    @Query("SELECT * FROM season_fixture WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): SeasonFixtureEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<SeasonFixtureEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SeasonFixtureEntity): Long

    @Update
    suspend fun update(entity: SeasonFixtureEntity)

    @Query(
        "UPDATE season_fixture SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
