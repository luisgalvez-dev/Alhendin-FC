package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM task
        WHERE teamId = :teamId AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    fun getByTeam(teamId: Int): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM task
        WHERE teamId = :teamId AND deletedAt IS NULL
          AND LOWER(name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    fun searchByName(teamId: Int, query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getById(id: Int): Flow<TaskEntity?>

    @Query("SELECT * FROM task WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): TaskEntity?

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): TaskEntity?

    @Query("SELECT * FROM task WHERE id IN (:ids)")
    suspend fun getByIdsIncludingDeleted(ids: List<Int>): List<TaskEntity>

    @Query("SELECT * FROM task")
    fun observeAllIncludingDeleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE boardSyncId = :syncId")
    suspend fun getByBoardSyncIdIncludingDeleted(syncId: String): List<TaskEntity>

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM task ORDER BY id ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<TaskEntity>)

    @Update
    suspend fun update(entity: TaskEntity)

    @Query(
        "UPDATE task SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
