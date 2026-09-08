package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "team",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val season: String = "",
    val shieldUri: String? = null,
    val isSelected: Boolean = false,
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
