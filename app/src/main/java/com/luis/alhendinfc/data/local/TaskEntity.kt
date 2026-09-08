package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val teamId: Int,
    val name: String,
    val objective: String = "",
    val playerCount: Int? = null,
    val durationMinutes: Int? = null,
    val description: String = "",
    val boardSyncId: String? = null,
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
