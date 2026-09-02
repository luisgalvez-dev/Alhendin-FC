package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM player WHERE teamId = :teamId ORDER BY jerseyNumber ASC")
    fun getAllByTeam(teamId: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE id = :id")
    fun getById(id: Int): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)
}
