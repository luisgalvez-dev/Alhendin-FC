package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val season: String = "",
    val shieldUri: String? = null,
    val isSelected: Boolean = false
)
