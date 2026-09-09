package com.luis.alhendinfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {

    @Query(
        """
        SELECT * FROM board
        WHERE teamId = :teamId AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    fun getByTeam(teamId: Int): Flow<List<BoardEntity>>

    @Query("SELECT * FROM board WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getById(id: Int): Flow<BoardEntity?>

    @Query("SELECT * FROM board WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getByIdOnce(id: Int): BoardEntity?

    @Query("SELECT * FROM board WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): BoardEntity?

    @Query("SELECT * FROM board WHERE syncId = :syncId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBySyncIdOnce(syncId: String): BoardEntity?

    @Query("SELECT * FROM board WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncIdIncludingDeleted(syncId: String): BoardEntity?

    /** Incluye tombstones. Uso interno / backup / futura sync. */
    @Query("SELECT * FROM board ORDER BY id ASC")
    suspend fun getAllOnce(): List<BoardEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BoardEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<BoardEntity>)

    @Update
    suspend fun update(entity: BoardEntity)

    @Query(
        "UPDATE board SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun markDeleted(id: Int, now: Long)
}
