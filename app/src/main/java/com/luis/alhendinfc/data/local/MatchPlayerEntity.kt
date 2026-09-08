package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_player",
    indices = [
        Index(value = ["matchId", "playerId"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class MatchPlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val playerId: Int,
    val callupStatus: String = "NONE",
    val isOnField: Boolean = false,
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
