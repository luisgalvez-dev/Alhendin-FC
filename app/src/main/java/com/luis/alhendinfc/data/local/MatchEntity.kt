package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_table")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val rival: String = "",
    val stadium: String = "",
    val date: String = "",
    val time: String = "",
    val matchday: Int = 1,
    val isHome: Boolean = true,
    val durationPerPart: Int = 45,
    val numParts: Int = 2,
    val formation: String = "",
    val notes: String = "",
    val status: String = "OPEN",
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val opponentClubId: Int? = null,
    val rivalShieldUri: String? = null
)
