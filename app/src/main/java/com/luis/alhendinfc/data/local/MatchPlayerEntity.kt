package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_player",
    indices = [Index(value = ["matchId", "playerId"], unique = true)]
)
data class MatchPlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val playerId: Int,
    val callupStatus: String = "NONE"
)
