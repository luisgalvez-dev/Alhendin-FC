package com.luis.alhendinfc.domain.model

data class PlayerSeasonStats(
    val player: Player,
    val matchesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0
)
