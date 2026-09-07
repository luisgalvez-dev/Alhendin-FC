package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opponent_club",
    indices = [Index(value = ["teamId"]), Index(value = ["teamId", "name"], unique = true)]
)
data class OpponentClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val name: String,
    val shortName: String = "",
    val stadium: String = "",
    val shieldUri: String? = null,
    /** Texto libre: color(es) de la equipación. */
    val kitColors: String = "",
    val sortOrder: Int = 0
)
