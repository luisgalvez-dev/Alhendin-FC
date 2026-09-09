package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "board",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class BoardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val teamId: Int,
    val name: String,
    val sceneVersion: Int = 1,
    val sceneJson: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
