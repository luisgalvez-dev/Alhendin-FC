package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_table",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "status"]),
        Index(value = ["syncId"], unique = true),
        Index(value = ["dateEpochDay"])
    ]
)
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
    val rivalShieldUri: String? = null,
    val livePeriod: Int = 1,
    val liveElapsedSeconds: Int = 0,
    val liveClockRunning: Boolean = false,
    val liveClockAnchorWallMs: Long = 0L,
    val fieldSecondsJson: String = "",
    val fieldPositionsJson: String = "",
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val dateEpochDay: Long? = null
)
