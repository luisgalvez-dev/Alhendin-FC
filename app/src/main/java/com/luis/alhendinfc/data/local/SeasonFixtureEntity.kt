package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_fixture",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "matchday"], unique = true)
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
    /** Si vacío, se usa el estadio del club visitado / local según isHome */
    val stadiumOverride: String = ""
)
