package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_fixture",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "matchday"], unique = true),
        Index(value = ["syncId"], unique = true),
        Index(value = ["dateEpochDay"])
    ]
)
data class SeasonFixtureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val matchday: Int,
    val opponentClubId: Int,
    val isHome: Boolean = true,
    val date: String = "",
    val time: String = "",
    val stadiumOverride: String = "",
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val dateEpochDay: Long? = null
)
