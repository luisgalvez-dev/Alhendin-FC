package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_event",
    indices = [
        Index(value = ["matchId"]),
        Index(value = ["playerId"]),
        Index(value = ["typeCode"])
    ]
)
data class MatchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val typeCode: String,
    val playerId: Int? = null,
    val relatedPlayerId: Int? = null,
    val minute: Int = 0,
    val period: Int = 1,
    val value: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
